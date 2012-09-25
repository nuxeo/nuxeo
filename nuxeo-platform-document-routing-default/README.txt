=====================
Nuxeo Routing Default
=====================

This module defines the default workflow for Nuxeo.

It is adapted from the definition built from Studio project named
"nuxeo-routing-default".

Update
======

Changes in this module should be done in the Studio project to ensure
accurate synchronization of changes between the Studio project and
this code.

Here is the procedure to follow when making changes to files generated
by Studio:

- make changes in the Studio project, and commit with an accurate
  description of changes (references to JIRA issues are very welcome)
- download the generated jar and unzip in a temp folder
- from this directory, run:

    $ ./etc/update.sh  tmp-folder-where-jar-was-unzipped/

If you need more changes to the generated jar, you can update the
script at etc/update.sh.
