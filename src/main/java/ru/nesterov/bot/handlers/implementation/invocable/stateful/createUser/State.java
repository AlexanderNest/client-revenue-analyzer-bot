package ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser;

public enum State {
    STARTED,
    MAIN_CALENDAR_INPUT,
    CANCELLED_CALENDAR_ENABLED_QUESTION,
    CANCELLED_CALENDAR_ID_INPUT,
    FINISH
}
