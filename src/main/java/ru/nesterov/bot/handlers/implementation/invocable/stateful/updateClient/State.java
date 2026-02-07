package ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient;

public enum State {
    STARTED,
    SELECT_CLIENT,
    CHANGE_NAME,
    CHANGE_PHONE,
    CHANGE_DESCRIPTION,
    CHANGE_PRICE,
    APPROVE_NAME_CHANGE,
    APPROVE_PHONE_CHANGE,
    APPROVE_DESCRIPTION_CHANGE,
    APPROVE_PRICE_CHANGE,
    FINISH
}

