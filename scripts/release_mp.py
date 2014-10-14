#!/usr/bin/env python
"""
(C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Julien Carsique

This script manages releasing of Nuxeo Marketplace packages."""

import fnmatch
import optparse
import os
import sys
import inspect
from release import Release
import traceback

from IndentedHelpFormatterWithNL import IndentedHelpFormatterWithNL
from nxutils import ExitException, Repository, assert_git_config, log, \
    system, DEFAULT_MP_CONF_URL
from terminalsize import get_terminal_size


CONNECT_TEST_URL = "https://connect-test.nuxeo.com/nuxeo"
CONNECT_PROD_URL = "https://connect.nuxeo.com/nuxeo"


# pylint: disable=R0902
class ReleaseMP(object):
    """Nuxeo MP release manager.

    See 'self.perpare()', 'self.perform()'."""
    # pylint: disable=R0913
    def __init__(self, alias, restart_from, marketplace_conf=None):
        self.alias = alias
        self.restart_from = restart_from
        if marketplace_conf == '':
            marketplace_conf = DEFAULT_MP_CONF_URL
        self.marketplace_conf = marketplace_conf
        cwd = os.getcwd()
        if os.path.isdir(os.path.join(cwd, "marketplace")):
            pass
        elif os.path.split(cwd)[1] == "marketplace":
            cwd = os.path.abspath(os.path.join(cwd, os.pardir))
        else:
            if '__file__' not in locals():
                __file__ = inspect.getframeinfo(inspect.currentframe())[0]
            cwd = os.path.dirname(os.path.abspath(__file__))
            cwd = os.path.abspath(os.path.join(cwd, os.pardir))
        log("Nuxeo source location: %s" % cwd)
        self.repo = Repository(cwd, self.alias)
        self.mp_config = self.repo.get_mp_config(self.marketplace_conf)

    def clone(self):
        cwd = os.getcwd()
        self.repo.clone_mp(self.marketplace_conf)
        os.chdir(cwd)

    # pylint: disable=E1103
    def prepare(self, dryrun=False):
        """ Prepare the release."""
        cwd = os.getcwd()
        if not os.path.isdir(self.repo.mp_dir):
            self.clone()
        os.chdir(self.repo.mp_dir)
        marketplaces = self.mp_config.sections()
        marketplaces_skipped = []
        if self.restart_from:
            idx = marketplaces.index(self.restart_from)
            marketplaces = marketplaces[idx:]
        for marketplace in marketplaces:
            log("")
            if self.mp_config.has_option(marketplace, "skip"):
                log("[%s]" % marketplace)
                log("[WARN] Skipped '%s' (%s)" % (marketplace,
                                    self.mp_config.get(marketplace, "skip")))
                marketplaces_skipped.append(marketplace)
                upgrade_only = True
            else:
                upgrade_only = False
            if self.mp_config.getboolean(marketplace, "prepared"):
                log("[%s]" % marketplace)
                log("Skipped '%s' (%s)" % (marketplace, "Already prepared"))
                continue
            try:
                mp_dir = os.path.join(self.repo.mp_dir, marketplace)
                if not os.path.isdir(mp_dir):
                    os.chdir(self.repo.mp_dir)
                    self.repo.git_pull(marketplace,
                                    self.mp_config.get(marketplace, "branch"))
                else:
                    log("[%s]" % marketplace)
                os.chdir(mp_dir)
                mp_repo = Repository(os.getcwd(), self.alias)
                if upgrade_only:
                    log("Upgrade skipped %s..." % marketplace)
                else:
                    log("Prepare release of %s..." % marketplace)
                mp_release = Release(mp_repo,
                        self.mp_config.get(marketplace, "branch"),
                        self.mp_config.get(marketplace, "tag"),
                        self.mp_config.get(marketplace, "next_snapshot"),
                        self.mp_config.get(marketplace, "maintenance_version"),
                        is_final=True, skipTests=False,
                        other_versions=self.mp_config.get(
                                                marketplace, "other_versions",
                                                None))
                mp_release.log_summary()
                mp_release.prepare(dryrun=dryrun, upgrade_only=upgrade_only)
                prepared = True
            except Exception, e:
                stack = traceback.format_exc()
                if hasattr(e, 'message') and e.message is not None:
                    stack = e.message + "\n" + stack
                log("[ERROR] %s" % stack)
                prepared = False
                stack = stack.replace("%", "%%")
                self.mp_config.set(marketplace, "skip", "Failed! %s" % stack)
            self.mp_config.set(marketplace, "prepared", str(prepared))
            self.repo.save_mp_config(self.mp_config)
            if prepared and not upgrade_only:
                # Upload on Connect test
                for dirpath, _, filenames in os.walk(mp_repo.basedir):
                    for name in filenames:
                        path = os.path.join(dirpath, name)
                        if (os.path.isfile(path) and
                            fnmatch.fnmatch(path[len(mp_repo.basedir) + 1:],
                            self.mp_config.get(marketplace, "mp_to_upload"))):
                            self.upload(CONNECT_TEST_URL, path, dryrun=dryrun)
                            self.mp_config.set(marketplace, "uploaded",
                                               CONNECT_TEST_URL + ": " + path)
                            self.repo.save_mp_config(self.mp_config)
        os.chdir(cwd)

    # pylint: disable=R0914,C0103
    def perform(self, dryrun=False):
        """ Perform the release: push source, deploy artifacts and upload
        packages."""
        cwd = os.getcwd()
        marketplaces = self.mp_config.sections()
        marketplaces_skipped = []
        if self.restart_from:
            idx = marketplaces.index(self.restart_from)
            marketplaces = marketplaces[idx:]
        for marketplace in marketplaces:
            log("")
            if self.mp_config.has_option(marketplace, "skip"):
                log("[WARN] Skipped '%s' (%s)" % (marketplace,
                                    self.mp_config.get(marketplace, "skip")))
                marketplaces_skipped.append(marketplace)
                upgrade_only = True
            else:
                upgrade_only = False
            if not self.mp_config.getboolean(marketplace, "prepared"):
                log("[WARN] Skipped '%s' (%s)" % (marketplace, "Not prepared"))
                continue
            if self.mp_config.getboolean(marketplace, "performed"):
                log("Skipped '%s' (%s)" % (marketplace, "Already performed"))
                continue
            try:
                if upgrade_only:
                    log("Upgrade skipped %s..." % marketplace)
                else:
                    log("Perform %s" % marketplace)
                os.chdir(os.path.join(self.repo.mp_dir, marketplace))
                mp_repo = Repository(os.getcwd(), self.alias)
                # Perform release
                (_, branch, tag, next_snapshot, maintenance_version, is_final,
                 skipTests, _, other_versions,
                 _, _) = Release.read_release_log(mp_repo.basedir)
                mp_release = Release(mp_repo, branch, tag, next_snapshot,
                                     maintenance_version, is_final=is_final,
                                     skipTests=skipTests,
                                     other_versions=other_versions)
                mp_release.perform(dryrun=dryrun, upgrade_only=upgrade_only)
                performed = True
            except Exception, e:
                stack = traceback.format_exc()
                if hasattr(e, 'message') and e.message is not None:
                    stack = e.message + "\n" + stack
                log("[ERROR] %s" % stack)
                performed = False
                stack = stack.replace("%", "%%")
                self.mp_config.set(marketplace, "skip", "Failed! %s" % stack)
            self.mp_config.set(marketplace, "performed", str(performed))
            self.repo.save_mp_config(self.mp_config)
            if performed and not upgrade_only:
                # Upload on Connect
                for dirpath, _, filenames in os.walk(mp_repo.basedir):
                    for name in filenames:
                        path = os.path.join(dirpath, name)
                        if (os.path.isfile(path) and
                            fnmatch.fnmatch(path[len(mp_repo.basedir) + 1:],
                            self.mp_config.get(marketplace, "mp_to_upload"))):
                            self.upload(CONNECT_PROD_URL, path, dryrun=dryrun)
                            self.mp_config.set(marketplace, "uploaded",
                                               CONNECT_PROD_URL + ": " + path)
                            self.repo.save_mp_config(self.mp_config)
        os.chdir(cwd)

    def upload(self, url, mp_file, dryrun=False):
        """ Upload the given mp_file on the given Connect URL."""
        cmd = ("curl -i -n -F package=@%s %s%s"
               % (mp_file, url, "/site/marketplace/upload?batch=true"))
        system(cmd, run=(not dryrun))

    def test(self):
        """For current script development purpose."""
        self.prepare(dryrun=True)
