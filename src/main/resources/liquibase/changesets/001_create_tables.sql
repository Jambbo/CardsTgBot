CREATE TABLE deck_response
(
    id        BIGSERIAL PRIMARY KEY,
    success   BOOLEAN NOT NULL,
    deck_id   VARCHAR NOT NULL,
    shuffled  BOOLEAN NOT NULL,
    remaining INT     NOT NULL
)