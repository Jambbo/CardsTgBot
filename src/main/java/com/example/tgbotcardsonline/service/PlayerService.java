package com.example.tgbotcardsonline.service;

import com.example.tgbotcardsonline.model.Player;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface PlayerService {



    Player getByChatIdOrElseCreateNew(Long chatId, Message message);
}
