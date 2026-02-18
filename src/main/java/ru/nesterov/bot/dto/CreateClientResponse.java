package ru.nesterov.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientResponse {
    private long id;
    private String name;
    private int pricePerHour;
    private String description;
    private boolean active;
    private Date startDate;
    private String phone;
    private int responseCode;
    private String errorMessage;
}
