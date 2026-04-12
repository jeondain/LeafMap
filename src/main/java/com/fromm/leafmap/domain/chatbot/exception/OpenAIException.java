package com.fromm.leafmap.domain.chatbot.exception;

public class OpenAIException extends RuntimeException {

    public OpenAIException(String message, Throwable cause) {
        super(message, cause);
    }
}
