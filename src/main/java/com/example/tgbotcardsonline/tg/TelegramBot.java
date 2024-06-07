package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.service.CardService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final CardService cardService;

    @Value("${bot.name}")
    private String name;

    public TelegramBot(@Value("${bot.token}")String botToken, CardService cardService){
        super(new DefaultBotOptions(), botToken);
        this.cardService = cardService;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        SendMessage sendMessage = SendMessage.builder()
                .text(update.getMessage().getText())
                .chatId(update.getMessage().getChatId())
                .build();
        cardService.brandNewDeck();
        execute(sendMessage);
    }

    @Override
    public String getBotUsername() {
        return name;
    }


}
