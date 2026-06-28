package ru.nesterov.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.nesterov.bot.RevenueAnalyzerBot;

@Configuration
@Slf4j
public class BotConfig {
    @Bean
    public TelegramBotsApi telegramBotsApi(RevenueAnalyzerBot revenueAnalyzerBot) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(revenueAnalyzerBot);
        return telegramBotsApi;
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder, RevenueAnalyzerProperties properties) {
        return builder
                .baseUrl(properties.getUrl())
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
