package ru.nesterov.bot.handlers.implementation.invocable.stateful.deleteClient;

public enum State {
    STARTED,
    SELECT_CLIENT,
    APPROVE_DELETING,
    FINISH
}
