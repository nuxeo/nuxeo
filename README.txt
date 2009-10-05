How to build Nuxeo EP from these sources
========================================

Short story
-----------

1. Make sure all the sources are checked out, as this is a Mercurial forest,
i.e. there are several different trees that must be present in this directory,
notably:

- nuxeo-common
- nuxeo-core
- nuxeo-distribution
- nuxeo-dm
- nuxeo-features
- nuxeo-gwt
- nuxeo-jsf
- nuxeo-runtime
- nuxeo-services
- nuxeo-theme
- nuxeo-webengine

See http://doc.nuxeo.org/xwiki/bin/view/FAQ/DownloadingNuxeoSources for more
information if needed.

2. Have JBoss 4.2.3 installed somewhere on your system (we like /opt/jboss, but YMMV).

3. Copy build.properties.sample to build.properties and edit it to match the
location of your JBoss instance (not needed if your JBoss is in /opt/jboss).

4. Run:

- "ant patch" (will "patch" your JBoss, modifying certain config files)

- "ant deploy"

NB: you only have to run "ant patch" once. You have to run "ant" or "ant
deploy" each time you change the source code.

5. Start JBoss and go to http://localhost:8080/nuxeo/

6. You're done.

Alternative distributions
-------------------------

To target other application servers (Jetty, Glassfish) or other DB backends
(PostgreSQL, MySQL, Oracle, ...) see nuxeo-distribution/README.txt

Long story
----------

If the information above are not enough, please read the "Detailed Development
Software Installation Instructions" annex:

http://doc.nuxeo.org/current/books/nuxeo-book/html/dev-environment-installation.html

in the Nuxeo Book:

http://doc.nuxeo.org/current/books/nuxeo-book/html/
