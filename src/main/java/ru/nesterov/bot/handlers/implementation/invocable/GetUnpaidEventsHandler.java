package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetUnpaidEventsResponse;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Component
public class GetUnpaidEventsHandler extends InvocableCommandHandler {

    @Override
    public String getCommand() {
        return "Узнать неоплаченные события";
    }

    @Override
    public List<BotApiMethod<?>> handle(Update update) {
        List<GetUnpaidEventsResponse> unpaidEvents = client.getUnpaidEvents(TelegramUpdateUtils.getChatId(update));

        if (unpaidEvents.isEmpty()) {
            return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), "Нет неоплаченных событий");
        }
        String message = formatMessage(unpaidEvents);
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), message);
    }

    private String formatMessage(List<GetUnpaidEventsResponse> events) {
        StringBuilder message = new StringBuilder("Неоплаченные события:\n");
        for (GetUnpaidEventsResponse event : events) {
            message
                    .append("- ")
                    .append(event.getSummary())
                    .append(" (")
                    .append(event.getEventStart().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                    .append(")\n");
        }
        return message.toString();
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
