package ru.nesterov.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.handlers.abstractions.CommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.UpdateUserControlButtonsHandler;
import ru.nesterov.bot.handlers.service.HandlersService;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class RevenueAnalyzerBot extends TelegramLongPollingBot {
    private final HandlersService handlersService;
    private final BotProperties botProperties;
    private final ExecutorService executorService;
    private final KeyboardUpdateService keyboardUpdateService;
    private final UpdateUserControlButtonsHandler updateUserControlButtonsHandler;


    public RevenueAnalyzerBot(BotProperties botProperties, HandlersService handlersService,
                              ExecutorService executorService,
                              UpdateUserControlButtonsHandler updateUserControlButtonsHandler,
                              KeyboardUpdateService keyboardUpdateService) {
        super(botProperties.getApiToken());

        this.handlersService = handlersService;
        this.botProperties = botProperties;
        this.executorService = executorService;
        this.updateUserControlButtonsHandler = updateUserControlButtonsHandler;
        this.keyboardUpdateService = keyboardUpdateService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.execute(() -> handleUpdate(update));
    }

    public void handleUpdate(Update update) {
        log.debug("Получен update с содержимым: {}", update);

        long chatId = TelegramUpdateUtils.getChatId(update);

        CommandHandler commandHandler = handlersService.getHandler(update);
        if (commandHandler == null) {
            log.error("Не удалось обработать сообщение");
            return;
        }

        log.debug("Выбранный CommandHandler = {}", commandHandler.getClass().getSimpleName());

        List<PartialBotApiMethod<?>> sendMessages;

        try {
            sendMessages = commandHandler.handle(update);
        } catch (Exception exception) {
            handlersService.resetBrokeHandler(commandHandler, chatId);
            sendMessages = buildTextMessage(update, exception.getMessage());
        } finally {
            handlersService.resetFinishedHandlers(chatId);
        }

        sendMessages = enrichWithCommandButtons(sendMessages, update);
        sendMessage(sendMessages);
    }

    private List<PartialBotApiMethod<?>> enrichWithCommandButtons(List<PartialBotApiMethod<?>> sendMessages, Update update) {
        List<PartialBotApiMethod<?>> mutableList = new ArrayList<>(sendMessages);
        mutableList.addAll(keyboardUpdateService.getUpdateKeyboard(update));

        return mutableList;
    }

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }

    private List<PartialBotApiMethod<?>> buildTextMessage(Update update, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(TelegramUpdateUtils.getChatId(update));
        message.setText(text);

        return List.of(message);
    }

    private void sendMessage(List<PartialBotApiMethod<?>> sendMessages) {
        for (PartialBotApiMethod<?> message : sendMessages) {
            try {
                log.debug("Отправка сообщения с содержимым: {}", message);
                if (message instanceof SendDocument sendDocument) {
                    execute(sendDocument);
                } else {
                    execute((BotApiMethod<?>) message);
                }
                log.debug("Отправлено");
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки сообщения", e);
            }
        }
    }
}