package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.response.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {


    Optional<Card> findCardByCodeAndGameId(String callBackData, Long id);
    @Modifying
    @Query("DELETE FROM Card c WHERE c.gameId = :gameId")
    void deleteAllCardsByGameId(@Param("gameId") Long gameId);

    List<Card> findAllByGameId(Long gameId);
}
