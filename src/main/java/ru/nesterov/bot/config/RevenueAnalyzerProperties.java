package ru.nesterov.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "revenue.analyzer.integration")
public class RevenueAnalyzerProperties {
    private String url;
}
