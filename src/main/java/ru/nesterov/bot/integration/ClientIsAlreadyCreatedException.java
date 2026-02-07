package ru.nesterov.bot.integration;

public class ClientIsAlreadyCreatedException extends Exception {
    public ClientIsAlreadyCreatedException(String clientName) {
        super("Клиент с именем [" + clientName + "] уже создан");
    }
}
