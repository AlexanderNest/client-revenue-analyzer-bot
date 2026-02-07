package ru.nesterov.bot.exception;

public class UserFriendlyException extends RuntimeException {
    public UserFriendlyException(String message) {
        super(message);
    }
}
