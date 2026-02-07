package ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule;

public enum State {
    STARTED,
    SELECT_CLIENT,
    SELECT_FIRST_DATE,
    SELECT_SECOND_DATE,
    SECOND_DATE_SELECTED,
    FINISH
}
