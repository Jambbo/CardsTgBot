package com.example.tgbotcardsonline.model.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deck_response")
public class DeckResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("deck_id")
    private String deck_id;
    @JsonProperty("shuffled")
    private boolean shuffled;
    @JsonProperty("remaining")
    private int remaining;

}
