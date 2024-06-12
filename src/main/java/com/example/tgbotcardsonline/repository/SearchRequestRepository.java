package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.SearchRequest;
import com.example.tgbotcardsonline.service.SearchRequestService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SearchRequestRepository extends JpaRepository<SearchRequest,Long> {
    //  Что бы находило игру с тем кто дольше всего ждёт
    @Query("SELECT sr FROM SearchRequest sr ORDER BY sr.createdAt ASC")
    Optional<SearchRequest> findOldestRequest();
}
