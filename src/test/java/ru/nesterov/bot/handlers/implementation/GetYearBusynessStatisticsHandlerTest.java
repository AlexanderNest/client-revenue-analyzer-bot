package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetYearBusynessStatisticsResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getYearBusynessStatistics.GetYearBusynessStatisticsHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        GetYearBusynessStatisticsHandler.class,
})
class GetYearBusynessStatisticsHandlerTest extends RegisteredUserHandlerTest {
    @Test
    void handle() {
        Update update = createUpdateWithMessage("Анализ занятости за год");
        CommandHandler commandHandler = handlerService.getHandler(update);

        List<BotApiMethod<?>> command = commandHandler.handle(update);

        assertInstanceOf(SendMessage.class, command.get(0));

        SendMessage sendMessage = (SendMessage) command.get(0);
        assertEquals("Введите год для расчета занятости", sendMessage.getText());

        List<BotApiMethod<?>> wrongYearInput = commandHandler.handle(createUpdateWithMessage("fff"));
        SendMessage wrongYear = (SendMessage) wrongYearInput.get(0);
        assertEquals("Введите корректный год", wrongYear.getText());

        Map<String, Double> months = new LinkedHashMap<>();
        months.put("Август", 10.2532133123);
        months.put("Июль", 20.0);

        Map<String, Double> days = new LinkedHashMap<>();
        days.put("Среда", 7.25);
        days.put("Понедельник", 3.0);

        GetYearBusynessStatisticsResponse getYearBusynessStatisticsResponse = new GetYearBusynessStatisticsResponse();
        getYearBusynessStatisticsResponse.setMonths(months);
        getYearBusynessStatisticsResponse.setDays(days);

        when(client.getYearBusynessStatistics(anyLong(), anyInt())).thenReturn(getYearBusynessStatisticsResponse);

        List<BotApiMethod<?>> botApiMethod = commandHandler.handle(createUpdateWithMessage("2024"));
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));
        SendMessage sendStatistics = (SendMessage) botApiMethod.get(0);

        String expectedMessage = "\uD83D\uDCCA Анализ занятости за год:\n" +
                "\n" +
                "\uD83D\uDDD3\uFE0F Занятость по месяцам:\n" +
                "Август: 10.25 ч.\n" +
                "Июль: 20.00 ч.\n" +
                "\n" +
                "\uD83D\uDCC5 Занятость по дням недели:\n" +
                "Среда: 7.25 ч.\n" +
                "Понедельник: 3.00 ч.";

        assertEquals(expectedMessage, sendStatistics.getText());
    }
}