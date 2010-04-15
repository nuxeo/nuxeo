-----
About
-----

The templates in this directory are used to generate configuration and datasource
files at server startup only when the server is not already configured.  

1. Usage

Edit nuxeo.conf and set the name of the template to use (default template is "default").

Set the properties you want to customize, see nuxeo.defaults files for available parameters.
For example, recommended changes are:
  nuxeo.template=default
  nuxeo.bind.address=0.0.0.0

See point 3 for advanced customization.

DO NOT EDIT nuxeo.defaults files.

2. Available templates

2.1 default

Default Nuxeo configuration.
Designed for development or test purpose.
Repository backend: H2
Tags service backend: H2
Other services backend: Derby

2.2 postgresql

Recommended configuration for production, based on PostgreSQL.
Think to add PostgreSQL JDBC driver into server/default/lib/ directory.
Repository backend: PostgreSQL XA
Tags service backend: PostgreSQL XA
Other services backend: PostgreSQL XA

2.3 mysql

Repository backend: MySQL XA
Tags service backend: MySQL
Other services backend: MySQL

2.4 oracle

Repository backend: Oracle XA
Tags service backend: Oracle
Other services backend: Oracle

3. Customization

For custom configuration purpose.
Add your own template files in "templates/${server}/custom" directory. All files in this directory
will override the default configuration files. You can use new parameters in these new 
template files and set them from nuxeo.conf.
This allows to add customization such as using multiple databases, configuring services, ...

