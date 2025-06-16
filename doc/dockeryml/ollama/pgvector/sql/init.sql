CREATE EXTENSION IF NOT EXISTS vector;


CREATE TABLE public.vector_store (
        id TEXT PRIMARY KEY,
        content TEXT,
        metadata JSONB,
        embedding VECTOR(768)
);