BEGIN;
CREATE TABLE IF NOT EXISTS blog_article_slugs (
 slug text PRIMARY KEY,
 article_id text NOT NULL REFERENCES blog_articles(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS blog_article_revisions (
 revision_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 article_id text NOT NULL REFERENCES blog_articles(id) ON DELETE CASCADE,
 saved_at timestamptz NOT NULL DEFAULT now(),
 data jsonb NOT NULL
);
CREATE INDEX IF NOT EXISTS blog_revision_article_idx ON blog_article_revisions(article_id, revision_id DESC);
LOCK TABLE blog_articles IN SHARE ROW EXCLUSIVE MODE;
INSERT INTO blog_article_slugs(slug,article_id) SELECT slug,id FROM blog_articles ON CONFLICT DO NOTHING;
CREATE OR REPLACE FUNCTION blog_track_article() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO blog_article_slugs(slug,article_id) VALUES(NEW.slug,NEW.id)
 ON CONFLICT(slug) DO UPDATE SET article_id=EXCLUDED.article_id
 WHERE blog_article_slugs.article_id=EXCLUDED.article_id;
 IF NOT FOUND THEN RAISE unique_violation USING MESSAGE='Previously assigned article slug'; END IF;
 IF TG_OP='UPDATE' AND (OLD.data,OLD.slug,OLD.status) IS DISTINCT FROM (NEW.data,NEW.slug,NEW.status) THEN
  INSERT INTO blog_article_revisions(article_id,data) VALUES(OLD.id,OLD.data || jsonb_build_object(
   'id',OLD.id,'slug',OLD.slug,'status',OLD.status,'publishDate',OLD.publish_date,'updatedAt',OLD.updated_at));
 END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE TRIGGER blog_article_history AFTER INSERT OR UPDATE ON blog_articles
 FOR EACH ROW EXECUTE FUNCTION blog_track_article();
COMMIT;
