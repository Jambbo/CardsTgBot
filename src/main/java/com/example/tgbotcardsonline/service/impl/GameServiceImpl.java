package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.Attack;
import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.enums.Suit;
import com.example.tgbotcardsonline.repository.AttackRepository;
import com.example.tgbotcardsonline.repository.GameRepository;
import com.example.tgbotcardsonline.repository.OnlinePlayerRepository;
import com.example.tgbotcardsonline.repository.PlayerRepository;
import com.example.tgbotcardsonline.service.AttackService;
import com.example.tgbotcardsonline.service.CardService;
import com.example.tgbotcardsonline.service.GameService;
import com.example.tgbotcardsonline.service.OnlinePlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final CardsClient cardsClient;
    private final CardService cardService;
    private final OnlinePlayerService onlinePlayerService;
    private final AttackService attackService;
    private final OnlinePlayerRepository onlinePlayerRepository;
    private final PlayerRepository playerRepository;
    private final AttackRepository attackRepository;

    @Override
    public Game createGame(Player firstPlayer, Player secondPlayer) {
        // создать Online players

        String deck = cardService.brandNewDeck();
        OnlinePlayer player = onlinePlayerService.createOnlinePlayer(firstPlayer, deck);
        OnlinePlayer player2 = onlinePlayerService.createOnlinePlayer(secondPlayer, deck);

        Game game = getGame(deck, player, player2);
        Attack attack = attackService.createAttack(game);
        game.setCurrentAttack(attack);
        processPlayers(firstPlayer, secondPlayer, player, player2);
        Game savedGame = gameRepository.save(game);
        processOnlinePlayers(player, game, player2);
        attackRepository.save(attack);
        return savedGame;
    }

    private Game getGame(String deck, OnlinePlayer player, OnlinePlayer player2) {
        return Game.builder()
                .deckId(deck)
                .trump(getRandomTrump())
                .players(List.of(player, player2))
                .build();
    }

    private void processOnlinePlayers(OnlinePlayer player, Game game, OnlinePlayer player2) {
        player.setGame(game);
        player2.setGame(game);
        onlinePlayerRepository.save(player);
        onlinePlayerRepository.save(player2);
    }

    private void processPlayers(Player firstPlayer, Player secondPlayer, OnlinePlayer player, OnlinePlayer player2) {
        firstPlayer.setInGame(true);
        secondPlayer.setInGame(true);
        firstPlayer.setPlayerInGame(player);
        secondPlayer.setPlayerInGame(player2);
        playerRepository.saveAll(List.of(firstPlayer, secondPlayer));
    }

    public Suit getRandomTrump() {
        Suit[] suits = Suit.values();
        Random random = new Random();
        return suits[random.nextInt(suits.length)];
    }


}
