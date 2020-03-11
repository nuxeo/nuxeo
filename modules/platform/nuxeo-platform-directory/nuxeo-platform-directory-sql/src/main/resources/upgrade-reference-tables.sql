-- This SQL script aims at upgrading the tables that
-- links users and groups directories between 5.1M1 and 5.1M2
--
-- On a standalone server, you can upgrade be injecting the
-- script live, eg on PostgreSQL:
--
--   $ psql -d nxsqldirectory < upgrade-reference-tables.sql
--
-- With the builtin HSQL db you can do the following:
--
--   0- backup your data :)
--   1- stop your running JBoss instance if any
--   2- edit the default-sql-directories-bundle.xml file in nuxeo.ear
--   3- change <sqlScript>setup-default-directories.sql</sqlScript> by
--             <sqlScript>upgrade-reference-tables.sql</sqlScript>
--   4- change <createTablePolicy>on_missing_columns</createTablePolicy> by
--             <createTablePolicy>always</createTablePolicy>
--   5- restart JBoss and log in as Administrator
--   6- stop JBoss and restore previous settings: do not forget to reset the
--      <createTablePolicy>on_missing_columns</createTablePolicy>


ALTER TABLE USER2GROUP DROP COLUMN ID;
ALTER TABLE USER2GROUP ADD PRIMARY KEY (USERID, GROUPID);

ALTER TABLE GROUP2GROUP DROP COLUMN ID;
ALTER TABLE GROUP2GROUP ADD PRIMARY KEY(PARENTGROUPID, CHILDGROUPID);
