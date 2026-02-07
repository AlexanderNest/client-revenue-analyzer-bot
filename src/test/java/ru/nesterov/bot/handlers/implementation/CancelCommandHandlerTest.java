package ru.nesterov.bot.handlers.implementation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.nesterov.bot.handlers.AbstractHandlerTest;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createClient.CreateClientHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser.CreateUserHandler;
import ru.nesterov.bot.handlers.service.HandlersService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ContextConfiguration(classes = {
        HandlersService.class,
        CreateClientHandler.class,
        CreateUserHandler.class
})
public class CancelCommandHandlerTest  extends AbstractHandlerTest {
    @Autowired
    private CreateClientHandler createClientHandler;

    @Test
    void cancelHandlers() {
        SendMessage nameInput = send("Добавить клиента", createClientHandler);
        assertEquals("Введите имя", nameInput.getText());

        SendMessage priceInput = send("", createClientHandler);
        assertEquals("Введите стоимость за час", priceInput.getText());

        SendMessage contextReset = send("/cancel", cancelCommandHandler);
        assertEquals("Контекст сброшен", contextReset.getText());

        SendMessage nameInputAfterReset = send("Добавить клиента", createClientHandler);
        assertEquals("Введите имя", nameInputAfterReset.getText());
    }

    private SendMessage send(String text, CommandHandler commandHandler) {
        Chat chat = new Chat();
        chat.setId(1L);

        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setText(text);
        message.setFrom(user);
        message.setChat(chat);

        Update update = new Update();
        update.setMessage(message);

        List<BotApiMethod<?>> botApiMethod = commandHandler.handle(update);
        assertFalse(botApiMethod.isEmpty());
        assertInstanceOf(SendMessage.class, botApiMethod.get(0));

        return (SendMessage) botApiMethod.get(0);
    }
}
