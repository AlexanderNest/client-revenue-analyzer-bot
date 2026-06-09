package ru.nesterov.bot.handlers.implementation;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.handlers.abstractions.SendingMessageCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

@Component
public class UndefinedHandler extends SendingMessageCommandHandler {
    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Неизвестная команда");
    }

    @Override
    public boolean isApplicable(Update update) {
        return false; // не должен подбираться, вызывается только вручную сервисом
    }
}
