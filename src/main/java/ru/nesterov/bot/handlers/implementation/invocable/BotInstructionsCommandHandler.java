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
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BotInstructionsCommandHandler extends InvocableCommandHandler {
    private static final String COMMANDS_PLACEHOLDER = "commands";
    private static final String CANCEL_COMMAND_PLACEHOLDER = "cancel_command";
    private static final String CREATOR_CONTACT_PLACEHOLDER = "creator_contact";

    private final String cachedTemplate;

    private final List<InvocableCommandHandler> allHandlers;
    private final BotProperties botProperties;

    public BotInstructionsCommandHandler(@Lazy List<InvocableCommandHandler> allHandlers,
                                         @Value("classpath:templates/help_message.md") Resource helpTemplate,
                                         BotProperties botProperties) {
        this.allHandlers = allHandlers;
        this.botProperties = botProperties;
        this.cachedTemplate = readTemplate(helpTemplate);
    }

    private String readTemplate(Resource resource) {
        byte[] bdata;
        try (InputStream is = resource.getInputStream()){
            bdata = FileCopyUtils.copyToByteArray(is);
        } catch (IOException e) {
            throw new UserFriendlyException("Ошибка при формировании описания команд");
        }
        return new String(bdata, StandardCharsets.UTF_8);
    }

    @Override
    public String getCommand() {
        return "Инструкция по работе с ботом";
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


    /**
     * hash = длина слова
     * при каких одинаковых словах будет разныей хэш?
     *
     * @return
     */
    private String getCommandsDescription() {
        InvocableCommandHandler cancelCommandHandler = allHandlers.stream()
                .filter(h -> h instanceof CancelCommandHandler)
                .findFirst()
                .orElseThrow();

        String commandsInfo = allHandlers.stream()
                .filter(h ->  !h.getDescription().isBlank())
                .filter(h -> h != cancelCommandHandler)
                .map(h -> "- *%s* - %s".formatted (h.getCommand(), h.getDescription()))
                .collect(Collectors.joining("\n"));

        Map<String, String> values = Map.of(
                COMMANDS_PLACEHOLDER, commandsInfo,
                CANCEL_COMMAND_PLACEHOLDER, cancelCommandHandler.getCommand(),
                CREATOR_CONTACT_PLACEHOLDER, botProperties.getCreatorContact()
        );

        StringSubstitutor sub = new StringSubstitutor(values);
        return sub.replace(cachedTemplate);
    }


}
