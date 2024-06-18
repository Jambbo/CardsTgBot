CREATE TABLE deck_response
(
    id BIGSERIAL PRIMARY KEY,
    success   BOOLEAN NOT NULL,
    deck_id   VARCHAR NOT NULL,
    shuffled  BOOLEAN NOT NULL,
    remaining INT     NOT NULL
);
CREATE TABLE Player
(
    id BIGSERIAL PRIMARY KEY,
    chat_id           BIGINT    NOT NULL,
    username          VARCHAR(255)       DEFAULT 'User',
    player_in_game_id INT,
    in_game           BOOLEAN   NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Game
(
    id BIGSERIAL PRIMARY KEY,
    attack_id        INT,
    deck_id          VARCHAR,
    trump            VARCHAR,
    active_player_id INT
);

CREATE TABLE Attack
(
    id BIGSERIAL PRIMARY KEY,
    game_id          INT,
    attacker_id      INT,
    defender_id      INT,
    active_player_id INT
);

CREATE TABLE online_player
(
    id BIGSERIAL PRIMARY KEY,
    player_id INT
);
CREATE TABLE search_request
(
    id BIGSERIAL PRIMARY KEY,
    searcher_id INT,
    created_at  timestamp,
    game_type   VARCHAR
);

CREATE TABLE card
(
    id BIGSERIAL PRIMARY KEY,
    code  VARCHAR,
    image VARCHAR,
    suit  VARCHAR,
    value VARCHAR
);

CREATE TABLE op_cards
(
    online_player_id BIGINT,
    card_id          BIGINT,
    PRIMARY KEY (online_player_id, card_id),
    CONSTRAINT fk_op_cards_online_player_id FOREIGN KEY (online_player_id) REFERENCES online_player (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT fk_op_cards_card_id FOREIGN KEY (card_id) REFERENCES card (id) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE game_players
(
    game_id   BIGINT,
    player_id BIGINT,
    PRIMARY KEY (game_id, player_id),
    CONSTRAINT fk_op_cards_online_player_id FOREIGN KEY (player_id) REFERENCES online_player (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT fk_op_cards_card_id FOREIGN KEY (game_id) REFERENCES card (id) ON DELETE CASCADE ON UPDATE NO ACTION
);
CREATE TABLE attack_beaten
(
    attack_id BIGINT,
    beaten_card_id   BIGINT,
    PRIMARY KEY (attack_id, beaten_card_id),
    CONSTRAINT fk_op_cards_online_player_id FOREIGN KEY (attack_id) REFERENCES Attack (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT fk_op_cards_card_id FOREIGN KEY (beaten_card_id) REFERENCES card (id) ON DELETE CASCADE ON UPDATE NO ACTION
);
CREATE TABLE attack_offensive_cards
(
    attack_id BIGINT,
    offensive_card_id   BIGINT,
    PRIMARY KEY (attack_id, offensive_card_id),
    CONSTRAINT fk_op_cards_online_player_id FOREIGN KEY (attack_id) REFERENCES Attack (id) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT fk_op_cards_card_id FOREIGN KEY (offensive_card_id) REFERENCES card (id) ON DELETE CASCADE ON UPDATE NO ACTION

)