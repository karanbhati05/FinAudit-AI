package com.finaudit.core.model;

public record AskRequest(
        String question
) {
    public AskRequest {
        if (question == null) {
            question = "";
        }
    }
}
