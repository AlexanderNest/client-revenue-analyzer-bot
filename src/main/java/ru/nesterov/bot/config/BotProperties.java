package ru.nesterov.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("bot")
@Configuration
@Data
public class BotProperties {
    private String apiToken;
    private String username;
    private String secretToken;
    private Integer menuButtonsPerLine;
}
