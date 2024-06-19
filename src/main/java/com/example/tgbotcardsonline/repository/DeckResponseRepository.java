package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.response.DeckResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeckResponseRepository extends JpaRepository<DeckResponse,Long> {
    @Query("select dr from DeckResponse dr where dr.deck_id = :deckId")
    DeckResponse findByDeckId(@Param("deckId")String deckId);

}
