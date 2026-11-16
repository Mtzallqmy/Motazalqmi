BEGIN;
CREATE OR REPLACE FUNCTION blog_is_due(document jsonb) RETURNS boolean LANGUAGE plpgsql STABLE AS $$
BEGIN
 IF coalesce(document->>'scheduledAt','')='' THEN RETURN true; END IF;
 BEGIN
  RETURN (document->>'scheduledAt')::timestamptz <= now();
 EXCEPTION WHEN invalid_datetime_format OR datetime_field_overflow THEN RETURN false;
 END;
END $$;
CREATE OR REPLACE VIEW blog_published_articles AS
 SELECT * FROM blog_articles WHERE status='published' AND blog_is_due(data);
CREATE TABLE IF NOT EXISTS blog_deleted_media (
 url text PRIMARY KEY,
 state text NOT NULL CHECK(state IN ('pending','deleted')),
 requested_at timestamptz NOT NULL DEFAULT now(),
 deleted_at timestamptz
);
CREATE OR REPLACE FUNCTION blog_guard_media_reference() RETURNS trigger LANGUAGE plpgsql VOLATILE AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(6921330);
 IF EXISTS(SELECT 1 FROM blog_deleted_media WHERE strpos(NEW.data::text,url)>0) THEN
  RAISE check_violation USING MESSAGE='MEDIA_RESERVED_FOR_DELETION';
 END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE TRIGGER blog_article_media_guard BEFORE INSERT OR UPDATE ON blog_articles
 FOR EACH ROW EXECUTE FUNCTION blog_guard_media_reference();
CREATE OR REPLACE TRIGGER blog_metadata_media_guard BEFORE INSERT OR UPDATE ON blog_public_metadata
 FOR EACH ROW EXECUTE FUNCTION blog_guard_media_reference();
CREATE OR REPLACE TRIGGER blog_revision_media_guard BEFORE INSERT OR UPDATE ON blog_article_revisions
 FOR EACH ROW EXECUTE FUNCTION blog_guard_media_reference();
COMMIT;
