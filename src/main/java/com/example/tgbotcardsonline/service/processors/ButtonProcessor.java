package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.model.Game;
import com.example.tgbotcardsonline.model.OnlinePlayer;
import com.example.tgbotcardsonline.model.Player;
import com.example.tgbotcardsonline.tg.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class ButtonProcessor {

    public void createButton(Player player, SendMessage sendMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        getSpecificButtonForPlayer(player, row);

        keyboardRows.add(row);


        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        keyboardMarkup.setKeyboard(keyboardRows);

        sendMessage.setReplyMarkup(keyboardMarkup);
    }

    private void getSpecificButtonForPlayer(Player player, KeyboardRow row) {
        OnlinePlayer playerInGame = player.getPlayerInGame();
        if(isNull(playerInGame)){
            log.info("game is not started yet.");
            return;
        }
        Game game = playerInGame.getGame();
        if(game.getAttacker().equals(playerInGame)){
            row.add("finish attack");
        }else if(game.getDefender().equals(playerInGame)){
            row.add("take cards");
        }else{
            row.add("aboba aboba aboba...");
        }
        row.add("resign");

    }



    public List<BotCommand> getBotCommands() {
        return List.of(
                new BotCommand("/startgame", "start new game with random player"),
                new BotCommand("/myprofile", "open profile"),
                new BotCommand("/help", "bot usage guide")
        );
    }

}
