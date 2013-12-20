#!/usr/bin/env python
##
## (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl-2.1.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Contributors:
##     Julien Carsique
##
## This script manages releasing of Nuxeo Marketplace packages.
##
import fnmatch
import optparse
import os
import sys
from release import Release
import traceback

from IndentedHelpFormatterWithNL import IndentedHelpFormatterWithNL
from nxutils import ExitException, Repository, assert_git_config, log, system
from terminalsize import get_terminal_size


CONNECT_TEST_URL = "https://connect-test.nuxeo.com/nuxeo"
CONNECT_PROD_URL = "https://connect.nuxeo.com/nuxeo"


#pylint: disable=R0902
class ReleaseMP(object):
    """Nuxeo MP release manager.

    See 'self.perpare()', 'self.perform()'."""
    #pylint: disable=R0913
    def __init__(self, alias, restart_from, marketplace_conf=None):
        self.alias = alias
        self.restart_from = restart_from
        self.marketplace_conf = marketplace_conf
        self.repo = Repository(os.getcwd(), self.alias)
        self.mp_config = self.repo.get_mp_config(self.marketplace_conf)

    def clone(self):
        cwd = os.getcwd()
        self.repo = Repository(os.getcwd(), self.alias)
        self.repo.clone_mp(self.marketplace_conf)
        os.chdir(cwd)

    #pylint: disable=E1103
    def prepare(self, dryrun=False):
        """ Prepare the release."""
        cwd = os.getcwd()
        marketplaces = self.mp_config.sections()
        if self.restart_from:
            idx = marketplaces.index(self.restart_from)
            marketplaces = marketplaces[idx:]
        for marketplace in marketplaces:
            if self.mp_config.has_option(marketplace, "skip"):
                log("[WARN] Skipped '%s' (%s)" % (marketplace,
                                    self.mp_config.get(marketplace, "skip")))
                continue
            if self.mp_config.getboolean(marketplace, "prepared"):
                log("Skipped '%s' (%s)" % (marketplace, "Already prepared"))
                continue
            try:
                log("Prepare %s" % marketplace)
                os.chdir(os.path.join(self.repo.mp_dir, marketplace))
                mp_repo = Repository(os.getcwd(), self.alias)
                # Prepare release
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
                mp_release.prepare(dryrun=dryrun)
                prepared = True
            except:
                stack = traceback.format_exc()
                log("[ERROR] %s" % stack)
                prepared = False
                self.mp_config.set(marketplace, "skip", "Failed! %s" % stack)
            self.mp_config.set(marketplace, "prepared", prepared)
            self.repo.save_mp_config(self.mp_config)
            if (prepared):
                # Upload on Connect test
                for dirpath, _, filenames in os.walk(mp_repo.basedir):
                    for name in filenames:
                        path = os.path.join(dirpath, name)
                        if (os.path.isfile(path) and
                            fnmatch.fnmatch(path[len(mp_repo.basedir) + 1:],
                            self.mp_config.get(marketplace, "mp_to_upload"))):
                            self.upload(CONNECT_TEST_URL, path)
                            self.mp_config.set(marketplace, "uploaded",
                                               CONNECT_TEST_URL + ": " + path)
                            self.repo.save_mp_config(self.mp_config)
        os.chdir(cwd)

    #pylint: disable=R0914,C0103
    def perform(self):
        """ Perform the release: push source, deploy artifacts and upload
        packages."""
        cwd = os.getcwd()
        marketplaces = self.mp_config.sections()
        if self.restart_from:
            idx = marketplaces.index(self.restart_from)
            marketplaces = marketplaces[idx:]
        for marketplace in marketplaces:
            if self.mp_config.has_option(marketplace, "skip"):
                log("[WARN] Skipped '%s' (%s)" % (marketplace,
                                    self.mp_config.get(marketplace, "skip")))
                continue
            if not self.mp_config.getboolean(marketplace, "prepared"):
                log("[WARN] Skipped '%s' (%s)" % (marketplace, "Not prepared"))
                continue
            if self.mp_config.getboolean(marketplace, "performed"):
                log("Skipped '%s' (%s)" % (marketplace, "Already performed"))
                continue
            try:
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
                mp_release.perform()
                performed = True
            except:
                stack = traceback.format_exc()
                log("[ERROR] %s" % stack)
                performed = False
                self.mp_config.set(marketplace, "skip", "Failed! %s" % stack)
            self.mp_config.set(marketplace, "performed", performed)
            self.repo.save_mp_config(self.mp_config)
            if performed:
                # Upload on Connect
                for dirpath, _, filenames in os.walk(mp_repo.basedir):
                    for name in filenames:
                        path = os.path.join(dirpath, name)
                        if (os.path.isfile(path) and
                            fnmatch.fnmatch(path[len(mp_repo.basedir) + 1:],
                            self.mp_config.get(marketplace, "mp_to_upload"))):
                            self.upload(CONNECT_PROD_URL, path)
                            self.mp_config.set(marketplace, "uploaded",
                                               CONNECT_PROD_URL + ": " + path)
                            self.repo.save_mp_config(self.mp_config)
        os.chdir(cwd)

    def upload(self, url, mp_file):
        """ Upload the given mp_file on the given Connect URL."""
        cmd = ("curl -i -n -F package=@%s %s%s"
               % (mp_file, url, "/site/marketplace/upload?batch=true"))
        system(cmd)

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


#pylint: disable=R0912,R0914,R0915
def main():
    assert_git_config()

    try:
        usage = ("""usage: %prog <command> [options]
       %prog clone [-r alias] [-m URL]
       %prog prepare [-r alias] [-m URL] [--rf package]
       %prog perform [-r alias] [-m URL] [--rf package]""")
        description = """Release Nuxeo Marketplace packages.\n
The first call must provide an URL for the configuration (such as
https://gist.github.com/jcarsique/8040138/raw/). You can use a local file with
the 'file://' format.\n
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
Failed uploads are not retried and must be manually done."""
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
            full_release.prepare()
        elif command == "perform":
            full_release.perform()
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
