package com.example.tgbotcardsonline.tg;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.service.PlayerService;
import com.example.tgbotcardsonline.service.SearchRequestService;
import com.example.tgbotcardsonline.service.processors.ButtonProcessor;
import com.example.tgbotcardsonline.service.processors.CardProcessor;
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

import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final PlayerService playerService;
    private final ApplicationContext applicationContext;
    private final CardProcessor cardProcessor;
    @Value("${bot.name}")
    private String name;

    public TelegramBot(
            @Value("${bot.token}") String botToken,
            CardProcessor cardProcessor, PlayerService playerService,
            ApplicationContext applicationContext
    ) {
        super(new DefaultBotOptions(), botToken);
        this.cardProcessor = cardProcessor;
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
                getMessageProcessor().handleGameOperation(messageText, player);
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
            case "/start" -> messageBuilder.text(
                    "Welcome! " + player.getUsername() + "\n Let's play!"
            );
            case "/aboba" -> messageBuilder.text("aboba");
            case "/startgame" -> getSearchRequestService().StartLookForRandomGame(player);
            case "/myprofile" -> getMessageProcessor().handleMyProfileQuery(player);
            default -> messageBuilder.text("You sent: " + messageText);
        }
    }

    @SneakyThrows
    @Async
    public void sendMessageToPlayer(Player player, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(player.getChatId())
                .text(message)
                .build();

        getButtonProcessor().createButton(player, sendMessage);

        execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageToBothPlayers(Game game, String message) {
        sendMessageToPlayer(game.getAttacker().getPlayer(), message);
        sendMessageToPlayer(game.getDefender().getPlayer(), message);
    }

    @SneakyThrows
    public void showAvailableCards(long chatId, List<Card> cards) {
        SendMessage message = cardProcessor.createMessage(chatId);
        InlineKeyboardMarkup markup = cardProcessor.createMarkup(cards);
        message.setReplyMarkup(markup);
        execute(message);
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

    @Override
    public String getBotUsername() {
        return name;
    }

}
