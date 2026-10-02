package com.finaudit.core.model;

public record AskResponse(
        String answer,
        boolean grounded,
        String sourceType
) {
    public static final String REFUSAL_MESSAGE = "That's not something I can answer from this report's data.";

    public static AskResponse ungrounded() {
        return new AskResponse(REFUSAL_MESSAGE, false, "NONE");
    }

    public static AskResponse ungrounded(String message) {
        return new AskResponse(message, false, "NONE");
    }

    public static AskResponse grounded(String answer, String sourceType) {
        return new AskResponse(answer, true, sourceType);
    }
}
