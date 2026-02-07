package ru.nesterov.bot.dto;

import lombok.Data;

import java.util.Date;

@Data
public class GetClientStatisticResponse {
    private String name;
    private long id;
    private String description;
    private Date startDate;
    private long serviceDuration;
    private String phone;
    private double successfulMeetingsHours;
    private double cancelledMeetingsHours;
    private double incomePerHour;
    private int successfulEventsCount;
    private int plannedCancelledEventsCount;
    private int notPlannedCancelledEventsCount;
    private double totalIncome;
}
