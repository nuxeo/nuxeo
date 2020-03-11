================
LDAP setup howto
================

:Author: Olivier Grisel <ogrisel@nuxeo.com>

.. sectnum::    :depth: 2
.. contents::   :depth: 2

Overview
========

This project (org.nuxeo.ecm.directory.ldap) provides a partial (readonly)
implementation for the Nuxeo Directory interface for a LDAP server
(OpenLDAP, MS Active Directory, Sun Directory Server, ...) as storage
backend.

The typical use case is to fetch users and groups definitions and
credential from the company directory instead of using the default
builtin SQL DB.


Deployment
==========

By default the LDAP connector with no configuration since the internal SQL
directories are used as defined in the ``nuxeo-platform-ear`` project.

To use the LDAP Directory implementation, you will need to manually deploy one
of the following configurations:

- Users in LDAP, groups in SQL:

  Go to the ``examples`` sub-folder and copy the
  ``default-ldap-users-directory-bundle.xml`` file  in the ``nuxeo.ear/config``
  folder of the JBoss instance.

  This sample setup replaces the default ``userDirectory`` configuration
  SQL with users fetched from the LDAP server. The ``groupDirectory``
  remains unaffected by this setup.

  You might want to copy the file ``default-virtual-groups-bundle.xml``
  and adjust ``defaultAdministratorId`` to select a user from your LDAP that
  have administrative rights by default.

  You can also configure the section on ``defaultGroup`` to make all users
  members of some default group (typically the ``members`` group) so that
  they have default right without having to make them belong to groups
  explicitly.


- Users and groups in LDAP:

  Copy the users setup as previously; moreover copy the
  ``default-ldap-groups-directory-bundle.xml` file in the ``nuxeo.ear/config``
  folder of the JBoss instance.

  This sample setup wich is dependant on the previous one additionally
  overides the default ``groupDirectory`` setup to read the groups from
  the LDAP directory typically from groupOfUniqueNames entries with
  dully quallified ``dn`` references to the user entries or to subgroups.


You can edit the ``nuxeo.ear/config/*.xml`` files on the Jboss instance but
will need to restart JBoss to take changes into account.


Advanced setup
==============

TODO:

- Talk about how to embed and deploy your LDAP configuration in your own
  custom jar file.

- Talk about the server configuration block.

- Talk about the concept of references.

- Talk about configuration on multi-server installation

Tools to setup a sample LDAP server
===================================

The ``ldaptools/`` folder provides sample ldiff files and OpenLDAP configuration
file to help you setup a sample OpenLDAP server you can use as a base config to
build your corporate directory.

Please refer to the included ``README.txt`` file for sample usage and basic
instructions.
