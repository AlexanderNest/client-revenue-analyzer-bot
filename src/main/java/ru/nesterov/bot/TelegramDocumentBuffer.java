package ru.nesterov.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;

@Component
public class TelegramDocumentBuffer {
    private static final ThreadLocal<SendDocument> PENDING = new ThreadLocal<>();

    public void set(SendDocument document) {
        PENDING.set(document);
    }

    public SendDocument poll(){
        SendDocument document = PENDING.get();
        PENDING.remove();
        return document;
    }
}
