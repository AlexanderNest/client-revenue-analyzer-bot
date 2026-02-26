package ru.nesterov.bot.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClientAllScheduleResponse {
    private String clientName;
    private List<GetClientScheduleResponse> events;
}
