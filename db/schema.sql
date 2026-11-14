-- Moataz Alqami cultural blog — schema v1 (Neon PostgreSQL)
CREATE TABLE IF NOT EXISTS public.blog_articles (
  id TEXT PRIMARY KEY,
  slug TEXT NOT NULL UNIQUE,
  status TEXT NOT NULL CHECK (status IN ('draft', 'published')),
  publish_date DATE NOT NULL DEFAULT CURRENT_DATE,
  data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS blog_articles_published_idx
  ON public.blog_articles(status, publish_date DESC, updated_at DESC);
CREATE TABLE IF NOT EXISTS public.blog_login_attempts (
  ip_hash TEXT PRIMARY KEY,
  attempts INTEGER NOT NULL DEFAULT 0,
  last_attempt TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
