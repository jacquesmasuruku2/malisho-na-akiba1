-- ======================================================================================
-- MALISHO NA AKIBA (MALAKI) - SCRIPT DE MIGRATION TOTALE POSTGRESQL (NEON)
-- ======================================================================================
-- Base de données : malisho-na-akiba
-- Moteur : PostgreSQL 16+ sur Neon Serverless Cloud
-- Instructions : Ouvrez https://console.neon.tech/ > SQL Editor > Collez et exécutez.
-- ======================================================================================

BEGIN;

-- ======================================================================================
-- 1. TABLE DES UTILISATEURS (Direction, Agents de terrain, Caissiers, Clients portail)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS users (
    id                      VARCHAR(64) PRIMARY KEY,
    full_name               VARCHAR(150) NOT NULL,
    phone                   VARCHAR(30) NOT NULL,
    email                   VARCHAR(150) DEFAULT '',
    role                    VARCHAR(30) NOT NULL CHECK (role IN ('GESTIONNAIRE', 'AGENT', 'CAISSIER', 'CLIENT')),
    market_zone             VARCHAR(100) DEFAULT 'Marché de Kituku',
    is_active               BOOLEAN DEFAULT TRUE,
    created_date            BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    last_login_date         BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    invited_by              VARCHAR(150),
    pin_code                VARCHAR(30) DEFAULT '1234',
    username                VARCHAR(100) DEFAULT '',
    is_online               BOOLEAN DEFAULT FALSE,
    last_active_timestamp   BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    avatar_key              VARCHAR(50) DEFAULT 'bybit_vip'
);

