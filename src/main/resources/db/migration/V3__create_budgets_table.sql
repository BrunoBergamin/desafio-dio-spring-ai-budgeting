-- Orcamento mensal por categoria. reference_month guarda sempre o dia 1 do mes.
CREATE TABLE budgets (
    id              VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(36)   NOT NULL,
    category        VARCHAR(20)   NOT NULL,
    reference_month DATE          NOT NULL,
    monthly_limit   DECIMAL(12,2) NOT NULL,
    created_at      TIMESTAMP(6)  NOT NULL,
    updated_at      TIMESTAMP(6),
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT uk_budgets_user_cat_month UNIQUE (user_id, category, reference_month),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_budgets_user_month ON budgets (user_id, reference_month);
