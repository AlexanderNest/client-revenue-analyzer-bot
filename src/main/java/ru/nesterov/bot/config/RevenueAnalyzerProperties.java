package ru.nesterov.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "revenue.analyzer.integration")
public class RevenueAnalyzerProperties {
    private String url;
    private String getIncomeAnalysisForMonthUrl;
    private String getClientStatisticUrl;
    private String getYearBusynessStatisticsUrl;
    private String generateRecommendationUrl;
    private String getUserByUsernameUrl;
    private String clientCreateUrl;
    private String createUserUrl;
    private String getScheduleUrl;
    private String getActiveClientsUrl;
    private String eventsBackupUrl;
    private String getUnpaidEventsUrl;
    private String clientUrl;
    private String clientUpdateUrl;
    private String getUsersIdByRoleAndSourceUrl;
    private String getClientPdfReportUrl;
}
