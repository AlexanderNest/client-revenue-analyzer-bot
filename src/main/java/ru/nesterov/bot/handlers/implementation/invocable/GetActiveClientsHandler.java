package ru.nesterov.bot.handlers.implementation.invocable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.nesterov.bot.dto.GetActiveClientResponse;
import ru.nesterov.bot.handlers.abstractions.InvocableCommandHandler;
import ru.nesterov.bot.utils.TelegramUpdateUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetActiveClientsHandler extends InvocableCommandHandler {
    @Override
    public String getCommand() {
        return "Вывести список клиентов";
    }

    @Override
    public List<PartialBotApiMethod<?>> handle(Update update) {
        long chatId = TelegramUpdateUtils.getChatId(update);
        List<GetActiveClientResponse> activeClientResponseList = client.getActiveClients(chatId);

        if (activeClientResponseList.isEmpty()) {
            return getPlainSendMessage(TelegramUpdateUtils.getChatId(update),
                    "У вас пока нет клиентов.");
        }

        String activeClientsMessage = getActiveClientsMessage(activeClientResponseList);
        return getPlainSendMessage(TelegramUpdateUtils.getChatId(update), activeClientsMessage);
    }

    private String getActiveClientsMessage(List<GetActiveClientResponse> activeClientResponseList) {
        StringBuilder activeClientsResponse = new StringBuilder();
        for (int i = 0; i < activeClientResponseList.size(); i++) {
            GetActiveClientResponse clientResponse = activeClientResponseList.get(i);
            String clientDescription = String.format(
                    "%d. %s%n     Тариф: %d руб/час%n     Описание: %s%n%n",
                    i + 1,
                    clientResponse.getName(),
                    clientResponse.getPricePerHour(),
                    clientResponse.getDescription()
            );

            activeClientsResponse.append(clientDescription);
        }

        return activeClientsResponse.toString();
    }
}
