CREATE TABLE IF NOT EXISTS time (
id SERIAL PRIMARY KEY,
nome VARCHAR(255) UNIQUE NOT NULL,
logo TEXT
);


CREATE TABLE IF NOT EXISTS placar (
id SERIAL PRIMARY KEY,
hash_id VARCHAR(64) UNIQUE NOT NULL,
data_inicio TIMESTAMP DEFAULT now(),
status VARCHAR(20) DEFAULT 'ATIVO',
dados JSONB
);


-- procedure para iniciar placar (recebe JSON com times)
CREATE OR REPLACE FUNCTION sp_inicia_placar(dados JSONB) RETURNS JSONB AS $$
DECLARE
hid TEXT := encode(gen_random_bytes(16), 'hex');
rec JSONB;
BEGIN
INSERT INTO placar(hash_id, dados) VALUES (hid, dados) RETURNING jsonb_build_object('hash_id', hid) INTO rec;
RETURN rec;
END;
$$ LANGUAGE plpgsql;


-- atualizar: recebe hash_id e objeto JSON com alteração
CREATE OR REPLACE FUNCTION sp_atualiza_placar(hid TEXT, patch JSONB) RETURNS JSONB AS $$
DECLARE
cur JSONB;
BEGIN
SELECT dados INTO cur FROM placar WHERE hash_id = hid FOR UPDATE;
IF cur IS NULL THEN
RAISE EXCEPTION 'Placar nao encontrado';
END IF;
cur := cur || patch;
UPDATE placar SET dados = cur WHERE hash_id = hid;
RETURN cur;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION sp_finaliza_placar(hid TEXT) RETURNS VOID AS $$
BEGIN
UPDATE placar SET status = 'FINALIZADO' WHERE hash_id = hid;
END;
$$ LANGUAGE plpgsql;