#         cwd = os.getcwd()
#         self.repo = Repository(os.getcwd(), self.alias)
#         os.chdir(os.path.join(self.repo.mp_dir, "marketplace-agenda"))
#         for dirpath, _, filenames in os.walk(os.getcwd()):
#             for name in filenames:
#                 path = os.path.join(dirpath, name)
#                 log(path[len(os.getcwd()):])
#                 if (os.path.isfile(path) and
#                     fnmatch.fnmatch(path[len(os.getcwd()) + 1:],
#                             "marketplace/target/marketplace*.zip")):
#                     log('self.upload(%s, %s)' % (CONNECT_TEST_URL, path))
#         os.chdir(cwd)


# pylint: disable=R0912,R0914,R0915
def main():
    assert_git_config()

    try:
        usage = ("""usage: %prog <command> [options]
       %prog clone [-r alias] [-m URL]
       %prog prepare [-r alias] [-m URL] [--rf package] [--dryrun]
       %prog perform [-r alias] [-m URL] [--rf package] [--dryrun]""")
        description = """Release Nuxeo Marketplace packages.\n
The first call must provide an URL for the configuration. If set to '' (empty
string), it defaults to '%s'. You can use a local file URL ('file://').\n
Then, a 'release.ini' file is generated and will be reused for the next calls.
For each package, a 'release-$PACKAGE_NAME.log' file is also generated and
corresponds to the file generated by the release.py script.\n
The 'release.ini' file contains informations about the release process:\n
- 'prepared = True' if the prepare task succeeded,\n
- 'performed = True' if the perform task succeeded,\n
- 'uploaded = ...' if an upload successfully happened,\n
- 'skip = Failed!' followed by a stack trace in case of error.\n
The script can be re-called: it will skip the packages with a skip value and
skip the prepare (or perform) if 'prepared = True' (or 'performed = True').\n
Failed uploads are not retried and must be manually done.
""" % DEFAULT_MP_CONF_URL
        help_formatter = IndentedHelpFormatterWithNL(
#                 max_help_position=6,
                 width=get_terminal_size()[0])
        parser = optparse.OptionParser(usage=usage, description=description,
                                       formatter=help_formatter)
        parser.add_option('-r', action="store", type="string",
                          dest='remote_alias', default='origin',
                          help="""The Git alias of remote URL.
Default: '%default'""")
        parser.add_option('-m', "--marketplace-conf", action="store",
                          type="string", dest='marketplace_conf',
                    default=None,
                          help="""The Marketplace configuration URL.
Default: '%default'""")
        parser.add_option('-i', '--interactive', action="store_true",
                          dest='interactive', default=False,
                          help="""Not implemented (TODO NXP-8573). Interactive
mode. Default: '%default'""")
        parser.add_option('--rf', '--restart-from', action="store",
                          dest='restart_from', default=None,
                          help="""Restart from a package.
 Default: '%default'""")
        parser.add_option('--dryrun', action="store_true",
                          dest='dryrun', default=False,
                          help="""Dry run mode. Default: '%default'""")
        (options, args) = parser.parse_args()
        if len(args) == 1:
            command = args[0]
        elif len(args) > 1:
            raise ExitException(1, "'command' must be a single argument. "
                                "See usage with '-h'.")
        full_release = ReleaseMP(options.remote_alias, options.restart_from,
                                 options.marketplace_conf)
        if "command" not in locals():
            raise ExitException(1, "Missing command. See usage with '-h'.")
        elif command == "clone":
            full_release.clone()
        elif command == "prepare":
            full_release.prepare(dryrun=options.dryrun)
        elif command == "perform":
            full_release.perform(dryrun=options.dryrun)
        elif command == "test":
            full_release.test()
        else:
            raise ExitException(1, "Unknown command! See usage with '-h'.")
    except ExitException, e:
        if e.message is not None:
            log("[ERROR] %s" % e.message, sys.stderr)
        sys.exit(e.return_code)

if __name__ == '__main__':
    main()
