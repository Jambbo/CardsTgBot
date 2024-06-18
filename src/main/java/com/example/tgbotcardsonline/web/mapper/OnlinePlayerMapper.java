package com.example.tgbotcardsonline.web.mapper;

import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import org.mapstruct.Mapper;

import java.util.ArrayList;

@Mapper(componentModel = "spring")
public interface OnlinePlayerMapper {

    default OnlinePlayer toOnlinePlayer(Player player){
        return OnlinePlayer.builder()
                .player(player)
                .cards(new ArrayList<>())
                .game(null)
                .build();
    }
}
