/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * This PostgreSQL SQL scripts add new tables and functions
 * to replace NX_ACCESS_ALLOWED security checks with a simple join:
 *
 *  SELECT * FROM hierarchy AS h
 *  LEFT JOIN hierarchy_read_acl AS r ON h.id = r.id
 *  WHERE r.acl_id IN (SELECT * FROM nx_get_read_acls_for('{user1,members}'));
 *
 * Tables:
 *  read_acls: store canonnical read acl like
 *         -robert,Administrator,administrators,members
 *  hierarchy_read_acl: associate a read acl for each hierarch entry
 *  hierarchy_modifed_acl: log modified hierarchy that need read acl update
 *
 * API:
 *  nx_get_read_acls_for: list read acls for a user/groups
 *    SELECT * FROM nx_get_read_acls_for('{user1,members,Administrator}');
 *  nx_update_read_acls: update the read acls
 *    SELECT nx_update_read_acls();
 *  nx_rebuild_read_acls: rebuild all read acls
 *    SELECT nx_rebuild_read_acls();
 *
 * Note that changes in acls are tracked using triggers on hierarchy and
 * acls tables application must call "SELECT nx_update_read_acls()" to perform
 * the read acl update.
 *
 *
 * Contributors:
 *  Benoit Delbosc
 *
 */

------------------------------------------------------------
-- TABLES
------------------------------------------------------------


------------------------------------------------------------
-- read_acls: Store all cannonical read acls
--
DROP TABLE IF EXISTS read_acls;
CREATE TABLE read_acls (
  id character varying(34) PRIMARY KEY,
  acl character varying(4096)
);

ALTER TABLE public.read_acls OWNER TO qualiscope;


------------------------------------------------------------
-- hierarchy_read_acl: Add a read acl for each hierarchy entry
--
DROP TABLE IF EXISTS hierarchy_read_acl;
CREATE TABLE hierarchy_read_acl (
  id character varying(36) PRIMARY KEY,
  acl_id character varying(34)
);

ALTER TABLE public.hierarchy_read_acl OWNER TO qualiscope;

ALTER TABLE ONLY hierarchy_read_acl ADD CONSTRAINT
  hierarchy_read_acl_id_fk FOREIGN KEY (id) REFERENCES hierarchy(id)
  ON DELETE CASCADE;

CREATE INDEX hierarchy_read_acl_acl_id_idx ON hierarchy_read_acl USING btree (acl_id);


------------------------------------------------------------
-- hierarchy_modified_acl: Log hierarchy with updated read acl
--
DROP TABLE IF EXISTS hierarchy_modified_acl;
CREATE TABLE hierarchy_modified_acl (
  id character varying(36) PRIMARY KEY,
  is_new boolean
);

ALTER TABLE public.hierarchy_modified_acl OWNER TO qualiscope;

ALTER TABLE ONLY hierarchy_modified_acl ADD CONSTRAINT
  hierarchy_modified_acl_id_fk FOREIGN KEY (id) REFERENCES hierarchy(id)
  ON DELETE CASCADE;

------------------------------------------------------------
-- FUNCTIONS
------------------------------------------------------------

------------------------------------------------------------
-- Compute the read acl for a hierarchy id using a local acl
--
CREATE OR REPLACE FUNCTION nx_get_local_read_acl(id character varying) RETURNS character varying AS $$
DECLARE
  curid varchar(36) := id;
  read_acl varchar(4096) := NULL;
  r record;
BEGIN
  -- RAISE INFO 'call %', curid;
  FOR r in SELECT CASE
         WHEN (acls.grant AND
             acls.permission IN ('Read', 'ReadWrite', 'Everything')) THEN
           acls.user
         WHEN (NOT acls.grant AND
             acls.permission IN ('Read', 'ReadWrite', 'Everything')) THEN
           '-'|| acls.user
         ELSE NULL END AS op
       FROM acls WHERE acls.id = curid
       ORDER BY acls.pos LOOP
    IF r.op IS NULL THEN
      CONTINUE;
    END IF;
    IF read_acl IS NULL THEN
      read_acl := r.op;
    ELSE
      read_acl := read_acl || ',' || r.op;
    END IF;
  END LOOP;
  RETURN read_acl;
END $$
LANGUAGE plpgsql STABLE;

ALTER FUNCTION public.nx_get_local_read_acl(id character varying) OWNER TO qualiscope;

