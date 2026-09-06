package ru.nesterov.bot.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.nesterov.bot.RevenueAnalyzerBot;
import ru.nesterov.bot.kafka.dto.KafkaMessage;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(value = "app.kafka.enabled")
public class MessageListener {

    private final RevenueAnalyzerBot revenueAnalyzerBot;

    @KafkaListener(topics = "${app.kafka.default-topic}", groupId = "revenue-analyzer-group")
    public void consume(KafkaMessage message) {
        log.debug("Received Message: {} {}", message.userName(), message.message());

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.userName());
        sendMessage.setText(message.message());

        revenueAnalyzerBot.sendMessage(List.of(sendMessage));
    }
}
