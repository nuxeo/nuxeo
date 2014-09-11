-- display the path of a doc id
CREATE OR REPLACE FUNCTION nx_path(docid VARCHAR(36))
RETURNS varchar
AS $$
-- Return path of a doc, only used for SQL debug purpose
BEGIN
  RETURN (SELECT array_to_string(array_agg(name),'/','/')
    FROM hierarchy h
    JOIN (SELECT unnest(ancestors) AS id FROM ancestors WHERE id=docid
          UNION ALL SELECT docid) v ON v.id = h.id);
END $$
LANGUAGE plpgsql
STABLE;

-- view of ACL that need to be reworked to match Simplified ACL
CREATE OR REPLACE VIEW nx_aclr_limited_violation AS SELECT a.id, h.primarytype, nx_get_local_read_acl(a.id) AS local_read_ACL, nx_path(a.id) AS path
FROM acls a
JOIN hierarchy h on a.id = h.id
WHERE NOT a."grant" AND a."user" != 'Everyone' AND a."permission" IN (SELECT permission FROM aclr_permission);

-- List the documents that have an ACL to change
SELECT * FROM nx_aclr_limited_violation;
