Sample pure PostgreSQL setup
============================

Create a database 'dbname' for user 'username'::

  CREATE ROLE username WITH PASSWORD 'myPr3c1ous' LOGIN;
  CREATE DATABASE dbname WITH OWNER username;

Edit the connection parameters in batch::

 $ python dbsetup.py --url jdbc:postgresql://server:port/dbname \
                     --user usename --password myPr3c1ous

Use "ant copy-postgresql" in the top level build.xml file to deploy
that configuration to your instance.
