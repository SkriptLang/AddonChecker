package com.github.tpgamesnl.javausagechecker.query;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Utility method for performing {@link Method a certain} string check against a known string.
 *
 * All checks are case-insensitive.
 */
public class StringCheck {

    /**
     * Different methods for matching the strings
     */
    public enum Method {
        /**
         * An exact match
         *
         * @see String#equals(Object)
         */
        EXACT {
            @Override
            boolean match(String query, String s) {
                return query.equals(s);
            }
        },
        /**
         * A partial match
         *
         * @see String#contains(CharSequence)
         */
        CONTAINS {
            @Override
            boolean match(String query, String s) {
                return s.contains(query);
            }
        },
        /**
         * A partial match, where the query must be surrounded by dots ({@code .}) or by string boundaries.
         */
        CONTAINS_WORD {
            @Override
            boolean match(String query, String s) {
                int i = -1;
                while ((i = s.indexOf(query, i + 1)) != -1) {
                    if (i == 0 || s.charAt(i - 1) == '.') {
                        if (i + query.length() == s.length() || s.charAt(i + query.length()) == '.') {
                            return true;
                        }
                    }
                }
                return false;
            }
        };

        abstract boolean match(String query, String s);
    }

    public static StringCheck getTautology() {
        // Method doesn't matter, null query will always return true
        return new StringCheck(Method.EXACT, null);
    }

    private final Method method;
    @Nullable
    private final String query;

    public StringCheck(Method method, @Nullable String query) {
        this.method = method;
        this.query = query;// == null ? null : query.toLowerCase(Locale.ROOT);
    }

    public boolean match(String s) {
        if (this.query == null)
            return true;

        return this.method.match(this.query, s);
    }

    @Override
    public String toString() {
        if (query == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        switch (method) {
            case EXACT:
                stringBuilder.append("[e]");
                break;
            case CONTAINS:
                stringBuilder.append("[c]");
                break;
            case CONTAINS_WORD:
                stringBuilder.append("[w]");
                break;
        }
        stringBuilder.append(query);
        return stringBuilder.toString();
    }

}
