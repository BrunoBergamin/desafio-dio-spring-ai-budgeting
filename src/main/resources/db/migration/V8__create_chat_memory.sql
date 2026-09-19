-- Memoria de conversa da Lumi no banco, em vez de RAM: ela lembra do contexto mesmo depois de reiniciar.
--
-- A tabela e a mesma que o Spring AI criaria sozinho, com duas diferencas de proposito:
--   1. conversation_id VARCHAR(80) e nao 36. A chave aqui e "<uuid do usuario>:<nome da conversa>",
--      que passa de 36 caracteres; com o tamanho padrao o insert falharia.
--   2. type como VARCHAR(10) simples: o ENUM do script original so existe no MySQL, e o CHECK que
--      seria o equivalente portavel o H2 recusa nesta coluna. Os valores vem do enum do Spring AI
--      (USER, ASSISTANT, SYSTEM, TOOL), nao de entrada de usuario, entao nao ha o que validar aqui.
-- Por isso a criacao automatica fica desligada (initialize-schema=never) e quem manda e o Flyway.
CREATE TABLE SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(80)  NOT NULL,
    content         TEXT         NOT NULL,
    type            VARCHAR(10)  NOT NULL,
    timestamp       TIMESTAMP    NOT NULL,
    sequence_id     BIGINT       NOT NULL
);

CREATE INDEX spring_ai_chat_memory_conversation_timestamp_idx
    ON SPRING_AI_CHAT_MEMORY (conversation_id, timestamp);

CREATE INDEX spring_ai_chat_memory_conversation_sequence_idx
    ON SPRING_AI_CHAT_MEMORY (conversation_id, sequence_id);
