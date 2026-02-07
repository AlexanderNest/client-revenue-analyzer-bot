package ru.nesterov.bot.handlers.callback;

import lombok.Data;

@Data
public class ButtonCallback {
    private String command;
    private String value;
}
