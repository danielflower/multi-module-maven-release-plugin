package com.github.danielflower.mavenplugins.release;

public class Guard {

    public static void notNull(String thing, Object value) {
        if (value == null) {
            throw new GuardException("The value for " + thing + " cannot be null");
        }
    }

    public static void notBlank(String thing, String value) {
        notNull(thing, value);
        if (value.trim().length() == 0) {
            throw new GuardException("The value for " + thing + " cannot be a blank string");
        }
    }

    public static class GuardException extends RuntimeException {
        public GuardException(String message) {
            super(message);
        }
    }

}
