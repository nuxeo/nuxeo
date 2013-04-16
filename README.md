test to delete

About the Nuxeo Platform Project
==========================

What is Nuxeo Platform?
-----------------

Nuxeo Platform is an open source platform for Enterprise Content Management.

See: <http://www.nuxeo.com/en/products/ep> for a list of features and
benefits.

See: <http://doc.nuxeo.com/display/MAIN/Getting+started+with+Nuxeo+--+a+beginner's+page>
for a short introduction to Nuxeo EP.

See: <http://en.wikipedia.org/wiki/Enterprise_content_management> for a
general definition of Enterprise Content Management.


How to compile the Nuxeo EP sources
-----------------------------------

### Short story

Several sub-repositories need to be present to build Nuxeo:

  - nuxeo-common
  - nuxeo-runtime
  - nuxeo-core
  - nuxeo-services
  - nuxeo-theme
  - nuxeo-jsf
  - nuxeo-webengine
  - nuxeo-features
  - nuxeo-dm
  - addons/*
  - nuxeo-distribution

#### Clone the sources

For read-only access, run `git clone git://github.com/nuxeo/nuxeo.git`

For read+write access, run `git clone git@github.com:nuxeo/nuxeo.git`

Update to master branch: `git checkout master`

Finally run `python clone.py [wanted branch/tag]`

See [How to download the Nuxeo Platform source code ](http://doc.nuxeo.com/x/cwQz)
for more information if needed.

#### Launch the build

    mvn install -DskipTests=true -Paddons,distrib

You will get your tomcat-based build in the `nuxeo-distribution/nuxeo-distribution-tomcat/target` directory.

#### Run the tests

(see http://doc.nuxeo.com/display/CORG)

### Packaging Nuxeo EP from sources

Various pre-configured packages (various application servers and multiple
backends) are available for download from: <http://www.nuxeo.com/downloads>

In order to locally build Nuxeo EP, see `nuxeo-distribution/README.txt`.

### Long(er) story

If the information above are not enough, please read from the Nuxeo Book the
"Detailed Development Software Installation Instructions" annex:
<http://doc.nuxeo.org/current/books/nuxeo-book/html/dev-environment-installation.html>


Where to get help and get involved
----------------------------------

First, look at the documentation, on <http://doc.nuxeo.com>.

The Nuxeo Community Forum (<http://forum.nuxeo.com/>) is the place where
thousands of Nuxeo users gathers to exchange questions and answers,
information and tips. We have also a few mailing lists that mirror some
of the forums: <http://lists.nuxeo.com/>

If you've found a bug and want to suggest an improvement, you can use our
Jira issue tracker: <http://jira.nuxeo.org/>.

Last, if you need professional support for your critical application, we have
a subscription program: <http://www.nuxeo.com/en/subscription> that also
packages additional services.


How to contribute
-----------------

See this page for practical information:
<http://doc.nuxeo.com/display/NXDOC/Nuxeo+contributors+welcome+page>

This presentation will give you more insight about "the Nuxeo way":
<http://www.slideshare.net/nuxeo/nuxeo-world-session-becoming-a-contributor-how-to-get-started>


About Nuxeo
-----------

Nuxeo provides a modular, extensible Java-based
[open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep),
and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management),
[digital asset management](http://www.nuxeo.com/en/products/dam) and
[case management](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern
architecture, a powerful plug-in model and extensive packaging
capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

