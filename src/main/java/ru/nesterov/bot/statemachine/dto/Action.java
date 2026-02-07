package ru.nesterov.bot.statemachine.dto;

public enum Action {
    COMMAND_INPUT,
    TRUE,
    FALSE,
    ANY_BOOLEAN,
    ANY_NUMBER,
    ANY_STRING,
    CALLBACK_TRUE,
    CALLBACK_FALSE,
    ANY_CALLBACK_BOOLEAN,
    ANY_CALLBACK_INPUT,
    CALLBACK_DATE,
    CALLBACK_PREV,
    CALLBACK_NEXT,
    CALLBACK_TODAY
}