-- ======================================================================================
-- 2. TABLE DES CLIENTS DU MARCHÉ (Commerçants bénéficiaires de microcrédit)
-- Relation : assigned_agent_id -> users(id)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS clients (
    id                      VARCHAR(64) PRIMARY KEY,
    full_name               VARCHAR(150) NOT NULL,
    phone                   VARCHAR(30) NOT NULL,
    email                   VARCHAR(150) DEFAULT '',
    quarter                 VARCHAR(100) DEFAULT '',
    market_zone             VARCHAR(100) NOT NULL DEFAULT 'Marché de Kituku',
    activity_type           VARCHAR(150) NOT NULL DEFAULT 'Commerce de vivres',
    gender                  VARCHAR(20) DEFAULT 'Femme' CHECK (gender IN ('Femme', 'Homme')),
    registered_date         BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    is_id_verified          BOOLEAN DEFAULT FALSE,
    id_card_number          VARCHAR(100) DEFAULT '',
    guarantor_name          VARCHAR(150) DEFAULT '',
    guarantor_phone         VARCHAR(30) DEFAULT '',
    status                  VARCHAR(30) DEFAULT 'ACTIF' CHECK (status IN ('ACTIF', 'INACTIF', 'EN_RETARD')),
    assigned_agent_id       VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    notes                   TEXT DEFAULT '',
    avatar_url              TEXT,
    avatar_key              VARCHAR(50) DEFAULT 'bybit_female',
    is_online               BOOLEAN DEFAULT FALSE,
    last_active_timestamp   BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- ======================================================================================
-- 3. TABLE DES DEMANDES DE CRÉDIT
-- Relation : client_id -> clients(id)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS loan_applications (
    id                      VARCHAR(64) PRIMARY KEY,
    client_id               VARCHAR(64) NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_name             VARCHAR(150) NOT NULL,
    client_phone            VARCHAR(30) NOT NULL,
    amount_requested        NUMERIC(15, 2) NOT NULL CHECK (amount_requested > 0),
    duration_days           INT NOT NULL DEFAULT 30 CHECK (duration_days > 0),
    purpose                 TEXT DEFAULT '',
    activity_type           VARCHAR(150) DEFAULT '',
    gender                  VARCHAR(20) DEFAULT 'Femme',
    market_zone             VARCHAR(100) DEFAULT '',
    quarter                 VARCHAR(100) DEFAULT '',
    guarantor_name          VARCHAR(150) DEFAULT '',
    guarantor_phone         VARCHAR(30) DEFAULT '',
    id_card_number          VARCHAR(100) DEFAULT '',
    document_uri            TEXT,
    client_photo_uri        TEXT,
    daily_rate              NUMERIC(6, 4) DEFAULT 0.0350 CHECK (daily_rate > 0), -- Taux de base 3.5%
    daily_payment_amount    NUMERIC(15, 2) DEFAULT 0.0,
    accepted_terms          BOOLEAN DEFAULT TRUE,
    status                  VARCHAR(30) DEFAULT 'SOUMISE' CHECK (status IN ('SOUMISE', 'APPROUVEE', 'REFUSEE', 'DECAISSEE')),
    submission_date         BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    reviewed_by             VARCHAR(150),
    review_date             BIGINT,
    review_notes            TEXT
);

-- ======================================================================================
-- 4. TABLE DES CYCLES DE CRÉDIT (Cycles actifs de 30 jours & Suivi financier)
-- Relations : client_id -> clients(id), agent_id -> users(id), loan_application_id -> loan_applications(id)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS credit_cycles (
    id                      VARCHAR(64) PRIMARY KEY,
    client_id               VARCHAR(64) NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_name             VARCHAR(150) NOT NULL,
    client_phone            VARCHAR(30) NOT NULL,
    market_zone             VARCHAR(100) DEFAULT '',
    agent_id                VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    agent_name              VARCHAR(150),
    loan_application_id     VARCHAR(64) REFERENCES loan_applications(id) ON DELETE SET NULL,
    disbursement_date       BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    principal_amount        NUMERIC(15, 2) NOT NULL CHECK (principal_amount > 0),
    daily_payment_amount    NUMERIC(15, 2) NOT NULL CHECK (daily_payment_amount > 0),
    duration_days           INT NOT NULL DEFAULT 30 CHECK (duration_days > 0),
    total_contractual       NUMERIC(15, 2) NOT NULL CHECK (total_contractual >= principal_amount),
    total_collected         NUMERIC(15, 2) DEFAULT 0.0 CHECK (total_collected >= 0),
    balance_remaining       NUMERIC(15, 2) NOT NULL CHECK (balance_remaining >= 0),
    recovery_rate           NUMERIC(6, 2) DEFAULT 0.0,
    gross_profit            NUMERIC(15, 2) DEFAULT 0.0,
    enterprise_share        NUMERIC(15, 2) DEFAULT 0.0, -- Part entreprise (50%)
    client_share            NUMERIC(15, 2) DEFAULT 0.0, -- Part redistribution client (50%)
    charges                 NUMERIC(15, 2) DEFAULT 0.0,
    net_profit              NUMERIC(15, 2) DEFAULT 0.0,
    status                  VARCHAR(30) DEFAULT 'EN_COURS' CHECK (status IN ('EN_COURS', 'TERMINE', 'EN_RETARD', 'ANNULE')),
    days_elapsed            INT DEFAULT 0,
    days_late               INT DEFAULT 0,
    notes                   TEXT DEFAULT ''
);

-- ======================================================================================
-- 5. TABLE DES RECOUVREMENTS / PAIEMENTS JOURNALIERS
-- Relations : cycle_id -> credit_cycles(id), client_id -> clients(id), agent_id -> users(id)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS payments (
    id                      VARCHAR(64) PRIMARY KEY,
    cycle_id                VARCHAR(64) NOT NULL REFERENCES credit_cycles(id) ON DELETE CASCADE,
    client_id               VARCHAR(64) NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    client_name             VARCHAR(150) NOT NULL,
    agent_id                VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    agent_name              VARCHAR(150) NOT NULL,
    payment_date            BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    date_string             VARCHAR(30) DEFAULT '',
    day_number              INT NOT NULL CHECK (day_number > 0),
    expected_amount         NUMERIC(15, 2) NOT NULL CHECK (expected_amount >= 0),
    collected_amount        NUMERIC(15, 2) NOT NULL CHECK (collected_amount > 0),
    payment_method          VARCHAR(30) DEFAULT 'ESPECES' CHECK (payment_method IN ('ESPECES', 'AIRTEL_MONEY', 'MPESA', 'ORANGE_MONEY')),
    notes                   TEXT DEFAULT '',
    is_offline_synced       BOOLEAN DEFAULT TRUE,
    sync_pending            BOOLEAN DEFAULT FALSE,
    created_at              BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

-- ======================================================================================
-- 6. TABLE DES MOUVEMENTS DE CAISSE (Trésorerie Entrées & Sorties)
-- Relation : recorded_by_user_id -> users(id)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS cash_movements (
    id                      VARCHAR(64) PRIMARY KEY,
    date                    BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    date_string             VARCHAR(30) DEFAULT '',
    type                    VARCHAR(20) NOT NULL CHECK (type IN ('ENTREE', 'SORTIE')),
    category                VARCHAR(50) NOT NULL, -- 'APPORT_CAPITAL', 'REMBOURSEMENT', 'DECAISSEMENT_PRET', 'CHARGES_OPERATIONNELLES', etc.
    reference               VARCHAR(100) DEFAULT '',
    description             TEXT NOT NULL,
    amount                  NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    payment_method          VARCHAR(30) DEFAULT 'ESPECES',
    recorded_by_user_id     VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    recorded_by_name        VARCHAR(150) NOT NULL,
    receipt_uri             TEXT,
    is_approved             BOOLEAN DEFAULT TRUE
);

-- ======================================================================================
-- 7. TABLE DES PARAMÈTRES GÉNÉRAUX & MODALITÉS FINANCIÈRES
-- ======================================================================================
CREATE TABLE IF NOT EXISTS business_settings (
    id                      INT PRIMARY KEY DEFAULT 1,
    organization_name       VARCHAR(150) NOT NULL DEFAULT 'MALISHO NA AKIBA',
    currency                VARCHAR(10) NOT NULL DEFAULT 'FC',
    initial_capital         NUMERIC(15, 2) DEFAULT 5000000.00,
    target_capital          NUMERIC(15, 2) DEFAULT 15000000.00,
    default_daily_rate      NUMERIC(6, 4) DEFAULT 0.0350, -- 3.5% par jour
    min_initial_loan        NUMERIC(15, 2) DEFAULT 50000.00,
    max_initial_loan        NUMERIC(15, 2) DEFAULT 400000.00,
    default_duration_days   INT DEFAULT 30,
    enterprise_share_rate   NUMERIC(4, 2) DEFAULT 0.50,
    client_share_rate       NUMERIC(4, 2) DEFAULT 0.50,
    expense_allowance_rate  NUMERIC(4, 2) DEFAULT 0.05,
    loss_provision_rate     NUMERIC(4, 2) DEFAULT 0.02
);

-- ======================================================================================
-- 8. TABLE DES MESSAGES DU CHAT (Horodatage, Salons directs, Communauté, Vocal, Fichiers)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id                      VARCHAR(128) PRIMARY KEY,
    sender_id               VARCHAR(128) NOT NULL,
    sender_name             VARCHAR(150) NOT NULL,
    sender_role             VARCHAR(50) NOT NULL,
    client_id               TEXT NOT NULL, -- 'COMMUNITY_GROUP', 'CLI-...', ou 'DIRECT_USR-...'
    content                 TEXT NOT NULL,
    attachment_uri          TEXT,
    timestamp               BIGINT NOT NULL,
    is_read                 BOOLEAN DEFAULT FALSE,
    is_edited               BOOLEAN DEFAULT FALSE,
    edited_at               BIGINT,
    reply_to_message_id     VARCHAR(128),
    reply_to_sender_name    VARCHAR(150),
    reply_to_content_snippet TEXT,
    is_pinned               BOOLEAN DEFAULT FALSE,
    pinned_at               BIGINT,
    is_voice_message        BOOLEAN DEFAULT FALSE,
    voice_duration_seconds  INT DEFAULT 0,
    audio_file_path         TEXT,
    delivered_user_ids_json TEXT DEFAULT '[]',
    read_user_ids_json      TEXT DEFAULT '[]'
);

-- ======================================================================================
-- 9. TABLE DES NOTIFICATIONS SYSTÈME, MESSAGES & ALERTES CIBLÉES
-- ======================================================================================
CREATE TABLE IF NOT EXISTS app_notifications (
    id                      VARCHAR(128) PRIMARY KEY,
    target_user_id          VARCHAR(128),
    target_role             VARCHAR(50),
    target_client_id        TEXT,
    destination_address     VARCHAR(200),
    sender_id               VARCHAR(128),
    sender_name             VARCHAR(150),
    title                   VARCHAR(150) NOT NULL,
    message                 TEXT NOT NULL,
    type                    VARCHAR(50) DEFAULT 'SYSTEM',
    timestamp               BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    is_read                 BOOLEAN DEFAULT FALSE,
    action_route            TEXT
);

-- ======================================================================================
-- 10. TABLE DU JOURNAL D'AUDIT (Traçabilité & Sécurité)
-- Relation : user_id -> users(id)
-- ======================================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id                      VARCHAR(64) PRIMARY KEY,
    user_id                 VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    user_name               VARCHAR(150) NOT NULL,
    user_role               VARCHAR(30) NOT NULL,
    action                  VARCHAR(100) NOT NULL,
    entity_type             VARCHAR(50) NOT NULL,
    entity_id               VARCHAR(100) NOT NULL,
    details                 TEXT NOT NULL,
    timestamp               BIGINT NOT NULL
);

