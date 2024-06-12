package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.OnlinePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnlinePlayerRepository extends JpaRepository<OnlinePlayer,Long> {
}
