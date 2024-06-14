package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnlinePlayerRepository extends JpaRepository<OnlinePlayer, Long> {
    Optional<OnlinePlayer> findByPlayer(Player player);
}