-- ======================================================================================
-- 11. INDEX D'OPTIMISATION DES REQUÊTES ET RELATIONS CLOUD
-- ======================================================================================
CREATE INDEX IF NOT EXISTS idx_clients_agent ON clients(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_loan_apps_client ON loan_applications(client_id);
CREATE INDEX IF NOT EXISTS idx_credit_cycles_client ON credit_cycles(client_id);
CREATE INDEX IF NOT EXISTS idx_credit_cycles_agent ON credit_cycles(agent_id);
CREATE INDEX IF NOT EXISTS idx_credit_cycles_status ON credit_cycles(status);
CREATE INDEX IF NOT EXISTS idx_payments_cycle ON payments(cycle_id);
CREATE INDEX IF NOT EXISTS idx_payments_client ON payments(client_id);
CREATE INDEX IF NOT EXISTS idx_payments_agent ON payments(agent_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_date ON cash_movements(date);
CREATE INDEX IF NOT EXISTS idx_chat_messages_client ON chat_messages(client_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_timestamp ON chat_messages(timestamp);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- ======================================================================================
-- 12. VUES DE GESTION EN TEMPS RÉEL (Trésorerie & Portefeuille)
-- ======================================================================================
DROP VIEW IF EXISTS v_caisse_solde;
CREATE VIEW v_caisse_solde AS
SELECT 
    COALESCE(SUM(CASE WHEN type = 'ENTREE' THEN amount ELSE -amount END), 0.0) AS solde_disponible,
    COALESCE(SUM(CASE WHEN type = 'ENTREE' THEN amount ELSE 0 END), 0.0) AS total_entrees,
    COALESCE(SUM(CASE WHEN type = 'SORTIE' THEN amount ELSE 0 END), 0.0) AS total_sorties
FROM cash_movements
WHERE is_approved = TRUE;

DROP VIEW IF EXISTS v_synthese_portefeuille;
CREATE VIEW v_synthese_portefeuille AS
SELECT 
    COUNT(*) AS total_cycles,
    COUNT(CASE WHEN status = 'EN_COURS' THEN 1 END) AS cycles_actifs,
    COUNT(CASE WHEN status = 'EN_RETARD' THEN 1 END) AS cycles_en_retard,
    COUNT(CASE WHEN status = 'TERMINE' THEN 1 END) AS cycles_termines,
    COALESCE(SUM(principal_amount), 0.0) AS total_debourse,
    COALESCE(SUM(total_contractual), 0.0) AS total_contractuel,
    COALESCE(SUM(total_collected), 0.0) AS total_recouvre,
    COALESCE(SUM(balance_remaining), 0.0) AS solde_a_recouvrer
FROM credit_cycles;

-- ======================================================================================
-- 13. DONNÉES DE BASE INITIALES (Direction & Configuration)
-- ======================================================================================
INSERT INTO users (
    id, full_name, phone, email, role, market_zone, is_active, 
    created_date, pin_code, username, is_online, last_active_timestamp, avatar_key
) VALUES (
    'USR-001', 'Jacques Masuruku', '+243 970 000 001', 'jacquesmasuruku2@gmail.com', 
    'GESTIONNAIRE', 'Direction Générale', TRUE, 
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT, '1234', 'directeur', TRUE, 
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT, 'bybit_vip'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO business_settings (
    id, organization_name, currency, initial_capital, target_capital, 
    default_daily_rate, min_initial_loan, max_initial_loan, default_duration_days, 
    enterprise_share_rate, client_share_rate, expense_allowance_rate, loss_provision_rate
) VALUES (
    1, 'MALISHO NA AKIBA', 'FC', 5000000.0, 15000000.0, 
    0.035, 50000.0, 400000.0, 30, 
    0.50, 0.50, 0.05, 0.02
) ON CONFLICT (id) DO NOTHING;

COMMIT;
