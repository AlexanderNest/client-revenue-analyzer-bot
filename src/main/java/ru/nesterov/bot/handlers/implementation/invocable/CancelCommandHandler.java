package ru.nesterov.bot.handlers.implementation.invocable;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.handlers.abstractions.Priority;
import ru.nesterov.bot.handlers.service.HandlersService;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

@Component
public class CancelCommandHandler extends InvocableCommandHandler {
    @Resource
    @Lazy
    private HandlersService handlersService;

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        long chatId = TelegramUpdateUtils.getChatId(update);

        handlersService.resetAllHandlers(chatId);
        return getPlainSendMessage(chatId, "Контекст сброшен");
    }

    @Override
    public Priority getPriority() {
        return Priority.HIGHEST;
    }

    @Override
    public String getCommand() {
        return "/cancel";
    }
}
