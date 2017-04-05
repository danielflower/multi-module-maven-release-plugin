package de.hilling.maven.release;

import static java.util.Arrays.asList;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> messages;

    public ValidationException(String summary, List<String> messages) {
        super(summary);
        this.messages = messages;
    }

    public ValidationException(String summary, Throwable error) {
        super(summary);
        this.messages = asList(summary, "" + error);
    }

    public ValidationException(String message) {
        this(message, asList(message));
    }

    public List<String> getMessages() {
        return messages;
    }
}
