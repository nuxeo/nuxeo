Nuxeo Routing Default
=====================

This module defines the default workflow for Nuxeo.

## Information for Nuxeo developers

This module is adapted from the definition built from the Studio project named `nuxeo-routing-default`.

## Requirements

You need xmllint installed.
On Mac OS X (Yosemite), it is installed by default.
On Ubuntu, if it is not already installed, you can run apt-get install libxml2-utils

## Update

Changes in this module should be done in the Studio project to ensure
accurate synchronization of changes between the Studio project and
this code.

Here is the procedure to follow when making changes to files generated
by Studio:

- make changes in the Studio project, and commit with an accurate
  description of the changes (references to JIRA issues are very welcome),
- download the generated jar and unzip it in a temp folder,
- from this directory, run:

        $ ./etc/update.sh  temp-folder-where-jar-was-unzipped/

If you need more changes to the generated jar, you should update the
script at `etc/update.sh`.
