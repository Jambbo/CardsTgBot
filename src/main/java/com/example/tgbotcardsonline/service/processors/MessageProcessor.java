package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.model.response.DeckResponse;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final TelegramBot telegramBot;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final OnlinePlayerService onlinePlayerService;
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final CardService cardService;
    private final DeckResponseRepository deckResponseRepository;

    public void handleGameOperation(String messageText, Player player) {
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        Game game = onlinePlayer.getGame();
        switch (messageText) {
            case "resign":
                gameService.surrend(onlinePlayer);
                break;
            case "myCards":
                onlinePlayerService.showMyCards(onlinePlayer);
                break;
            default:
                System.out.println("aboba aboba aboba...");
        }

        boolean isPlayersTurn = isPlayersMove(player, game);
        if(isPlayersTurn){
            switch (messageText){
                case "finish attack" -> finishAttack(player, game);
                case "take cards" -> takeCards();
                default -> telegramBot.sendMessageToPlayer(player,"Unknown command.");
            }
        }

    }

    private void finishAttack(Player player, Game game) {
        boolean possibleToFinishMove = isPossibleToFinishMove(player, game);
        if(possibleToFinishMove) {
            game.setBeaten(new ArrayList<>());
            switchTurnsAtFinishAttack(game);
            refillCards(game);
        }
    }

    private void refillCards(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        if(isCardNeeded(attacker)){
           if(isPossibleToDrawCards(attacker)){
               cardService.drawACard(game.getDeckId(),6-attacker.getCards().size());
            }else{
               int validatedCountToDrawCards = getValidatedCountToDrawCards(attacker);
               cardService.drawACard(game.getDeckId(),validatedCountToDrawCards);
           }
        }
        if(isCardNeeded(defender)){
            if(isPossibleToDrawCards(defender)){
                cardService.drawACard(game.getDeckId(),6-defender.getCards().size());
            }else{
                int validatedCountToDrawCards = getValidatedCountToDrawCards(attacker);
                cardService.drawACard(game.getDeckId(),validatedCountToDrawCards);
            }
        }
        cardService.drawACard(game.getDeckId(),6-defender.getCards().size());
    }

    private int getValidatedCountToDrawCards(OnlinePlayer player) {
        Game game = player.getGame();
        String deckId = game.getDeckId();
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
        return deckResponse.getRemaining();

    }

    private boolean isCardNeeded(OnlinePlayer player) {
        if(player.getCards().size()>=6){
            return false;
        }
        return true;
    }

    private boolean isPossibleToDrawCards(OnlinePlayer onlinePlayer) {
        Game game = onlinePlayer.getGame();
        String deckId = game.getDeckId();
        DeckResponse deckResponse = deckResponseRepository.findByDeckId(deckId);
        int remaining = deckResponse.getRemaining();
        int playerCardsAmount = onlinePlayer.getCards().size();
        int cardsNeeded = 6 - playerCardsAmount;
        if(cardsNeeded>remaining){
            return false;
        }else{
            return true;
        }
    }

    private void switchTurnsAtFinishAttack(Game game) {
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        game.setAttacker(defender);
        game.setDefender(attacker);
        gameRepository.save(game);
    }

    private boolean isPossibleToFinishMove(Player player, Game game){
        List<Card> beaten = game.getBeaten();
        if(beaten.isEmpty()){
            telegramBot.sendMessageToPlayer(player, "You are not able to finish your first move");
            return false;
        }
        if(isNull(game.getOffensiveCard())){
            return true;
        }else{
            telegramBot.sendMessageToPlayer(player,
                    " You are not able to finish attack. Defender haven't defended yet. Offensive card: "+
                            gameService.getPrettyMove(game.getOffensiveCard())
            );
            return false;
        }
    }

    private void takeCards() {

    }

    public void handleWithMove(String callbackData, Player player) {
        Game game = player.getPlayerInGame().getGame();
        String playerMove = CardsClient.containsCard(callbackData);
        if (isNull(playerMove)) {
            telegramBot.sendMessageToPlayer(player, callbackData + " is not a card!");
        }

        if (!isPlayersMove(player, game)) {
            telegramBot.sendMessageToPlayer(player, "It is not your turn.");
            return;
        }

        Card playersCard = checkIfPlayerHasThisCard(player.getPlayerInGame().getCards(), callbackData);
        if (playersCard == null) {
            telegramBot.sendMessageToPlayer(player, "You do not have this card.");
            return;
        }
        gameService.makeMove(player, playersCard);
    }


    private boolean isPlayersMove(Player player, Game game) {
        OnlinePlayer activePlayerInGame = game.getActivePlayer();
        OnlinePlayer onlinePlayer = player.getPlayerInGame();
        return activePlayerInGame.equals(onlinePlayer);
    }

    private void handleCommandsInGame(String message, OnlinePlayer player) {

    }


    public Card checkIfPlayerHasThisCard(List<Card> cards, String messageText) {
        for (Card card : cards) {
            if (card.getCode().equals(messageText)) {
                return card;
            }
        }
        return null;
    }

}
