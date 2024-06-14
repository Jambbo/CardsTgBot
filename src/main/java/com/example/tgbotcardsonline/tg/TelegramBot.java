package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.PlayerService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.service.processors.MessageProcessor;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

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

        if (player.isInGame()){
            getMessageProcessor().handleGameOperation(messageText, player); // process game
            return;
        }

        switch (messageText) { // process commands
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

                break;
        }

        SendMessage sendMessage = messageBuilder.build();
        if (!isNull(sendMessage)) {
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
    public void showAvailableCards(long chatId, List<Card> cards) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Choose a card:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Card card : cards) {
            String cardCode = card.getCode();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(cardCode); // TODO handle to show suits of card by symbols
            button.setCallbackData(cardCode);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SearchRequestService getSearchRequestService() {
        return applicationContext.getBean(SearchRequestService.class);
    }

    private MessageProcessor getMessageProcessor() {
        return applicationContext.getBean(MessageProcessor.class);
    }

    @Override
    public String getBotUsername() {
        return name;
    }


}
