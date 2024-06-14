package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.Attack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttackRepository extends JpaRepository<Attack,Long> {
    @Query("SELECT a.activePlayer FROM Attack a WHERE a.id = :attackId")
    Long findActivePlayerIdByAttackId(@Param("attackId") Long attackId);
}
