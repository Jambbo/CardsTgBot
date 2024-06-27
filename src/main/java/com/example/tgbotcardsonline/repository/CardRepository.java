package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.response.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {


    Optional<Card> findCardByCodeAndGameId(String callBackData, Long id);
}
