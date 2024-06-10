package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.PlayerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final CardService cardService;
    private final PlayerService playerService;

    @Value("${bot.name}")
    private String name;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       CardService cardService,
                       PlayerService playerService) {
        super(new DefaultBotOptions(), botToken);
        this.cardService = cardService;
        this.playerService = playerService;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        SendMessage.SendMessageBuilder messageBuilder = SendMessage.builder()
                .chatId(chatId.toString());

        switch (messageText) {
            case "/start":
                Player player = playerService.getByChatIdOrElseCreateNew(chatId,update.getMessage());
                messageBuilder.text("Welcome!"+player.getUsername()+ "\n Let's play!.");

                break;
            case "/aboba":
                messageBuilder.text("aboba");
                break;
            default:
                messageBuilder.text("You sent: " + messageText);

                cardService.brandNewDeck();
                break;
        }

        SendMessage sendMessage = messageBuilder.build();
        execute(sendMessage);
    }

    @Override
    public String getBotUsername() {
        return name;
    }


}
