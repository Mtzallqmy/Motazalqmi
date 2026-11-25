-- Public short addresses are derived from immutable article IDs. No content is rewritten.
CREATE UNIQUE INDEX IF NOT EXISTS blog_articles_short_code_idx
ON public.blog_articles ((substr(md5(id), 1, 16)));
