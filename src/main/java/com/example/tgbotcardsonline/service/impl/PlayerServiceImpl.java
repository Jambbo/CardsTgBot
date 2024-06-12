package com.example.tgbotcardsonline.service.impl;

import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.repository.PlayerRepository;
import com.example.tgbotcardsonline.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {
    private PlayerRepository playerRepository;

    @Override
    public Player getByChatIdOrElseCreateNew(Long chatId, Message message) {
        return playerRepository.findByChatId(chatId).orElseGet(() -> {
            Player newPlayer = new Player();
            newPlayer.setChatId(message.getChatId());
            newPlayer.setUsername(message.getFrom().getUserName());
            newPlayer.setInGame(false);
            newPlayer.setCreatedAt(LocalDateTime.now());
            return playerRepository.save(newPlayer);
        });
    }
}
