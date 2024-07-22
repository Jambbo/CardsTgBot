package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.PlayerService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.service.processors.ButtonProcessor;
import com.example.tgbotcardsonline.service.processors.CardProcessor;
import com.example.tgbotcardsonline.service.processors.MessageProcessor;
import com.example.tgbotcardsonline.service.validator.MoveValidator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final PlayerService playerService;
    private final ApplicationContext applicationContext;
    private final CardProcessor cardProcessor;
    private final OnlinePlayerRepository onlinePlayerRepository;
    @Value("${bot.name}")
    private String name;

    public TelegramBot(
            @Value("${bot.token}") String botToken,
            CardProcessor cardProcessor,
            PlayerService playerService,
            ApplicationContext applicationContext,
            OnlinePlayerRepository onlinePlayerRepository
    ) {
        super(new DefaultBotOptions(), botToken);
        this.cardProcessor = cardProcessor;
        this.playerService = playerService;
        this.applicationContext = applicationContext;
        this.onlinePlayerRepository = onlinePlayerRepository;
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
            handleCommands(messageText, player);
        }
    }

    @SneakyThrows
    private void handleCommands(String messageText, Player player) {
        if (player.isInGame()) {
            getMessageProcessor().handleGameOperation(messageText, player);
            return;
        }
        switch (messageText) {
            case "/start" -> sendMessageToPlayer(player, "Hi " + player.getUsername() + " let's play some durak!");
            case "\uD83D\uDCF1 My profile", "/myprofile" -> getMessageProcessor().handleMyProfileQuery(player);
            case "/startgame", "\uD83C\uDFB2 Start random game" ->
                    getSearchRequestService().StartLookForRandomGame(player);
            case "/help" -> getMessageProcessor().handleHelpQuery(player);
            default -> sendMessageToPlayer(player, messageText + "??");
        }
    }

    @Async
    public CompletableFuture<Integer> sendMessageToPlayer(Player player, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(player.getChatId())
                .text(message)
                .build();

        try {
            Message sentMessage = execute(sendMessage);
            return CompletableFuture.completedFuture(sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @SneakyThrows
    @Async
    public CompletableFuture<Integer> sendMessageToPlayer(Player player, String message, String parseMode) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(player.getChatId())
                .text(message)
                .parseMode(parseMode)
                .build();

        Message sentMessage = execute(sendMessage);
        return CompletableFuture.completedFuture(sentMessage.getMessageId());
    }

    @SneakyThrows
    @Async
    public void updateBeatenCardsMessages(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        updateBeatenCardsMessage(attacker);
        updateBeatenCardsMessage(defender);
        if (attacker.getMessageId() != null) {
            deleteMessage(attacker.getPlayer().getChatId(), attacker.getMessageId());
            deleteMessage(defender.getPlayer().getChatId(), defender.getMessageId());

            deleteMessage(attacker.getPlayer().getChatId(), defender.getMessageIdSentToOpponent());
            deleteMessage(defender.getPlayer().getChatId(), attacker.getMessageIdSentToOpponent());
        }
    }

    private void updateBeatenCardsMessage(OnlinePlayer onlinePlayer) throws TelegramApiException, ExecutionException, InterruptedException {
        Integer attackerMessageId = onlinePlayer.getBeatenCardsMessageId();
        if (attackerMessageId != null) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(onlinePlayer.getPlayer().getChatId());
            editMessage.setMessageId(attackerMessageId);
            editMessage.setParseMode(ParseMode.MARKDOWNV2);
            editMessage.setText(getBeatenCardsString(onlinePlayer.getGame()));

            execute(editMessage);
        } else {
            Integer beatenCardsMessageId = sendMessageToPlayer(
                    onlinePlayer.getPlayer(),
                    getBeatenCardsString(
                            onlinePlayer.getGame()
                    ),
                    ParseMode.MARKDOWNV2
            ).get();
            onlinePlayer.setBeatenCardsMessageId(beatenCardsMessageId);
            onlinePlayerRepository.save(onlinePlayer);
        }
    }

    @Async
    @SneakyThrows
    public void updateNowMoveMessages(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();

        boolean nextMoveIsAttack = game.getActivePlayer().equals(attacker);

        try {
            if (nextMoveIsAttack) {
                updateNowMoveMessage(attacker.getPlayer(), "Now is Your move");
                updateNowMoveMessage(defender.getPlayer(), "Now is " + attacker.getPlayer().getUsername() + "'s move");
            } else {
                updateNowMoveMessage(attacker.getPlayer(), "Now is " + defender.getPlayer().getUsername() + "'s move");
                updateNowMoveMessage(defender.getPlayer(), "Now is Your move");
            }
        } catch (TelegramApiException e) {
            log.error("Failed to update now move message", e);
        }
    }

    private void updateNowMoveMessage(Player player, String text) throws TelegramApiException, ExecutionException, InterruptedException {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        Integer messageId = onlinePlayer.getMessageIdNowMove();
        if (messageId != null) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(player.getChatId());
            editMessage.setMessageId(messageId);
            editMessage.setText(text);

            execute(editMessage);
        } else {
            Integer beatenCardsMessageId = sendMessageToPlayer(player, text).get();
            onlinePlayer.setMessageIdNowMove(beatenCardsMessageId);
            onlinePlayerRepository.save(onlinePlayer);
        }
    }

    @Async
    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to delete message", e);
        }
    }


    @SneakyThrows
    public void sendMessageToBothPlayers(Game game, String message) {
        sendMessageToPlayer(game.getAttacker().getPlayer(), message);
        sendMessageToPlayer(game.getDefender().getPlayer(), message);
    }

    @SneakyThrows
    public void showAvailableCards(OnlinePlayer onlinePlayer, List<Card> cards) {
        Long chatId = onlinePlayer.getPlayer().getChatId();

        SendMessage message = cardProcessor.createMessage(chatId);
        InlineKeyboardMarkup markup = cardProcessor.createMarkup(cards);
        message.setReplyMarkup(markup);

        Integer messageId = execute(message).getMessageId();

        onlinePlayer.setCardsMessageId(messageId);

        onlinePlayerRepository.save(onlinePlayer);
    }

    @SneakyThrows
    public void updateAvailableCards(OnlinePlayer onlinePlayer, List<Card> newCards) {
        Long chatId = onlinePlayer.getPlayer().getChatId();
        Integer messageId = onlinePlayer.getCardsMessageId();

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);
        editMessage.setText(cardProcessor.createMessage(chatId).getText());
        editMessage.setReplyMarkup(cardProcessor.createMarkup(newCards));
        execute(editMessage);
    }

    @SneakyThrows
    public void generateMenuButtons() {
        List<BotCommand> listOfCommands = getButtonProcessor().getBotCommands();
        this.execute(
                new SetMyCommands(
                        listOfCommands,
                        new BotCommandScopeDefault(),
                        null
                )
        );
    }

    private SearchRequestService getSearchRequestService() {
        return applicationContext.getBean(SearchRequestService.class);
    }

    private MessageProcessor getMessageProcessor() {
        return applicationContext.getBean(MessageProcessor.class);
    }

    private ButtonProcessor getButtonProcessor() {
        return applicationContext.getBean(ButtonProcessor.class);
    }

    private MoveValidator getMoveValidator() {
        return applicationContext.getBean(MoveValidator.class);
    }

    @Override
    public String getBotUsername() {
        return name;
    }


    public String getBeatenCardsString(Game game) {
        if (game.getBeaten().isEmpty()) {
            return "No cards have been beaten yet.";
        }

        List<Card> beatenCards = game.getBeaten();
        StringBuilder sb = new StringBuilder("Beaten cards: ");

        for (int i = 0; i < beatenCards.size(); i++) {
            Card card = beatenCards.get(i);
            String prettyMove = getMoveValidator().getPrettyMove(card);
            if (i % 2 == 0) {
                sb.append("||").append(prettyMove).append("||");
            } else {
                sb.append(prettyMove);
            }

            if (i < beatenCards.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

//    public String getPrettyMove(Card move) {
//        Map<String, String> suitSymbols = TelegramBot.suitSymbols;
//
//        String cardCode = move.getCode();
//        String cardValue = cardCode.startsWith("0") ? "10" : cardCode.substring(0, cardCode.length() - 1);
//        String cardSuit = cardCode.substring(cardCode.length() - 1);
//
//        return cardValue + suitSymbols.get(cardSuit);
//    }
}
