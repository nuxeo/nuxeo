Sample pure PostgreSQL setup
============================

Create a database 'dbname' for user 'username'::

  CREATE ROLE username WITH PASSWORD 'myPr3c1ous' LOGIN;
  CREATE DATABASE dbname WITH OWNER username;

Edit the connection parameters in batch::

 $ python dbsetup.py --url jdbc:postgresql://server:port/dbname \
                     --user usename --password myPr3c1ous

Note that you need lxml (python-lxml under debian/ubuntu) installed for this
script to work:

  http://codespeak.net/lxml/

Use "ant copy-postgresql" in the top level build.xml file to deploy
that configuration to your instance.
