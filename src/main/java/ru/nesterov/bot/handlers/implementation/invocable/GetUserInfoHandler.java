package ru.nesterov.bot.handlers.implementation.invocable;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetUserRequest;
import ru.nesterov.bot.dto.GetUserResponse;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

@Component
public class GetUserInfoHandler extends InvocableCommandHandler {

    @Override
    public String getCommand() {
        return "Информация о пользователе";
    }

    @Override
    public List<PartialBotApiMethod<?>> handle(Update update) {
        long telegramUserId = TelegramUpdateUtils.getUserId(update);

        GetUserRequest request = new GetUserRequest();
        request.setUsername(String.valueOf(telegramUserId));
        GetUserResponse user = client.getUserByUsername(request);

        long chatId = TelegramUpdateUtils.getChatId(update);
        if (user == null) {
            return getPlainSendMessage(chatId, "Пользователь не найден в системе.");
        }

        String message = String.format(
                "Информация о пользователе:%n" +
                "Telegram ID: %d%n" +
                "ID в системе: %d%n" +
                "Username: %s%n",
                telegramUserId,
                user.getUserId(),
                update.getCallbackQuery().getFrom().getUserName()
        );

        return getPlainSendMessage(chatId, message);
    }
}
