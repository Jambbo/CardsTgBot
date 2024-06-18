package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.response.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    Card findByCode(String code);

}
