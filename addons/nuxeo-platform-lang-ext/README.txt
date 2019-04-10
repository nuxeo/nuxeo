Nuxeo translation sandbox
=========================

This module contains community-created translations, as well as some basic
tools to help manage these translations.

How to create a new translation?
--------------------------------

1. Start from the messages_en.properties file that's found in the main source repository, in:

nuxeo-features/nuxeo-platform-lang/src/main/resources/nuxeo.war/WEB-INF/classes

and copy it to messages_XX.properties where XX is the 2 letters codename for
your language.

2. Translate the strings.

3. Send the file back to us, so that we can integrate it in the SVN. Or better,
create a task in the Jira (http://jira.nuxeo.org/) with the translation
attached to it.

4. If your translation seems OK, we will grant you SVN access to this module so
that you can update the translation yourself afterward.

How to update an existing translation?
--------------------------------------

1. Take a messages_XX.properties file, update it, send us a patch or attach it
to Jira (see above).

2. If your translation seems OK, we will grant you SVN access to this module so
that you can update the translation yourself afterward.

How to use my new translation?
------------------------------

1. Put the messages_XX.properties file with the other properties files,
if you're building from the source, or into:

/opt/jboss/server/default/deploy/nuxeo.ear/nuxeo.war/WEB-INF/classes

to be able to see your translations at runtime without rebuilding the
application.

(Assuming your jboss lives in /opt/jboss).

2. In the file:

src/main/resources/OSGI-INF/deployment-fragment.xml (in the sources)

or:

/opt/jboss/server/default/deploy/nuxeo.ear/nuxeo.war/WEB-INF/faces-config.xml

add your language in the list that looks like this:

<locale-config>
  <default-locale>en</default-locale>
  <supported-locale>en</supported-locale>
  <supported-locale>fr</supported-locale>
  <supported-locale>de</supported-locale>
  <supported-locale>it</supported-locale>
</locale-config>
<message-bundle>messages</message-bundle>

