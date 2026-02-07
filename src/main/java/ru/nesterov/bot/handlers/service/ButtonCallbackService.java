package ru.nesterov.bot.handlers.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.handlers.callback.ButtonCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ButtonCallbackService {
    private final Map<String, String> commandToIdMapping = new HashMap<>();
    private final Map<String, String> idToCommandMapping = new HashMap<>();

    public ButtonCallbackService(List<InvocableCommandHandler> commandHandlers) {
        for (int i = 0; i < commandHandlers.size(); i++) {
            commandToIdMapping.put(commandHandlers.get(i).getCommand(), String.valueOf(i));
            idToCommandMapping.put(String.valueOf(i), commandHandlers.get(i).getCommand());
        }
    }

    public String getTelegramButtonCallbackString(ButtonCallback buttonCallback) {
        String result = commandToIdMapping.get(buttonCallback.getCommand()) + ":" + buttonCallback.getValue();
        log.trace("Mapped [{}] to [{}]", buttonCallback.getCommand(), result);
        return result;
    }

    public ButtonCallback buildButtonCallback(String telegramButtonCallbackString) {
        String[] parts = telegramButtonCallbackString.split(":");
        ButtonCallback buttonCallback = new ButtonCallback();
        buttonCallback.setCommand(idToCommandMapping.get(parts[0]));
        buttonCallback.setValue(parts[1]);

        return buttonCallback;
    }
}
