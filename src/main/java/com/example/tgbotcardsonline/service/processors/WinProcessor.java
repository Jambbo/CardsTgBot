package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.repository.DeckResponseRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WinProcessor {
    private final TelegramBot telegramBot;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final DeckResponseRepository deckResponseRepository;

    public void processWinningState(OnlinePlayer onlinePlayer, Game game) {
        Player player = onlinePlayer.getPlayer();
        OnlinePlayer attacker = game.getAttacker();
        Player opponent = getOpponent(game, attacker, player);
        processAppStatistics(game, player, opponent);
        processDeletingDataFromDb(game);
        notifyPlayersAboutFinishGame(game);
    }

    public static void processAppStatistics(Game game, Player player, Player opponent) {
        player.setWins(player.getWins()+1);
        opponent.setLosses(player.getLosses()+1);
        game.setWinner(player);
    }

    public void processDeletingDataFromDb(Game game) {
        game.setBeaten(null);
        onlinePlayerRepository.deleteAll(List.of(game.getAttacker(),game.getDefender()));
        deckResponseRepository.delete(deckResponseRepository.findByDeckId(game.getDeckId()));
    }
    private static Player getOpponent(Game game, OnlinePlayer attacker, Player player) {
        return attacker.equals(player) ? game.getDefender().getPlayer() : attacker.getPlayer();
    }

    public void notifyPlayersAboutFinishGame(Game game){
        telegramBot.sendMessageToBothPlayers(game,game.getWinner()+" won!");
    }

}
