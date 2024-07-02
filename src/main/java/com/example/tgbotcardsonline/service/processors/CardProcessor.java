package com.example.tgbotcardsonline.service.processors;

import com.example.tgbotcardsonline.client.CardsClient;
import com.example.tgbotcardsonline.model.response.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CardProcessor {

    public SendMessage createMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Your cards:");
        return message;
    }

    public InlineKeyboardMarkup createMarkup(List<Card> cards) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Card card : cards) {
            List<InlineKeyboardButton> row = createButtonRow(card);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        return markup;
    }

    private List<InlineKeyboardButton> createButtonRow(Card card) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = createButton(card);
        row.add(button);
        return row;
    }

    private InlineKeyboardButton createButton(Card card) {
        String cardCode = card.getCode();
        String cardValue = getCardValue(cardCode);
        String cardSuit = cardCode.substring(cardCode.length() - 1);

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(cardValue + CardsClient.suitSymbols.get(cardSuit));
        button.setCallbackData(cardCode);
        return button;
    }

    private String getCardValue(String cardCode) {
        String cardValue = cardCode.substring(0, cardCode.length() - 1);
        return cardValue.equals("0") ? "10" : cardValue;
    }

}
