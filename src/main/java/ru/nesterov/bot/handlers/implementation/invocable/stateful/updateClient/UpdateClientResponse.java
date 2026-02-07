package ru.nesterov.bot.handlers.implementation.invocable.stateful.updateClient;

import lombok.Data;

@Data
public class UpdateClientResponse {
    private String name;
    private Integer pricePerHour;
    private String description;
    private String phone;
}
