package ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient;

import lombok.Data;

@Data
public class UpdateClientRequest {
    private String clientName;
    private String newName;
    private Integer pricePerHour;
    private String description;
    private String phone;
}

