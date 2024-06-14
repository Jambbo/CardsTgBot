package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.PlayerService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final CardService cardService;
    private final PlayerService playerService;
    private final ApplicationContext applicationContext;
    @Value("${bot.name}")
    private String name;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       CardService cardService,
                       PlayerService playerService, ApplicationContext applicationContext) {
        super(new DefaultBotOptions(), botToken);
        this.cardService = cardService;
        this.playerService = playerService;
        this.applicationContext = applicationContext;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        SendMessage.SendMessageBuilder messageBuilder = SendMessage.builder()
                .chatId(chatId.toString());

        Player player = playerService.getByChatIdOrElseCreateNew(chatId, update.getMessage());
        switch (messageText) {
            case "/start":
                messageBuilder.text("Welcome! " + player.getUsername() + "\n Let's play!");

                break;
            case "/aboba":
                messageBuilder.text("aboba");
                break;
            case "/startGame":
                getSearchRequestService().StartLookForRandomGame(player);
                break;
            default:
                messageBuilder.text("You sent: " + messageText);

//                cardService.brandNewDeck();
                break;
        }

        SendMessage sendMessage = messageBuilder.build();
        if(!isNull(sendMessage)){
            execute(sendMessage);
        }

    }

    @SneakyThrows
    @Async
    public void sendMessageToPlayer(Player player, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(player.getChatId())
                .text(message)
                .build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            // Handle exception
            e.printStackTrace();
        }
    }

    private SearchRequestService getSearchRequestService() {
        return applicationContext.getBean(SearchRequestService.class);
    }

    @Override
    public String getBotUsername() {
        return name;
    }


}
