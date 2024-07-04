package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.PlayerStatistics;
import com.example.tgbotcardsonline.model.response.Card;
import com.example.tgbotcardsonline.repository.*;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WinProcessor {
    private final CardRepository cardRepository;
    private final GameRepository gameRepository;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final PlayerRepository playerRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;

    public void processWinningState(OnlinePlayer player){
        Game game = player.getGame();
        OnlinePlayer attacker = game.getAttacker();
        OnlinePlayer defender = game.getDefender();
        Player attackerPlayer = attacker.getPlayer();
        Player defenderPlayer = defender.getPlayer();

        updateWinner(player, game);
        resetPlayerStates(attackerPlayer, defenderPlayer);
        detachCardsFromPlayers(attacker, defender);

        game.setAttacker(null);
        game.setDefender(null);
        game.setActivePlayer(null);
        game.setCards(null);
        playerRepository.saveAll(List.of(attackerPlayer,defenderPlayer));
        gameRepository.save(game);
//        cardRepository.deleteAll(null);
        List<Card> cards = cardRepository.findAllByGameId(game.getId());
        cardRepository.deleteAll(cards);
//        cardRepository.deleteAll(game.getCards());
//        cardRepository.deleteAllCardsByGameId(game.getId());
        onlinePlayerRepository.deleteAll(List.of(attacker,defender));
    }

    private void updateWinner(OnlinePlayer player, Game game) {
        Player attackerPlayer = game.getAttacker().getPlayer();
        Player defenderPlayer = game.getDefender().getPlayer();

        if (game.getAttacker().equals(player)) {
            game.setWinner(defenderPlayer);
            updateStatistics(defenderPlayer, attackerPlayer);
        } else {
            game.setWinner(attackerPlayer);
            updateStatistics(attackerPlayer, defenderPlayer);
        }
    }

    private void updateStatistics(Player winner, Player loser) {
        PlayerStatistics winnerStatistics = winner.getPlayerStatistics();
        winnerStatistics.setWins(winnerStatistics.getWins()+1);
        winnerStatistics.setGamesPlayed(winnerStatistics.getGamesPlayed()+1);

        PlayerStatistics loserStatistics = loser.getPlayerStatistics();
        loserStatistics.setGamesPlayed(loserStatistics.getGamesPlayed()+1);

        playerStatisticsRepository.save(winnerStatistics);
        playerStatisticsRepository.save(loserStatistics);
        playerRepository.save(winner);
        playerRepository.save(loser);
    }

    private void resetPlayerStates(Player attackerPlayer, Player defenderPlayer) {
        attackerPlayer.setInGame(false);
        attackerPlayer.setPlayerInGame(null);
        defenderPlayer.setInGame(false);
        defenderPlayer.setPlayerInGame(null);
    }

    private void detachCardsFromPlayers(OnlinePlayer attacker, OnlinePlayer defender) {
        List<Card> attackerCards = attacker.getCards();
        List<Card> defenderCards = defender.getCards();
        attackerCards.forEach(c -> c.setOnlinePlayer(null));
        defenderCards.forEach(c -> c.setOnlinePlayer(null));
        cardRepository.saveAll(attackerCards);
        cardRepository.saveAll(defenderCards);
    }

    public String getWinnerNameDuringResign(OnlinePlayer player){
        Player resignedPlayer = player.getPlayer();
        Game game = player.getGame();
        Player attackerPlayer = game.getAttacker().getPlayer();
        Player defenderPlayer = game.getDefender().getPlayer();
        return attackerPlayer.equals(resignedPlayer)? defenderPlayer.getUsername():attackerPlayer.getUsername();
    }

}
