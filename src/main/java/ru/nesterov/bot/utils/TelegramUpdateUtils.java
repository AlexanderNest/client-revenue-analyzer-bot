package ru.nesterov.bot.utils;

import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramUpdateUtils {
    public static long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else {
            return update.getCallbackQuery().getMessage().getChatId();
        }
    }

    public static long getUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else {
            return update.getCallbackQuery().getFrom().getId();
        }
    }

    public static int getMessageId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getMessageId();
        } else {
            return update.getCallbackQuery().getMessage().getMessageId();
        }
    }
}
