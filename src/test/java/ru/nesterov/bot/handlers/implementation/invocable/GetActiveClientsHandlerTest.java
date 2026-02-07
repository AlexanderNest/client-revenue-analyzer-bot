package ru.nesterov.bot.handlers.implementation.invocable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetActiveClientResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        GetActiveClientsHandler.class
})
class GetActiveClientsHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private GetActiveClientsHandler getActiveClientsHandler;

    @Test
    void handleShouldReturnFormattedMessage() {
        Update update = createUpdateWithMessage(getActiveClientsHandler.getCommand());

        GetActiveClientResponse clientResponse = new GetActiveClientResponse();
        clientResponse.setName("Макс");
        clientResponse.setPricePerHour(999);
        clientResponse.setDescription("SSS");
        GetActiveClientResponse clientResponse2 = new GetActiveClientResponse();
        clientResponse2.setName("Анна");
        clientResponse2.setPricePerHour(100);
        clientResponse2.setDescription("Zzz");

        List<GetActiveClientResponse> getActiveClientsResponseList = List.of(clientResponse, clientResponse2);

        when(client.getActiveClients(1L)).thenReturn(getActiveClientsResponseList);

        List<BotApiMethod<?>> result = getActiveClientsHandler.handle(update);

        String expectedMessage =
                "1. Макс" + System.lineSeparator() +
                "     Тариф: 999 руб/час" + System.lineSeparator() +
                "     Описание: SSS" + System.lineSeparator() + System.lineSeparator() +
                "2. Анна" + System.lineSeparator() +
                "     Тариф: 100 руб/час" + System.lineSeparator() +
                "     Описание: Zzz" + System.lineSeparator() + System.lineSeparator();

        assertEquals(1, result.size());
        SendMessage sendMessage = (SendMessage) result.get(0);
        assertEquals(expectedMessage, sendMessage.getText());
    }

    @Test
    void handleShouldReturnNoClientsMessage() {
        Update update = createUpdateWithMessage(getActiveClientsHandler.getCommand());

        when(client.getActiveClients(1L)).thenReturn(Collections.emptyList());

        List<BotApiMethod<?>> result = getActiveClientsHandler.handle(update);
        assertEquals(1, result.size());
        SendMessage sendMessage = (SendMessage) result.get(0);

        assertEquals("У вас пока нет клиентов.", sendMessage.getText());
    }
}
