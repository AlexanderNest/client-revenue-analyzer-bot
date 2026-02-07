package ru.nesterov.bot.handlers;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.nesterov.bot.dto.GetUserResponse;
import ru.nesterov.bot.dto.Role;
import ru.nesterov.bot.handlers.implementation.invocable.UpdateUserControlButtonsHandler;
import ru.nesterov.bot.handlers.implementation.invocable.stateful.createUser.CreateUserHandler;
import ru.nesterov.bot.handlers.service.ButtonCallbackService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Базовый тест для Handler для работы с зарегистрированным пользователем. Содержит основные бины, которые используют обработчики.
 */
@ContextConfiguration(classes = {
        ButtonCallbackService.class,
        UpdateUserControlButtonsHandler.class,
        CreateUserHandler.class
})
public abstract class RegisteredUserHandlerTest extends AbstractHandlerTest {
    @Autowired
    protected UpdateUserControlButtonsHandler updateUserControlButtonsHandler;

    @PostConstruct
    private void mockBean(){
        GetUserResponse getUserResponse =
                GetUserResponse.builder()
                        .userId(1L)
                        .username("UserName")
                        .mainCalendarId("mainCalendar")
                        .isCancelledCalendarEnabled(true)
                        .cancelledCalendarId("cancelCalendar")
                        .role(Role.USER)
                        .source(null)
                        .build();

        when(client.getUserByUsername(any())).thenReturn(getUserResponse);
    }
}
