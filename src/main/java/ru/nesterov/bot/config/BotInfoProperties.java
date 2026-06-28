package ru.nesterov.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bot.info")
@Data
public class BotInfoProperties {
    private String creatorContact;
}