------------------------------------------------------------
-- Compute the read acl for a hierarchy id using inherited acl
--
CREATE OR REPLACE FUNCTION nx_get_read_acl(id character varying) RETURNS character varying AS $$
DECLARE
  curid varchar(36) := id;
  newid varchar(36);
  first boolean := true;
  read_acl varchar(4096);
  ret varchar(4096);
BEGIN
  -- RAISE INFO 'call %', curid;
  WHILE curid IS NOT NULL LOOP
    -- RAISE INFO '  curid %', curid;
    SELECT nx_get_local_read_acl(curid) INTO read_acl;
    IF (read_acl IS NOT NULL) THEN
      IF (ret is NULL) THEN
        ret = read_acl;
      ELSE
        ret := ret || ',' || read_acl;
      END IF;
    END IF;
    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;
    IF (first AND newid IS NULL) THEN
      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;
    END IF;
    first := false;
    curid := newid;
  END LOOP;
  RETURN ret;
END $$
LANGUAGE plpgsql STABLE;

ALTER FUNCTION public.nx_get_read_acl(id character varying) OWNER TO qualiscope;


------------------------------------------------------------
-- List read acl ids for a list of user/groups
--
CREATE OR REPLACE FUNCTION nx_get_read_acls_for(users character varying[]) RETURNS SETOF text AS $$
DECLARE
  r record;
  rr record;
  users_blacklist character varying[];
BEGIN
  RAISE INFO 'nx_get_read_acls_for called';
  -- Build a black list with negative users
  SELECT regexp_split_to_array('-' || array_to_string(users, ',-'), ',')
    INTO users_blacklist;
  <<acl_loop>>
  FOR r IN SELECT read_acls.id, read_acls.acl FROM read_acls LOOP
    -- RAISE INFO 'ACL %', r.id;
    -- split the acl into aces
    FOR rr IN SELECT ace FROM regexp_split_to_table(r.acl, ',') AS ace LOOP
       -- RAISE INFO '  ACE %', rr.ace;
       IF (rr.ace = ANY(users)) THEN
         -- RAISE INFO '  GRANT %', users;
         RETURN NEXT r.id;
         CONTINUE acl_loop;
         -- ok
       ELSEIF (rr.ace = ANY(users_blacklist)) THEN
         -- RAISE INFO '  DENY';
         CONTINUE acl_loop;
       END IF;
    END LOOP;
  END LOOP acl_loop;
  RETURN;
END $$
LANGUAGE plpgsql STABLE;

ALTER FUNCTION public.nx_get_read_acls_for(users character varying[]) OWNER TO qualiscope;


------------------------------------------------------------
-- Trigger to log change in the acls table
--
CREATE OR REPLACE FUNCTION nx_log_acls_modified() RETURNS trigger  AS $$
DECLARE
  doc_id varchar(36);
BEGIN
  IF (TG_OP = 'DELETE') THEN
    doc_id := OLD.id;
  ELSE
    doc_id := NEW.id;
  END IF;
  INSERT INTO hierarchy_modified_acl VALUES(doc_id, 'f');
  RETURN NEW;
END $$
LANGUAGE plpgsql;

ALTER FUNCTION public.nx_log_acls_modified() OWNER TO qualiscope;

DROP TRIGGER IF EXISTS nx_trig_acls_modified ON acls;
CREATE TRIGGER nx_trig_acls_modified
  AFTER INSERT OR UPDATE OR DELETE ON acls
  FOR EACH ROW EXECUTE PROCEDURE nx_log_acls_modified();

------------------------------------------------------------
-- Trigger to log doc_id that need read acl update
--
CREATE OR REPLACE FUNCTION nx_log_hierarchy_modified() RETURNS trigger  AS $$
DECLARE
  doc_id varchar(36);
BEGIN
  IF (TG_OP = 'INSERT') THEN
    -- New document
    INSERT INTO hierarchy_modified_acl VALUES(NEW.id, 't');
  ELSEIF (TG_OP = 'UPDATE') THEN
    IF (NEW.parentid != OLD.parentid) THEN
      -- New container
      INSERT INTO hierarchy_modified_acl VALUES(NEW.id, 'f');
    END IF;
  END IF;
  RETURN NEW;
END $$
LANGUAGE plpgsql;

ALTER FUNCTION public.nx_log_hierarchy_modified() OWNER TO qualiscope;

DROP TRIGGER IF EXISTS nx_trig_hierarchy_modified ON hierarchy;
CREATE TRIGGER nx_trig_hierarchy_modified
  AFTER INSERT OR UPDATE OR DELETE ON hierarchy
  FOR EACH ROW EXECUTE PROCEDURE nx_log_hierarchy_modified();


