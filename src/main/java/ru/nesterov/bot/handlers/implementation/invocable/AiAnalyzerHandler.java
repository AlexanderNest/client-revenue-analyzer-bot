package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.AiAnalyzerResponse;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiAnalyzerHandler extends InvocableCommandHandler {
    @Override
    public String getCommand() {
        return "Анализ клиентов ИИ";
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        long chatId = TelegramUpdateUtils.getChatId(update);
        AiAnalyzerResponse response = client.getAiStatistics(chatId);

        return getPlainSendMessage(update.getMessage().getChatId(), response.getContent());
    }

    @Override
    public String getDescription() {
        return "Анализирует Ваши доходы и расходы с помощью ИИ и дает советы.";
    }
}
