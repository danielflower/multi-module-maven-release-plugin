package com.github.danielflower.mavenplugins.release;

import java.util.List;

public class ValidationException extends Exception {
    private final List<String> messages;

    public ValidationException(String summary, List<String> messages) {
        super(summary);
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
