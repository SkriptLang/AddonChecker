package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.query.ClassQuery;
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

        for (Query query : builder.getQueries()) {
            System.out.println("Query: " + query);
        }

        builder.stateTracker(new StateTracker() {
            @Override
            void updateState(State state) {
                System.out.println("New state: " + state);
            }

            @Override
            void jarOpenedCountUpdated(int count) {
                System.out.println("jars opened: " + count);
            }

            @Override
            void classesCheckedCountUpdated(int count) {
                System.out.println("classes checked: " + count);
            }
        });

        List<Report> reports = builder.create()
                .start()
                .join()
                .getReports();

        for (Report report : reports) {
            System.out.println("-  " + report);
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

        // type: starts with 'f', 'm' or 'c'
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

            StringCheck.Method method = StringCheck.Method.CONTAINS;
            if (value.startsWith("[c]")) {
                method = StringCheck.Method.CONTAINS;
                value = value.substring(3);
            } else if (value.startsWith("[e]")) {
                method = StringCheck.Method.EXACT;
                value = value.substring(3);
            } else if (value.startsWith("[w]")) {
                method = StringCheck.Method.CONTAINS_WORD;
                value = value.substring(3);
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
            case "c": {
                StringCheck nameCheck = getCheck(tags, "n", "name");
                return new ClassQuery(nameCheck);
            }
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

        out.println("Usage: java -jar JavaUsageChecker.jar <options>");
        out.println();
        out.println("Possible options:");
        out.println("  f:<file path>");
        out.println("  d:<directory path>");
        out.println("  q:<query>");
        out.println();
        out.println("Query syntax:");
        out.println("  <prefix>:<key>=<value>;<key>=<value>;<key>=<value> etc");
        out.println();
        out.println("  Possible prefixes are c(lass), f(ield) and m(ethod)");
        out.println();
        out.println("  For all prefixes, the n(ame) key refers to the name of the respective object (class, field or method)");
        out.println("  For fields and methods, o(wner) refers to the class that defined the member");
        out.println("  For fields and methods, d(escriptor) refers to the (method or field) descriptor of the member");
        out.println();
        out.println("  All values can be prefixed by either [c], [e] or [w] ([c] being the default)");
        out.println("  These are different methods of string comparisons:");
        out.println("    [c] checks if the value is contained in the string");
        out.println("    [e] checks if the value is exactly equal to the string");
        out.println("    [w] checks if the value is a word (separated by .) within the string, mostly useful for class names");
        out.println();
        out.println();
        out.println("Examples:");
        out.println("  java -jar JavaUsageChecker.jar f:MyJavaProgram.jar q:m:n=get");
        out.println("    looks for method usages of methods with names containing 'get' in the file MyJavaProgram.jar");
        out.println("  java -jar JavaUsageChecker.jar d:\"java testing\" q:c:n=[w]Info");
        out.println("    looks for class usages of classes with names containing the word 'Info' in the directory 'java testing'");
    }

}
