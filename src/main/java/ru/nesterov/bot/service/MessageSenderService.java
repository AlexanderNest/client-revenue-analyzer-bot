package ru.nesterov.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.SocketException;
import java.util.List;

@Service
@Slf4j
public class MessageSenderService {

    @Retryable(
            retryFor = {TelegramApiException.class, SocketException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 3000),
            listeners = "retryListenerImpl"
    )
    public void sendMessage(AbsSender absSender, List<BotApiMethod<?>> sendMessages) throws TelegramApiException {
        for (BotApiMethod<?> message : sendMessages) {
            log.debug("Отправка сообщения с содержимым: {}", message);
            absSender.execute(message);
            log.debug("Отправлено");
        }
    }
}
