package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final PlayerService playerService;
    private final ApplicationContext applicationContext;
    @Value("${bot.name}")
    private String name;

    public TelegramBot(@Value("${bot.token}") String botToken,
                       PlayerService playerService, ApplicationContext applicationContext) {
        super(new DefaultBotOptions(), botToken);
        this.playerService = playerService;
        this.applicationContext = applicationContext;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Player player = playerService.getByChatIdOrElseCreateNew(chatId, update.getMessage());
            getMessageProcessor().handleWithMove(update.getCallbackQuery().getData(), player);
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            Player player = playerService.getByChatIdOrElseCreateNew(chatId, update.getMessage());

            SendMessage.SendMessageBuilder messageBuilder = SendMessage.builder()
                    .chatId(chatId.toString());

            if (player.isInGame()) {
                getMessageProcessor().handleGameOperation(messageText, player); // process game
                return;
            }
            handleCommands(messageText, messageBuilder, player);
            SendMessage sendMessage = messageBuilder.build();
            if (!isNull(sendMessage)) {
                execute(sendMessage);
            }
        }
    }

    private void handleCommands(String messageText, SendMessage.SendMessageBuilder messageBuilder, Player player) {
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

    @SneakyThrows
    public void sendMessageToBothPlayers(Game game, String message) {
        sendMessageToPlayer(game.getAttacker().getPlayer(), message);
        sendMessageToPlayer(game.getDefender().getPlayer(), message);
    }

    public void showAvailableCards(long chatId, List<Card> cards) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Your cards:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        Map<String, String> suitSymbols = new HashMap<>();
        suitSymbols.put("H", "♥");
        suitSymbols.put("D", "♦");
        suitSymbols.put("S", "♠");
        suitSymbols.put("C", "♣");

        for (Card card : cards) {
            String cardCode = card.getCode();
            String cardValue = cardCode.substring(0, cardCode.length() - 1);
            if (cardValue.equals("0")) cardValue = "10";
            String cardSuit = cardCode.substring(cardCode.length() - 1);
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(cardValue + suitSymbols.get(cardSuit));
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
