package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.AiAnalyzerResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.AiAnalyzerHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        AiAnalyzerHandler.class
})
class AiAnalyzerHandlerTest extends RegisteredUserHandlerTest {
    @Test
    void handle() {
        Update update = createUpdateWithMessage("Анализ клиентов ИИ");
        CommandHandler commandHandler = handlerService.getHandler(update);

        assertInstanceOf(AiAnalyzerHandler.class, commandHandler);

        AiAnalyzerResponse aiAnalyzerResponse = new AiAnalyzerResponse();
        aiAnalyzerResponse.setContent("Клиент 1 показал отличные результаты в плане продуктивности и дохода.");
        when(client.getAiStatistics(anyLong())).thenReturn(aiAnalyzerResponse);

        List<BotApiMethod<?>> command = commandHandler.handle(update);
        assertFalse(command.isEmpty());
        assertEquals(1, command.size());
        assertInstanceOf(SendMessage.class, command.get(0));
        SendMessage sendStatistics = (SendMessage) command.get(0);

        assertEquals(aiAnalyzerResponse.getContent(), sendStatistics.getText());
    }
}
