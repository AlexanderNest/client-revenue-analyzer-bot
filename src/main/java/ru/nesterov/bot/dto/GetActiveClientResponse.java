package ru.nesterov.bot.dto;

import lombok.Data;

@Data
public class GetActiveClientResponse {
    private long id;
    private String name;
    private int pricePerHour;
    private String description;
    private boolean active;
}
