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

You can also use graphical LDAP client such as Apache Directory Studio
(cross platform) to test the config of your LDAP server.

Use the ldaploader.py python script to load you LDAP server with random
user and groups in order to tests the application with many users and groups.
(edit it to adjust the global variables)::

  $ sudo aptitude install python-ldap
  $ python ldaploader.py                                                                                                                                                 ~
  2008-10-03 21:35:49,346 INFO created branch ou=people,dc=example,dc=com
  2008-10-03 21:35:49,346 INFO about to inject 1000 users
  2008-10-03 21:35:49,890 INFO injected 100 users at 184 users/s, eta: 0m 4s
  2008-10-03 21:35:50,389 INFO injected 200 users at 200 users/s, eta: 0m 3s
  2008-10-03 21:35:51,049 INFO injected 300 users at 151 users/s, eta: 0m 4s
  2008-10-03 21:35:52,065 INFO injected 400 users at 98 users/s, eta: 0m 6s
  2008-10-03 21:35:52,832 INFO injected 500 users at 130 users/s, eta: 0m 3s
  2008-10-03 21:35:53,457 INFO injected 600 users at 160 users/s, eta: 0m 2s
  2008-10-03 21:35:54,138 INFO injected 700 users at 146 users/s, eta: 0m 2s
  2008-10-03 21:35:54,684 INFO injected 800 users at 182 users/s, eta: 0m 1s
  2008-10-03 21:35:55,214 INFO injected 900 users at 188 users/s, eta: 0m 0s
  2008-10-03 21:35:56,037 INFO injected 1000 users at 121 users/s, eta: 0m 0s
  2008-10-03 21:35:56,045 INFO created branch ou=groups,dc=example,dc=com
  2008-10-03 21:35:56,045 INFO about to inject 100 groups
  2008-10-03 21:36:00,895 INFO injected 100 groups at 20 groups/s, eta: 0m 0s


For more details on OpenLDAP administration, please refer to this excellent
online technical guide:

  http://www.zytrax.com/books/ldap/
