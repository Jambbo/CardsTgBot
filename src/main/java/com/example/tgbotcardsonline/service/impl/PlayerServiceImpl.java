package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.model.PlayerStatistics;
import com.example.tgbotcardsonline.repository.PlayerRepository;
import com.example.tgbotcardsonline.repository.PlayerStatisticsRepository;
import com.example.tgbotcardsonline.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerServiceImpl implements PlayerService {
    private final PlayerRepository playerRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;

    @Override
    public Player getByChatIdOrElseCreateNew(Long chatId, Message message) {
        return playerRepository.findByChatId(chatId).orElseGet(() -> {

            PlayerStatistics playerStatistics = PlayerStatistics.builder()
                    .gamesPlayed(0L)
                    .wins(0L)
                    .build();

            playerStatisticsRepository.save(playerStatistics);

            Player newPlayer = Player.builder()
                    .chatId(message.getChatId())
                    .username(
                            message.getFrom().getUserName() != null
                                    ? message.getFrom().getUserName()
                                    : "Player"+message.getFrom().getId()
                    )
                    .inGame(false)
                    .playerStatistics(playerStatistics)
                    .build();
            return playerRepository.save(newPlayer);
        });
    }
}
