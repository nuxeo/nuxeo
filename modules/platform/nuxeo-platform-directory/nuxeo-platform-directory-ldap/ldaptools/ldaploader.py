#!/usr/bin/python
"""Utility script to load an LDAP server with sample data"""

from __future__ import with_statement
from contextlib import closing

import ldap
import ldap.modlist
import random
import string
import logging
import logging.config
import os.path
import time

BIND_DN = "cn=ldapadmin,dc=example,dc=com"
BIND_PASSWORD = "changeme"

LDAP_SERVER = "localhost"
LDAP_PORT = 389

VOYELS = "aaeeiou"
CONSUMNS = "bbccddffghjklmmmnnppqrsssttvwxyz"

LOGGING_CONFIGURATION = "ldaploader.conf"
LOGGING_CONFIGURATION_CONTENT = """\
[loggers]
keys=root

[logger_root]
level=INFO
handlers=console

[handlers]
keys=console

[handler_console]
class=StreamHandler
level=NOTSET
formatter=default
args=(sys.stderr,)

[formatters]
keys=default

[formatter_default]
format=%(asctime)s %(levelname)s %(message)s
datefmt=
class=logging.Formatter
"""

class Loader(object):
    """Loader class able to generate random users and groups"""

    logger  = logging.getLogger()

    base_dn = "dc=example,dc=com"

    users_branch_dn = "ou=people," + base_dn

    groups_branch_dn = "ou=groups," + base_dn

    nb_users = 1000

    nb_groups = 100

    max_users_in_group = 1000

    max_subgroups_in_group = 10

    checkpoint = 100

    closed = True

    def __init__(self, server, port=389, dn="", password="", use_ssl=False):
        url = "ldap%s://%s:%d" % (use_ssl and "s" or "", server, port)
        self._conn = ldap.initialize(url)
        self._conn.simple_bind_s(dn, password)
        self.closed = False

        self._rng = random.Random(0)
        self._generated_users = set()
        self._generated_groups = set()

    def close(self):
        if not self.closed:
            self._conn.unbind_s()
            self.closed = True

    def __del__(self):
        self.close()

    def compute_eta(self, index, total, last_time, current_time):
        speed = float(self.checkpoint) / (current_time - last_time)
        eta = (total - index) / speed
        return speed, eta

    def inject_users(self, n=None):
        self.check_branch("people")
        if n is None:
            n = self.nb_users
        self.logger.info("about to inject %d users", n)
        checkpoint_time = time.time()
        for i in xrange(1, n + 1):
            self.inject_user()
            if i % self.checkpoint == 0:
                last_checkpoint_time = checkpoint_time
                checkpoint_time = time.time()
                speed, eta = self.compute_eta(
                    i, n, last_checkpoint_time, checkpoint_time)
                eta_min, eta_sec = divmod(eta, 60)
                self.logger.info(
                    "injected %d users at %d users/s, eta: %dm %ds", i, speed,
                    eta_min, eta_sec)

    def inject_groups(self, n=None):
        # TODO: factorize the common code with inject_users
        self.check_branch("groups")
        if n is None:
            n = self.nb_groups
        self.logger.info("about to inject %d groups", n)
        checkpoint_time = time.time()
        for i in xrange(1, n + 1):
            self.inject_group()
            if i % 100 == 0:
                last_checkpoint_time = checkpoint_time
                checkpoint_time = time.time()
                speed, eta = self.compute_eta(
                    i, n, last_checkpoint_time, checkpoint_time)
                eta_min, eta_sec = divmod(eta, 60)
                self.logger.info(
                    "injected %d groups at %d groups/s, eta: %dm %ds", i, speed,
                    eta_min, eta_sec)

    def random_name(self):
        length = self._rng.choice(range(5, 12))
        letters = []
        for i in range(length):
            if i % 2:
                letters.append(self._rng.choice(CONSUMNS))
            else:
                letters.append(self._rng.choice(VOYELS))
        return "".join(letters)

    def user_dn(self, uid):
        return "uid=%s,%s" % (uid, self.users_branch_dn)

    def group_dn(self, cn):
        return "cn=%s,%s" % (cn, self.groups_branch_dn)

    def check_branch(self, ou):
        dn = "ou=%s,%s" % (ou, self.base_dn)

        try:
            self._conn.search_s(dn, ldap.SCOPE_BASE)
            self.logger.info("branch %s already exists" % dn)
        except ldap.NO_SUCH_OBJECT:
            branch = {
                'ou': ou,
                'objectClass': ['top', 'organizationalUnit'],
            }
            modlist = ldap.modlist.addModlist(branch)
            self._conn.add_s(dn, modlist)
            self.logger.info("created branch %s" % dn)

    def inject_user(self):
        uid = None
        while uid is None or uid in self._generated_users:
            firstname = self.random_name()
            lastname = self.random_name()
            uid = "%s%s" % (firstname[0], lastname)

        dn = self.user_dn(uid)
        firstname = firstname.capitalize()
        lastname = lastname.capitalize()
        user = {
            "objectClass": ["top", "person", "inetOrgPerson"],
            "uid": uid,
            "userPassword": uid,
            "cn": "%s %s" % (firstname, lastname),
            "givenName": firstname,
            "sn": lastname,
            "mail": uid + "@example.com",
        }
        modlist = ldap.modlist.addModlist(user)
        try:
            self._conn.add_s(dn, modlist)
            self.logger.debug("injected user %s %s with dn %s " %(
                firstname, lastname, dn))
        except ldap.ALREADY_EXISTS:
            self.logger.warn("ignore already existing user %s" % uid)
        self._generated_users.add(uid)

    def inject_group(self):
        cn = None
        while cn is None or cn in self._generated_groups:
            cn = self.random_name().capitalize()

        dn = self.group_dn(cn)

        # number of users in this group
        nb_users = self._rng.randrange(self.max_users_in_group)
        nb_users = min(len(self._generated_users), nb_users)

        # number of subgroups in this group
        nb_groups = self._rng.randrange(self.max_subgroups_in_group)
        nb_groups = min(len(self._generated_groups), nb_groups)

        group = {
            "objectClass": ["top", "groupOfUniqueNames"],
            "cn": cn,
            "uniqueMember": (
                [self.user_dn(uid) for uid in
                 self._rng.sample(self._generated_users, nb_users)]
                + [self.group_dn(cn2) for cn2 in
                  self._rng.sample(self._generated_groups, nb_groups)]
                or ["cn=empty_group_marker"])
        }
        modlist = ldap.modlist.addModlist(group)
        try:
            self._conn.add_s(dn, modlist)
            self.logger.debug("injected group %s with %d users at dn %s " %(
                cn, nb_users, dn))
        except ldap.ALREADY_EXISTS:
            self.logger.warn("ignore already existing group %s" % cn)
        self._generated_groups.add(cn)


if __name__ == "__main__":
    # TODO use optparse to select the number of users/groups to inject + the
    # server parameters and readpass for the bind dn

    if not os.path.exists(LOGGING_CONFIGURATION):
        with file(LOGGING_CONFIGURATION, 'wb') as f:
            f.write(LOGGING_CONFIGURATION_CONTENT)
    logging.config.fileConfig(LOGGING_CONFIGURATION)

    with closing(Loader(LDAP_SERVER, LDAP_PORT, BIND_DN, BIND_PASSWORD)) as loader:
        loader.inject_users()
        loader.inject_groups()