------------------------------------------------------------
-- Rebuild the read acls tables
--
CREATE OR REPLACE FUNCTION nx_rebuild_read_acls() RETURNS void AS $$
BEGIN
  RAISE INFO 'nx_rebuild_read_acls truncate hierarchy_read_acl';
  TRUNCATE TABLE hierarchy_read_acl;
  RAISE INFO 'nx_rebuild_read_acls update acl map';
  INSERT INTO hierarchy_read_acl
    SELECT id, md5(nx_get_read_acl(id))
    FROM (SELECT DISTINCT(id) AS id FROM hierarchy) AS uids;
  RAISE INFO 'nx_rebuild_read_acls truncate read_acls';
  TRUNCATE TABLE read_acls;
  INSERT INTO read_acls
    SELECT md5(acl), acl
    FROM (SELECT DISTINCT(nx_get_read_acl(id)) AS acl
        FROM  (SELECT DISTINCT(id) AS id
           FROM acls) AS uids) AS read_acls_input;
  RAISE INFO 'nx_rebuild_read_acls done';
  RETURN;
END $$
LANGUAGE plpgsql VOLATILE;

ALTER FUNCTION public.nx_rebuild_read_acls() OWNER TO qualiscope;


------------------------------------------------------------
-- Rebuild only necessary read acls
--
CREATE OR REPLACE FUNCTION nx_update_read_acls() RETURNS void AS $$
DECLARE
  update_count integer;
BEGIN
  -- Rebuild read_acls
  RAISE INFO 'nx_update_read_acls REBUILD read_acls';
  TRUNCATE TABLE read_acls;
  INSERT INTO read_acls
    SELECT md5(acl), acl
    FROM (SELECT DISTINCT(nx_get_read_acl(id)) AS acl
        FROM (SELECT DISTINCT(id) AS id FROM acls) AS uids) AS read_acls_input;

  -- New hierarchy_read_acl entry
  RAISE INFO 'nx_update_read_acls ADD NEW hierarchy_read_acl entry';
  INSERT INTO hierarchy_read_acl
    SELECT id, md5(nx_get_read_acl(id))
    FROM (SELECT DISTINCT(id) AS id
        FROM hierarchy_modified_acl WHERE is_new) AS uids;
  GET DIAGNOSTICS update_count = ROW_COUNT;
  RAISE INFO 'nx_update_read_acls % hierarchy_read_acl ADDED', update_count;
  DELETE FROM hierarchy_modified_acl WHERE is_new;

  -- Update hierarchy_read_acl entry
  RAISE INFO 'nx_update_read_acls UPDATE existing hierarchy_read_acl';
  -- Mark acl that need to be updated (set to NULL)
  UPDATE hierarchy_read_acl SET acl_id = NULL WHERE id IN (
    SELECT DISTINCT(id) AS id FROM hierarchy_modified_acl WHERE NOT is_new);
  GET DIAGNOSTICS update_count = ROW_COUNT;
  RAISE INFO 'nx_update_read_acls % hierarchy_read_acl MARKED', update_count;
  DELETE FROM hierarchy_modified_acl WHERE NOT is_new;
  -- Mark all childrens
  LOOP
    UPDATE hierarchy_read_acl SET acl_id = NULL WHERE id IN (
      SELECT h.id
      FROM hierarchy AS h
      LEFT JOIN hierarchy_read_acl AS r ON h.id = r.id
      WHERE r.acl_id IS NOT NULL
        AND h.parentid IN (SELECT id FROM hierarchy_read_acl WHERE acl_id IS NULL));
    GET DIAGNOSTICS update_count = ROW_COUNT;
    RAISE INFO 'nx_update_read_acls % hierarchy_read_acl MARKED for udpate', update_count;
    IF (update_count = 0) THEN
      EXIT;
    END IF;
  END LOOP;
  -- Update hierarchy_read_acl acl_ids
  UPDATE hierarchy_read_acl SET acl_id = md5(id) WHERE acl_id IS NULL;
  GET DIAGNOSTICS update_count = ROW_COUNT;
  RAISE INFO 'nx_update_read_acls % hierarchy_read_acl UPDATED', update_count;

  RETURN;
END $$
LANGUAGE plpgsql VOLATILE;

ALTER FUNCTION public.nx_update_read_acls() OWNER TO qualiscope;



------------------------------------------------------------
-- MAIN
------------------------------------------------------------

-- Populate the read_acls
SELECT nx_rebuild_read_acls();


-- test
SELECT * FROM nx_get_read_acls_for('{members,user45,Everyone}');

