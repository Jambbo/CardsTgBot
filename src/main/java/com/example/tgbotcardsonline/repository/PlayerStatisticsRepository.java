package com.example.tgbotcardsonline.repository;

import com.example.tgbotcardsonline.model.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
}
