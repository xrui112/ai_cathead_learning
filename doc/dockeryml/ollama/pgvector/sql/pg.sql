CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS long_term_memory (
                                                id TEXT PRIMARY KEY,
                                                session_id TEXT,
                                                knowledge_id TEXT,
                                                agent_id TEXT,
                                                title TEXT,
                                                summary TEXT,
                                                tags TEXT,
                                                importance DOUBLE PRECISION,
                                                created_at TIMESTAMP,
                                                last_access_at TIMESTAMP,
                                                embedding VECTOR(1536)
);

-- 常用索引（可选）
CREATE INDEX IF NOT EXISTS idx_ltm_session ON long_term_memory(session_id);
CREATE INDEX IF NOT EXISTS idx_ltm_knowledge ON long_term_memory(knowledge_id);
CREATE INDEX IF NOT EXISTS idx_ltm_agent ON long_term_memory(agent_id);