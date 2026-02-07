package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.nesterov.bot.dto.GetUnpaidEventsResponse;
import ru.nesterov.bot.handlers.RegisteredUserHandlerTest;
import ru.nesterov.bot.handlers.implementation.invocable.GetUnpaidEventsHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        GetUnpaidEventsHandler.class
})
public class GetUnpaidEventsHandlerTest extends RegisteredUserHandlerTest {
    @Autowired
    private GetUnpaidEventsHandler getUnpaidEventsHandler;

    @Test
    void handleWithoutUnpaidEvents() {
        Chat chat = new Chat();
        chat.setId(1L);
        User user = new User();
        user.setId(1L);
        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);
        Update update = new Update();
        update.setMessage(message);

        when(client.getUnpaidEvents(user.getId())).thenReturn(Collections.emptyList());
        List<BotApiMethod<?>> response = getUnpaidEventsHandler.handle(update);
        Assertions.assertInstanceOf(SendMessage.class, response.get(0));
        SendMessage sendMessage = (SendMessage) response.get(0);
        Assertions.assertEquals("Нет неоплаченных событий", sendMessage.getText());
    }

    @Test
    void handleWithUnpaidEvents() {
        Chat chat = new Chat();
        chat.setId(1L);
        User user = new User();
        user.setId(1L);
        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);
        Update update = new Update();
        update.setMessage(message);

        GetUnpaidEventsResponse getUnpaidEventsResponse1 = new GetUnpaidEventsResponse();
        getUnpaidEventsResponse1.setEventStart(LocalDateTime.of(2025, 6, 15, 14, 30));
        getUnpaidEventsResponse1.setSummary("Неоплаченное событие");

        GetUnpaidEventsResponse getUnpaidEventsResponse2 = new GetUnpaidEventsResponse();
        getUnpaidEventsResponse2.setEventStart(LocalDateTime.of(2025, 6, 15, 14, 30));
        getUnpaidEventsResponse2.setSummary("Неоплаченное событие");

        List<GetUnpaidEventsResponse> unpaidEvents = new ArrayList<>();
        unpaidEvents.add(getUnpaidEventsResponse1);
        unpaidEvents.add(getUnpaidEventsResponse2);
        when(client.getUnpaidEvents(user.getId())).thenReturn(unpaidEvents);

        List<BotApiMethod<?>> response = getUnpaidEventsHandler.handle(update);
        Assertions.assertInstanceOf(SendMessage.class, response.get(0));
        SendMessage sendMessage = (SendMessage) response.get(0);
        String expectedMessage = "Неоплаченные события:\n" +
                "- Неоплаченное событие (15.06.2025 14:30)\n" +
                "- Неоплаченное событие (15.06.2025 14:30)\n";
        Assertions.assertEquals(expectedMessage, sendMessage.getText());
    }
}
