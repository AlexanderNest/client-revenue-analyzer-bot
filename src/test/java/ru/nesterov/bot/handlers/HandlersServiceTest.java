package ru.nesterov.bot.handlers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.GetUnpaidEventsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createClient.CreateClientHandler;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ContextConfiguration(
        classes = {
                CreateClientHandler.class,
                GetUnpaidEventsHandler.class
        }
)
public class HandlersServiceTest extends RegisteredUserHandlerTest {
    @Autowired
    private CreateClientHandler createClientHandler;
    @Autowired
    private GetUnpaidEventsHandler getUnpaidEventsHandler;

    @Test
    public void shouldResetContextWhenNewCommandWasInput() {
        System.out.println(createClientHandler.handle(createUpdateWithMessage(createClientHandler.getCommand())));
        CommandHandler commandHandler = handlerService.getHandler(createUpdateWithMessage(getUnpaidEventsHandler.getCommand()));

        assertInstanceOf(GetUnpaidEventsHandler.class, commandHandler);
    }

    @Test
    public void shouldNotResetStarterHandler() {
        createClientHandler.handle((createUpdateWithMessage(createClientHandler.getCommand())));
        CommandHandler commandHandler = handlerService.getHandler(createUpdateWithMessage("Вася"));

        assertInstanceOf(CreateClientHandler.class, commandHandler);
    }
}
