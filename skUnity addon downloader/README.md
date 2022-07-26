# skUnity addon downloader

The skUnity addon downloader script can be used to download all addons from the skUnity forums - resources section.

First, rename `config_example.txt` to `config.txt` and fill in the missing values. This includes
- A skUnity API key, see [skUnity docs](https://docs.skunity.com/admin/api)
- Your GitHub username
- Your GitHub password or Personal Access Token (the latter is recommended). 
See [GitHub](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)

Additionally, you can modify some other settings in the file.

After this, you can run `addons.py` as you would run any Python file in a command line window 
(i.e. make sure Python is installed, and use `py addons.py`)

This will download all addons, and place them in the `addons` directory (or another if you've changed it in config.txt).

It will give a status report when the program finishes, telling you which addons were downloaded, updated, errored or skipped.
