package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.exception.UserFriendlyException;
import ru.nesterov.bot.handlers.abstractions.GroupingCommandHandler;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HelpCommandHandler extends GroupingCommandHandler {
    private static final String COMMANDS_PLACEHOLDER = "commands";
    private static final String CANCEL_COMMAND_PLACEHOLDER = "cancel_command";
    private static final String CREATOR_CONTACT_PLACEHOLDER = "creator_contact";
    private final List<InvocableCommandHandler> allHandlers;
    private final String cachedTemplate;
    private final BotProperties botProperties;

    public HelpCommandHandler(@Lazy List<InvocableCommandHandler> allHandlers,
                              @Value("classpath:templates/help_message.md") Resource helpTemplate,
                              BotProperties botProperties) {
        super(List.of());
        this.allHandlers = allHandlers;
        this.botProperties = botProperties;
        this.cachedTemplate = readTemplate(helpTemplate);


    }

    private String readTemplate(Resource resource) {
        byte[] bdata;
        try {
            bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            throw new UserFriendlyException("Ошибка при формировании описания команд");
        }
        return new String(bdata, StandardCharsets.UTF_8);
    }

    @Override
    public String getCommand() {
        return "Помощь";
    }

    @Override
    public String getDescription() {
        return "Показать справку по всем доступным функциям бота";
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update){
        String finalMessage = getCommandsDescription();

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(TelegramUpdateUtils.getChatId(update)));
        message.setText(finalMessage);
        message.setParseMode("Markdown");

        return List.of(message);
    }

    private String getCommandsDescription() {
        String commandsInfo = allHandlers.stream()
                .filter(h ->  !h.getDescription().isBlank())
                .filter(h -> !(h instanceof CancelCommandHandler))
                .map(h -> "- *%s* - %s".formatted (h.getCommand(), h.getDescription()))
                .collect(Collectors.joining("\n"));
        String cancelCommand = allHandlers.stream()
                .filter(h -> h instanceof CancelCommandHandler)
                .map(InvocableCommandHandler::getCommand)
                .findFirst()
                .orElse("/cancel");
        Map<String, String> values = Map.of(
                COMMANDS_PLACEHOLDER, commandsInfo,
                CANCEL_COMMAND_PLACEHOLDER, cancelCommand,
                CREATOR_CONTACT_PLACEHOLDER, botProperties.getCreatorContact());
        StringSubstitutor sub = new StringSubstitutor(values);
        return sub.replace(cachedTemplate);
    }


}
