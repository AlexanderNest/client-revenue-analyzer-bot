package ru.nesterov.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAnalyzerRequest {
    private String monthName;
}
