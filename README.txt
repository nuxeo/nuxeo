How to build Nuxeo EP from these sources
========================================

Short story
-----------

1. Make sure all the sources are checked out, as this is a Mercurial forest,
i.e. there are several different trees that must be present in this directory,
notably:

- nuxeo-common
- nuxeo-core
- nuxeo-platform
- nuxeo-runtime
- nuxeo-theme

See http://doc.nuxeo.org/xwiki/bin/view/FAQ/DownloadingNuxeoSources for more
information if needed.

2. Have the right version of JBoss installed somewhere on your system (we like
/opt/jboss, but YMMV).

"Right" = JBoss 4.2.2 or 4.2.3 for Nuxeo EP 5.2, and JBoss 4.0.5 for Nuxeo EP
5.1.

3. Copy build.properties.sample to build.properties and edit it to match the
location of your JBoss instance.

3. Run:

- "ant patch" (will "patch" your JBoss, modifying certain config files)

- "ant deploy"

NB: you only have to run "ant patch" once. You have to run "ant" or "ant
deploy" each time you change the source code.

Long story
----------

If the information above are not enough, please read the "Detailed Development
Software Installation Instructions" annex:

http://doc.nuxeo.org/5.1/books/nuxeo-book/html/dev-environment-installation.html

in the Nuxeo Book:

http://doc.nuxeo.org/5.1/books/nuxeo-book/html/
