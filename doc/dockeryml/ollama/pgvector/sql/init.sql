CREATE EXTENSION IF NOT EXISTS vector;


CREATE TABLE public.vector_store (
        id TEXT PRIMARY KEY,
        content TEXT,
        metadata JSONB,
        embedding VECTOR(768)
);


/**
 * -- 删除旧的表（如果存在）
 * DROP TABLE IF EXISTS public.vector_store_ollama_deepseek;
 *
 * -- 创建新的表，使用UUID作为主键
 * CREATE TABLE public.vector_store_ollama_deepseek (
 *     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 *     content TEXT NOT NULL,
 *     metadata JSONB,
 *     embedding VECTOR(768)
 * );
 *
 * SELECT * FROM vector_store_ollama_deepseek
 */

DROP TABLE IF EXISTS public.vector_store_ollama_deepseek;
-- 创建新的表，使用UUID作为主键
CREATE TABLE public.vector_store_ollama_deepseek (
          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
          content TEXT NOT NULL,
          metadata JSONB,
          embedding VECTOR(768)
);
SELECT * FROM vector_store_ollama_deepseek