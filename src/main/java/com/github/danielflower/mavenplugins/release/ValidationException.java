package com.github.danielflower.mavenplugins.release;

import java.util.Arrays;
import java.util.List;

public class ValidationException extends Exception {
    private final List<String> messages;

    public ValidationException(String summary, List<String> messages) {
        super(summary);
        this.messages = messages;
    }

    public ValidationException(String summary, Throwable error) {
        super(summary);
        this.messages = Arrays.asList(summary, "" + error);
    }

    public List<String> getMessages() {
        return messages;
    }
}
