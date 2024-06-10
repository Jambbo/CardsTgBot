
CREATE TABLE "DurakSchema".deck_response
(
    id BIGSERIAL PRIMARY KEY,
    success   BOOLEAN NOT NULL,
    deck_id   VARCHAR(255) NOT NULL,
    shuffled  BOOLEAN NOT NULL,
    remaining INT     NOT NULL
);
CREATE TABLE "DurakSchema".Player
(
    id BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT       NOT NULL,
    username   VARCHAR(255) DEFAULT 'User',
    in_game    BOOLEAN      NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);