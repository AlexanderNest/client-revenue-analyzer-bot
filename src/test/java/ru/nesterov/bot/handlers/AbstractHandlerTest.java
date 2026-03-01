package ru.nesterov.bot.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.nesterov.bot.config.BotProperties;
import ru.nesterov.bot.handlers.implementation.UndefinedHandler;
import ru.nesterov.bot.handlers.implementation.invocable.CancelCommandHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.getSchedule.InlineCalendarBuilder;
import ru.nesterov.bot.handlers.service.ButtonCallbackService;
import ru.nesterov.bot.handlers.service.HandlersService;
import ru.nesterov.bot.handlers.service.MetricService;
import ru.nesterov.bot.integration.ClientRevenueAnalyzerIntegrationClient;
import ru.nesterov.bot.statemachine.ActionService;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Базовый тест для Handler. Содержит основные бины, которые используют обработчики.
 */
@ContextConfiguration(classes = {
        ObjectMapper.class,
        InlineCalendarBuilder.class,
        HandlersService.class,
        ButtonCallbackService.class,
        BotProperties.class,
        CancelCommandHandler.class,
        ActionService.class,
        UndefinedHandler.class,
        MetricService.class
})
@SpringBootTest
public abstract class AbstractHandlerTest {
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected HandlersService handlerService;
    @Autowired
    protected ButtonCallbackService buttonCallbackService;
    @Autowired
    protected BotProperties botProperties;
    @Autowired
    protected CancelCommandHandler cancelCommandHandler;
    @Autowired
    protected MetricService metricService;
    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    protected ClientRevenueAnalyzerIntegrationClient client;

    @BeforeEach
    void setUp() {
        Counter mockCounter = Mockito.mock(Counter.class);
        Mockito.when(meterRegistry.counter(anyString()))
                .thenReturn(mockCounter);
    }

    protected Update createUpdateWithMessage(String text) {
        Chat chat = new Chat();
        chat.setId(1L);
        User user = new User();
        user.setId(1L);

        Message message = new Message();
        message.setText(text);
        message.setChat(chat);
        message.setFrom(user);

        Update update = new Update();
        update.setMessage(message);

        return update;
    }
}
