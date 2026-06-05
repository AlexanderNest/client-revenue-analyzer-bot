package ru.nesterov.bot;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.handlers.implementation.invocable.UpdateUserControlButtonsHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KeyboardUpdateService {
    private static final String KEY_PREFIX = "keyboard:last-update:%s";

    private final StringRedisTemplate redisTemplate;
    private final BotProperties botProperties;

    private final UpdateUserControlButtonsHandler updateUserControlButtonsHandler;

    @NotNull
    public List<BotApiMethod<?>> getUpdateKeyboard(Update update) {
        String messageText = update.hasMessage() ? update.getMessage().getText() : null;
        if (!shouldUpdateKeyboard(TelegramUpdateUtils.getChatId(update))) {
            return List.of();
        }
        if ("/start".equals(messageText)) {
            shouldUpdateKeyboard(TelegramUpdateUtils.getChatId(update));
            return List.of();
        }
        return updateUserControlButtonsHandler.handle(update);
    }

    private boolean shouldUpdateKeyboard(Long chatId) {

        if (botProperties.isControlButtonsAutoUpdateEnabled()) {
            return true;
        }

        String key = KEY_PREFIX.formatted(chatId);

        if (redisTemplate.hasKey(key)) {
            return false;
        }

        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString(), botProperties.getControlButtonsUpdateIntervalHours(), TimeUnit.HOURS);
        return true;
    }
}
