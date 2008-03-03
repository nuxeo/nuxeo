Sample utilities for LDAP setup
===============================
$Id: README.txt 15629 2007-04-05 16:32:34Z ogrisel $

- sample-users.ldif:
  provides a bunch of ldap entries to test your LDAP setup

- sample-groups.ldif:
  provides a bunch of ldap entries to test your LDAP setup with groups; this
  requires sample-users.ldif to be loaded first

- slapd.conf:
  sample OpenLDAP server configuration files that fit the sample-user.ldif setup

- ssha.py:
  utility script to compute SSHA digest for passwords, usage:
   $ python ssha.py mypassword

Use the slapadd command line tool to load the ldif configuration to your
OpenLDAP server:

  $ sudo slapadd -d 256 -vc -l sample-users.ldif

You might also want to load the groups:

  $ sudo slapadd -d 256 -vc -l sample-groups.ldif

You might need to restart your server to take the new tree into account.

To check/export what's in the OpenLDAP server:

  $ sudo slapcat

You can also use graphical LDAP client such luma (linux only) or ldapbrowser
(cross platform) to test the config of your LDAP server.

For more details on OpenLDAP administration, please refer to this excellent
online technical guide:

  http://www.zytrax.com/books/ldap/

