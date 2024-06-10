package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player,Long> {
    Optional<Player> findByChatId(Long chatId);
}
