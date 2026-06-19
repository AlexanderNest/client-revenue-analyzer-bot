package ru.nesterov.bot.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryListenerImpl implements RetryListener {


    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        log.info("=== Запуск операции с поддержкой ретраев ===");
        return true;
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        int attempt = context.getRetryCount();
        log.warn("Попытка №{} завершилась ошибкой: {}. Ожидание следующей попытки", attempt, throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable == null) {
            log.info("=== Операция успешно завершена на попытке №{} ===", context.getRetryCount() + 1);
        } else {
            log.error("=== Все попытки ретрая исчерпаны. Ошибка: {} ===", throwable.getMessage());
        }
    }
}
