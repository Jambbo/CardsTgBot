package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game,Long> {



}
