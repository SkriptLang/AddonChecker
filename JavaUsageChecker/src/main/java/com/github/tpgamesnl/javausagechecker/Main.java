package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.query.FieldQuery;
import com.github.tpgamesnl.javausagechecker.query.MethodQuery;
import com.github.tpgamesnl.javausagechecker.query.Query;
import com.github.tpgamesnl.javausagechecker.query.StringCheck;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendUsage(false);
            return;
        }

        JavaUsageChecker.Builder builder = JavaUsageChecker.builder();

        for (String arg : args) {
            String[] parts = arg.split(":");
            if (parts.length == 1) {
                sendUsage();
                return;
            }

            String key = parts[0];
            String value = Arrays.stream(parts)
                    .skip(1)
                    .collect(Collectors.joining(":"));
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            switch (key.toLowerCase(Locale.ROOT)) {
                // File or Directory
                case "f":
                case "d": {
                    File file = new File(value);
                    if (!file.exists()) {
                        System.err.println("The file/directory '" + value + "' does not exist");
                        return;
                    }

                    builder.scans(file);

                    break;
                }

                // Query
                case "q": {
                    Query query = parseQuery(value);
                    if (query == null) {
                        System.err.println("Invalid query syntax: '" + value + "'");
                        return;
                    }

                    builder.uses(query);

                    break;
                }

                // Worker threads
                case "t": {
                    int threadCount;
                    try {
                        threadCount = Integer.parseInt(value);
                        if (threadCount <= 0) {
                            throw new RuntimeException("Only positive not allowed");
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid thread count: " + value + " (" + e.getMessage() + ")");
                        return;
                    }

                    builder.threadCount(threadCount);

                    break;
                }

                default: {
                    sendUsage();
                    return;
                }
            }

        }

        if (builder.getFiles().size() == 0) {
            System.err.println("No files or directories specified");
            return;
        }

        if (builder.getQueries().size() == 0) {
            System.err.println("No queries specified");
            return;
        }

        Map<Query, List<ReportedUsage>> reports = builder.create()
                .start()
                .join()
                .getReports();

        for (Query query : reports.keySet()) {
            List<ReportedUsage> reportedUsages = reports.get(query);
            System.out.println("Query: " + query + " (" + reportedUsages.size() + " found)");
            for (ReportedUsage usage : reportedUsages) {
                System.out.println("-  " + usage);
            }
        }
    }

    private static final String QUOTE_STRING = "([^\"]+|\"[^\"]+\")";
    private static final Pattern QUERY_PATTERN = Pattern.compile(
            "(?<type>f(ield)?|m(ethod)?|c(class)?):" +
                    "(?<tags>([a-zA-Z-_]+="+QUOTE_STRING+"(;|$))+)"
    );

    @Nullable
    private static Query parseQuery(String s) {
        Matcher matcher = QUERY_PATTERN.matcher(s);
        if (!matcher.matches()) {
            return null;
        }

        // type: 'f', 'm' or 'c'
        String type = matcher.group("type");

        String tagsString = matcher.group("tags");
        Map<String, StringCheck> tags = new HashMap<>();
        for (String tagString : tagsString.split(";")) {
            int split = tagString.indexOf('=');

            String key = tagString.substring(0, split);

            String value = tagString.substring(split + 1);

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            StringCheck.Method method;
            if (value.startsWith("[c]")) {
                method = StringCheck.Method.CONTAINS;
                value = value.substring(3);
            } else if (value.startsWith("[e]")) {
                method = StringCheck.Method.EXACT;
                value = value.substring(3);
            } else {
                method = StringCheck.Method.CONTAINS_WORD;
            }

            StringCheck stringCheck = new StringCheck(method, value);

            tags.put(key, stringCheck);
        }

        switch (type) {
            case "f":
            case "field":
            case "m":
            case "method": {
                StringCheck ownerCheck = getCheck(tags, "o", "owner");
                StringCheck nameCheck = getCheck(tags, "n", "name");
                StringCheck descriptorCheck = getCheck(tags, "d", "descriptor");

                if (type.startsWith("f")) {
                    return new FieldQuery(ownerCheck, nameCheck, descriptorCheck);
                } else {
                    return new MethodQuery(ownerCheck, nameCheck, descriptorCheck);
                }
            }

            // TODO class
        }

        return null;
    }

    private static StringCheck getCheck(Map<String, StringCheck> tags, String... keys) {
        for (String key : keys) {
            StringCheck stringCheck = tags.get(key);
            if (stringCheck != null) {
                return stringCheck;
            }
        }
        return StringCheck.getTautology();
    }

    private static void sendUsage() {
        sendUsage(true);
    }

    private static void sendUsage(boolean invalidSyntax) {
        PrintStream out = invalidSyntax ? System.err : System.out;

        // TODO usage
        out.println("usage");
    }

}
