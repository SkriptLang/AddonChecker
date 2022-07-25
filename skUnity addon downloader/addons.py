import base64
import hashlib
import json
import os
import re
from enum import Enum
from queue import Queue
from threading import Lock, Thread

import dateutil.parser
import requests

with open("config.txt") as file:
	CONFIG = json.load(file)

API_KEY = CONFIG["SKUNITY_API_KEY"]
GITHUB_USERNAME = CONFIG["GITHUB_USERNAME"]
GITHUB_TOKEN = CONFIG["GITHUB_ACCESS_TOKEN"]
ADDON_DOWNLOAD_DIRECTORY = CONFIG["ADDON_DOWNLOAD_DIRECTORY"]
SKU_ADDON_DOWNLOAD_URL = f"https://docs.skunity.com/api/?key={API_KEY}&function=getAddonDownloadURLs"
DOWNLOAD_THREAD_AMOUNT = CONFIG["DOWNLOAD_THREAD_COUNT"]
UNSTABLE_GITHUB_RELEASES = CONFIG["UNSTABLE_GITHUB_RELEASES"]

# TODO SkriptTools ?q= link support, SkriptTools Data API
# TODO mb also try implementing SkriptTools addon list

# noinspection PyListCreation
DOWNLOAD_URL_CONVERTERS = []
# GitHub release link
DOWNLOAD_URL_CONVERTERS.append((
	"https?://(?:www\\.)?github\\.com/([a-zA-Z0-9_\\-.]+)/([a-zA-Z0-9_\\-.]+)/releases/tag/([a-zA-Z0-9_\\-.]+)",
	lambda m: get_github_download_url(m.group(1), m.group(2))
))
# GitHub releases list or latest link
DOWNLOAD_URL_CONVERTERS.append((
	"https?://(?:www\\.)?github\\.com/([a-zA-Z0-9_\\-.]+)/([a-zA-Z0-9_\\-.]+)/?(?:releases/?(?:latest/?)?)?",
	lambda m: get_github_download_url(m.group(1), m.group(2))
))
# skUnity resource link
DOWNLOAD_URL_CONVERTERS.append((
	"https?://forums\\.skunity\\.com/resources/(\\d+)/download\\?version=(\\d+)",
	lambda m: m.group(0)
))
# dev.bukkit.org
DOWNLOAD_URL_CONVERTERS.append((
	"https?://dev\\.bukkit\\.org/projects/([a-zA-Z0-9_\\-.]+)(?:/?files/latest/?)?",
	lambda m: f"https://dev.bukkit.org/projects/{m.group(1)}/files/latest"
))
# Direct .jar download
DOWNLOAD_URL_CONVERTERS.append((
	"https?://.+\\.jar",
	lambda m: m.group(0)
))


def get_addons():
	"""
	Gets a list of addons, each addon a tuple (name,url)
	"""
	request = requests.get(SKU_ADDON_DOWNLOAD_URL)
	if request.status_code != 200:
		raise IOError(f"GET request to skUnity API got error code {request.status_code}, body {request.text}")

	data = json.loads(request.text)

	if data["response"] != "success":
		raise IOError(f"skUnity API returned non-success response: {request.text}")

	addon_list = []

	addon_map = data["result"]
	for key in addon_map:
		addon = addon_map[key]

		addon_name = addon["title"]
		addon_url = addon["download_url"]

		addon_list.append((addon_name, addon_url))

	return addon_list


def get_direct_download_url(url):
	"""
	Gets a direct download URL from an addon URL
	"""
	for converter in DOWNLOAD_URL_CONVERTERS:
		pattern = re.compile(converter[0])
		func = converter[1]

		match = pattern.fullmatch(url)
		if match:
			return func(match)
	print(f"Skipped {url} because no matching converter was found")
	return None


def get_github_download_url(owner, repo):
	"""
	Gets a direct download URL from the release asset of a GitHub repo
	"""
	request = requests.get(f"https://api.github.com/repos/{owner}/{repo}/releases",
						   auth=(GITHUB_USERNAME, GITHUB_TOKEN))

	if request.status_code != 200:
		raise IOError(f"GET request to GH {owner}/{repo} gave status code {request.status_code}: {request.text}")

	data = json.loads(request.text)

	if not UNSTABLE_GITHUB_RELEASES:
		for i in reversed(range(len(data))):
			if data[i]["prerelease"]:
				data.pop(i)

	if len(data) == 0:
		raise IOError(f"GH repo {owner}/{repo} doesn't have any (matching) releases")

	data.sort(key=lambda release: dateutil.parser.isoparse(release["created_at"]))

	release = data[-1]

	jar_downloads = []
	for asset in release["assets"]:
		if asset["name"].endswith(".jar"):
			jar_downloads.append(asset["browser_download_url"])

	match len(jar_downloads):
		case 0:
			raise IOError(f"GH last {owner}/{repo} release is a release without .jar assets")
		case 1:
			return jar_downloads[0]
		case other:
			raise IOError(f"GH last {owner}/{repo} release is a release with multiple .jar assets: {jar_downloads}")


def get_file_name(headers, download_url):
	if "Content-Disposition" in headers:
		content_disposition = headers["Content-Disposition"]

		file_name = None

		parts = content_disposition.split(";")
		for part in parts:
			part = part.strip()
			if len(part) == 0:
				continue
			if part.startswith("filename="):
				file_name = part[len("filename="):].strip()

		if file_name.startswith('"') and file_name.endswith('"'):
			file_name = file_name[1:-1]

		if file_name == None:
			raise IOError(f"Illegal Content-Disposition header: {content_disposition}")

		if not file_name.endswith(".jar"):
			raise IOError(f"File name from Content-Disposition isn't a jar file: {file_name}")

		return file_name
	else:
		if not download_url.endswith(".jar"):
			raise IOError(
				f"Response headers ({headers}) doesn't contain Content-Disposition and download URL ({download_url}) "
				f"does not end in .jar")
		split = download_url.split("/")
		return split[-1]


