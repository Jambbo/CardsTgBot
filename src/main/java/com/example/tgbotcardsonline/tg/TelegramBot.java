package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.service.PlayerService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.service.processors.MessageProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final PlayerService playerService;
    private final ApplicationContext applicationContext;
    @Value("${bot.name}")
    private String name;

    public TelegramBot(
                       @Value("${bot.token}") String botToken,
                       PlayerService playerService,
                       ApplicationContext applicationContext
    ) {
        super(new DefaultBotOptions(), botToken);
        this.playerService = playerService;
        this.applicationContext = applicationContext;
        generateMenuButtons();
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
        switch (messageText) {
            case "/start":
                messageBuilder.text("Welcome! " + player.getUsername() + "\n Let's play!");

                break;
            case "/aboba":
                messageBuilder.text("aboba");
                break;
            case "/startgame":
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

        createButton(player, sendMessage);

        execute(sendMessage);

    }

    private static void createButton(Player player, SendMessage sendMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        getSpecificButtonForPlayer(player, row);

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        sendMessage.setReplyMarkup(keyboardMarkup);
    }

    private static void getSpecificButtonForPlayer(Player player, KeyboardRow row) {
        OnlinePlayer playerInGame = player.getPlayerInGame();
        if(isNull(playerInGame)){
            log.error("game is not started yet.");
            return;
        }
        Game game = playerInGame.getGame();
        if(game.getAttacker().equals(playerInGame)){
            row.add("finish attack");
        }else if(game.getDefender().equals(playerInGame)){
            row.add("take cards");
        }else{
            row.add("aboba aboba aboba...");
        }
    }

    @SneakyThrows
    public void sendMessageToBothPlayers(Game game, String message) {
        sendMessageToPlayer(game.getAttacker().getPlayer(), message);
        sendMessageToPlayer(game.getDefender().getPlayer(), message);
    }

    @SneakyThrows
    public void showAvailableCards(long chatId, List<Card> cards) {
        SendMessage message = createMessage(chatId);
        InlineKeyboardMarkup markup = createMarkup(cards);
        message.setReplyMarkup(markup);
        execute(message);
    }

    private SendMessage createMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Your cards:");
        return message;
    }

    private InlineKeyboardMarkup createMarkup(List<Card> cards) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Card card : cards) {
            List<InlineKeyboardButton> row = createButtonRow(card);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        return markup;
    }

    private List<InlineKeyboardButton> createButtonRow(Card card) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = createButton(card);
        row.add(button);
        return row;
    }

    private InlineKeyboardButton createButton(Card card) {
        String cardCode = card.getCode();
        String cardValue = getCardValue(cardCode);
        String cardSuit = cardCode.substring(cardCode.length() - 1);

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(cardValue + CardsClient.suitSymbols.get(cardSuit));
        button.setCallbackData(cardCode);
        return button;
    }

    private String getCardValue(String cardCode) {
        String cardValue = cardCode.substring(0, cardCode.length() - 1);
        return cardValue.equals("0") ? "10" : cardValue;
    }

    @SneakyThrows
    private void generateMenuButtons() {
        List<BotCommand> listOfCommands = List.of(
                new BotCommand("/startgame", "start new game with random player"),
                new BotCommand("/myprofile", "open profile"),
                new BotCommand("/help", "bot usage guide")
        );
        this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
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
