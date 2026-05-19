package ru.nesterov.bot.handlers.implementation.invocable;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HelpCommandHandler extends InvocableCommandHandler {
    private final List<InvocableCommandHandler> allHandlers;

    @Value("classpath:templates/help_message.md")
    private Resource helpTemplate;

    public HelpCommandHandler(@Lazy List<InvocableCommandHandler> allHandlers) {
        this.allHandlers = allHandlers;
    }

    @Override
    public String getCommand() {
        return "/help";
    }

    @Override
    public String getDescription() {
        return "Показать справку по всем доступным функциям бота";
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update){
        String commandsInfo = allHandlers.stream()
                .filter(h ->  !h.getDescription().isBlank())
                .map(h -> String.format("- *%s* - %s", h.getCommand(), h.getDescription()))
                .collect(Collectors.joining("\n"));

        String finalMessage = fillTemplate(commandsInfo);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(TelegramUpdateUtils.getChatId(update)));
        message.setText(finalMessage);
        message.setParseMode("Markdown");

        return List.of(message);
    }

    private String fillTemplate(String commandsInfo) {
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(helpTemplate.getInputStream());
            String template = new String(bdata, StandardCharsets.UTF_8);

            return template.replace("${commands}", commandsInfo);
        } catch (Exception e) {
            return "Ошибка при генерации помощи";
        }
    }


}