class AddonStatus(Enum):
	UPDATED = 1
	SKIPPED = 2
	EXCEPTION = 3
	NO_CONVERTER_FOUND = 4


class Addon:
	def __init__(self, addon_name, addon_url, status, file_name):
		self.addon_name = addon_name
		self.addon_url = addon_url
		self.status = status
		self.file_name = file_name


class WorkerThread(Thread):
	def __init__(self, addon_downloader, worker_number):
		self.queue = addon_downloader.queue
		self.lock = addon_downloader.lock
		self.addon_downloader = addon_downloader
		super().__init__(name=f"addon-download-thread-{worker_number}")

	def run(self):
		while True:
			with self.lock:
				if self.queue.empty():
					return
				addon = self.queue.get_nowait()

			try:
				addon = self.addon_downloader.do_single_download(addon)
				self.addon_downloader.handled_addons.append(addon)
			except Exception as e:
				print(f"Error downloading addon {addon[0]} from {addon[1]}: {type(e)}: {e}")
				self.addon_downloader.handled_addons.append(Addon(addon[0], addon[1], AddonStatus.EXCEPTION, None))

			self.queue.task_done()


class AddonDownloader:
	def __init__(self):
		self.addons = None
		self.addon_file_names = []

		self.lock = Lock()
		self.queue = Queue()

		self.handled_addons = []

		self.threads = []

		if not os.path.isdir(ADDON_DOWNLOAD_DIRECTORY):
			os.makedirs(ADDON_DOWNLOAD_DIRECTORY)

	def update_addon_list(self):
		if self.addons is not None:
			return
		self.addons = get_addons()
		for addon in self.addons:
			self.queue.put(addon)

	def start_worker(self, worker_number):
		self.update_addon_list()
		thread = WorkerThread(self, worker_number)
		thread.start()
		self.threads.append(thread)

	def start_downloads(self):
		for worker_number in range(DOWNLOAD_THREAD_AMOUNT):
			self.start_worker(worker_number)

	def wait_for_downloads(self):
		self.queue.join()

	def do_single_download(self, addon):
		addon_name = addon[0]
		addon_url = addon[1]

		download_url = get_direct_download_url(addon_url)
		if download_url is not None:
			response = requests.get(download_url)

			file_name = get_file_name(response.headers, response.url)
			file_path = ADDON_DOWNLOAD_DIRECTORY + os.path.sep + file_name

			# Check if addon file needs to be updated
			update = True
			if os.path.isfile(file_path):
				# Get online version hash
				if "Content-MD5" in response.headers:
					# Use MD5 hash if provided
					hash = bytes(response.headers["Content-MD5"], "ASCII")
				else:
					# Create MD5 hash from response
					md5 = hashlib.md5()
					md5.update(response.content)
					hash = base64.b64encode(md5.digest())

				# Check if hash matches
				with open(file_path, 'rb') as file:
					# First, generate local file hash
					md5 = hashlib.md5()
					md5.update(file.read())

					disk_hash = base64.b64encode(md5.digest())
					if disk_hash == hash:
						update = False

			if update:
				with open(file_path, 'wb') as file:
					file.write(response.content)
				return Addon(addon_name, addon_url, AddonStatus.UPDATED, file_name)
			else:
				return Addon(addon_name, addon_url, AddonStatus.SKIPPED, file_name)
		else:
			return Addon(addon_name, addon_url, AddonStatus.NO_CONVERTER_FOUND, None)

	def clean_directory(self):
		file_names = os.listdir(ADDON_DOWNLOAD_DIRECTORY)
		for addon in self.handled_addons:
			if addon.file_name is not None and addon.file_name in file_names:
				file_names.remove(addon.file_name)
		for file_name in file_names:
			os.remove(ADDON_DOWNLOAD_DIRECTORY + os.path.sep + file_name)
		print(f"Deleted files: {', '.join(file_names)}")

	def print_status_report(self):
		updated = []
		skipped = []
		errored = []
		no_converter = []

		for addon in self.handled_addons:
			if addon.status == AddonStatus.UPDATED:
				updated.append((addon.addon_name, addon.file_name))
			elif addon.status == AddonStatus.SKIPPED:
				skipped.append(addon.addon_name)
			elif addon.status == AddonStatus.EXCEPTION:
				errored.append(addon.addon_name)
			elif addon.status == AddonStatus.NO_CONVERTER_FOUND:
				no_converter.append(addon.addon_name)

		print("Skipped addons:")
		for addon in skipped:
			print(f"\t{addon}")

		print()
		print("Updated addons:")
		for addon in updated:
			print(f"\t{addon[0]} ({addon[1]})")

		print()
		print("Unsupported addons:")
		for addon in no_converter:
			print(f"\t{addon}")

		print()
		print("Errored addons:")
		for addon in errored:
			print(f"\t{addon}")


# TODO progress status updates
def main():
	downloader = AddonDownloader()
	downloader.update_addon_list()
	downloader.start_downloads()
	print("Starting downloads")
	downloader.wait_for_downloads()
	print("Downloads done")
	downloader.clean_directory()
	downloader.print_status_report()


if __name__ == "__main__":
	main()
