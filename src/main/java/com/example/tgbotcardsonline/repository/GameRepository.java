package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game,Long> {
}
