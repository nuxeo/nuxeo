#!/usr/bin/env python
"""
(C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    Julien Carsique

This script manages releasing of Nuxeo Marketplace packages."""

import glob
import inspect
import optparse
import os
import sys
import traceback

from IndentedHelpFormatterWithNL import IndentedHelpFormatterWithNL
from nxutils import ExitException, Repository, assert_git_config, log, system, DEFAULT_MP_CONF_URL
from release import Release, ReleaseInfo
from terminalsize import get_terminal_size


CONNECT_TEST_URL = "https://nos-preprod-connect.nuxeocloud.com/nuxeo"
CONNECT_PROD_URL = "https://connect.nuxeo.com/nuxeo"


# pylint: disable=R0902
class ReleaseMP(object):
    """Nuxeo MP release manager.

    See 'self.perpare()', 'self.perform()'."""
    # pylint: disable=R0913
    def __init__(self, alias, restart_from, default_conf=None, marketplace_conf=None):
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
                __file__ = inspect.getframeinfo(inspect.currentframe())[0]  # @ReservedAssignment
            cwd = os.path.dirname(os.path.abspath(__file__))
            cwd = os.path.abspath(os.path.join(cwd, os.pardir))
        log("Nuxeo source location: %s" % cwd)
        self.repo = Repository(cwd, self.alias)
        self.defaults = {}
        if default_conf:
            default_info = ReleaseInfo()
            default_info.read_release_log(default_conf)
            prefix = default_info.module
            for key, value in vars(default_info).iteritems():
                self.defaults[prefix + "-" + key] = str(value)
        self.mp_config = self.repo.get_mp_config(self.marketplace_conf, self.defaults)

    def clone(self):
        cwd = os.getcwd()
        self.repo.clone_mp(self.marketplace_conf)
        os.chdir(cwd)

    # pylint: disable=E1103

    def get_packages_list(self):
        """Return the list of packages to work on."""
        marketplaces = self.mp_config.sections()
        if self.restart_from:
            idx = marketplaces.index(self.restart_from)
            marketplaces = marketplaces[idx:]
        return marketplaces

    def prepare(self, dryrun=False):
        """ Prepare the release."""
        cwd = os.getcwd()
        if not os.path.isdir(self.repo.mp_dir):
            self.clone()
        os.chdir(self.repo.mp_dir)
        marketplaces_skipped = []
        for marketplace in self.get_packages_list():
            log("")
            if self.mp_config.has_option(marketplace, "skip"):
                log("[%s]" % marketplace)
                log("[WARN] Skipped '%s' (%s)" % (marketplace, self.mp_config.get(marketplace, "skip")))
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
                    self.repo.git_pull(marketplace, self.mp_config.get(marketplace, "branch"))
                else:
                    log("[%s]" % marketplace)
                os.chdir(mp_dir)
                mp_repo = Repository(os.getcwd(), self.alias)
                if upgrade_only:
                    log("Upgrade skipped %s..." % marketplace)
                else:
                    log("Prepare release of %s..." % marketplace)

                release_info = ReleaseInfo(module=marketplace, remote_alias=self.alias,
                                           branch=self.mp_config.get(marketplace, "branch"),
                                           tag=self.mp_config.get(marketplace, "tag"),
                                           next_snapshot=self.mp_config.get(marketplace, "next_snapshot"),
                                           maintenance_version=self.mp_config.get(marketplace, "maintenance_version"),
                                           is_final=self.mp_config.getboolean(marketplace, "is_final"),
                                           skip_tests=self.mp_config.getboolean(marketplace, "skipTests"),
                                           skip_its=self.mp_config.getboolean(marketplace, "skipITs"),
                                           profiles=self.mp_config.get(marketplace, "profiles"),
                                           other_versions=self.mp_config.get(marketplace, "other_versions"),
                                           #files_pattern, props_pattern, msg_commit, msg_tag,
                                           auto_increment_policy=self.mp_config.get(marketplace,
                                                                                    "auto_increment_policy"),
                                           dryrun=dryrun)
                mp_release = Release(mp_repo, release_info)
                release_log = mp_release.log_summary()
                release_info.read_release_log(release_log)
                if dryrun:
                    print "DEBUG -- init %s with:" % marketplace
                for key, value in vars(release_info).iteritems():
                    if dryrun:
                        print "DEBUG: %s-%s=%s" % (marketplace, key, value)
                    self.mp_config.set("DEFAULT", marketplace + "-" + key, str(value))
                if dryrun:
                    print

                mp_release.prepare(dryrun=dryrun, upgrade_only=upgrade_only, dodeploy=True)
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
                owner = None
                if self.mp_config.has_option(marketplace, "owner"):
                    owner = self.mp_config.get(marketplace, "owner")
                self.upload(CONNECT_TEST_URL, marketplace, dryrun=dryrun, owner=owner)
        os.chdir(cwd)

    def release_branch(self, dryrun=False):
        """ Create the release branch."""
        cwd = os.getcwd()
        if not os.path.isdir(self.repo.mp_dir):
            self.clone()
        os.chdir(self.repo.mp_dir)
        marketplaces_skipped = []
        for marketplace in self.get_packages_list():
            log("")
            if self.mp_config.has_option(marketplace, "skip"):
                log("[%s]" % marketplace)
                log("[WARN] Skipped '%s' (%s)" % (marketplace, self.mp_config.get(marketplace, "skip")))
                marketplaces_skipped.append(marketplace)
                upgrade_only = True
            else:
                upgrade_only = False
            if self.mp_config.getboolean(marketplace, "branched"):
                log("[%s]" % marketplace)
                log("Skipped '%s' (%s)" % (marketplace, "Already branched"))
                continue
            try:
                mp_dir = os.path.join(self.repo.mp_dir, marketplace)
                if not os.path.isdir(mp_dir):
                    os.chdir(self.repo.mp_dir)
                    self.repo.git_pull(marketplace, self.mp_config.get(marketplace, "branch"))
                else:
                    log("[%s]" % marketplace)
                os.chdir(mp_dir)
                mp_repo = Repository(os.getcwd(), self.alias)
                if upgrade_only:
                    log("Upgrade skipped %s..." % marketplace)
                else:
                    log("Prepare release of %s..." % marketplace)
                release_info = ReleaseInfo(module=marketplace, remote_alias=self.alias,
                                           branch=self.mp_config.get(marketplace, "branch"),
                                           tag=self.mp_config.get(marketplace, "tag"),
                                           next_snapshot=self.mp_config.get(marketplace, "next_snapshot"),
                                           maintenance_version=self.mp_config.get(marketplace, "maintenance_version"),
                                           is_final=self.mp_config.getboolean(marketplace, "is_final"),
                                           skip_tests=self.mp_config.getboolean(marketplace, "skipTests"),
                                           skip_its=self.mp_config.getboolean(marketplace, "skipITs"),
                                           profiles=self.mp_config.get(marketplace, "profiles"),
                                           other_versions=self.mp_config.get(marketplace, "other_versions"),
                                           #files_pattern, props_pattern, msg_commit, msg_tag,
                                           auto_increment_policy=self.mp_config.get(marketplace,
                                                                                    "auto_increment_policy"),
                                           dryrun=dryrun)
                mp_release = Release(mp_repo, release_info)
                release_log = mp_release.log_summary()
                release_info.read_release_log(release_log)
                if dryrun:
                    print "DEBUG -- init %s with:" % marketplace
                for key, value in vars(release_info).iteritems():
                    if dryrun:
                        print "DEBUG: %s-%s=%s" % (marketplace, key, value)
                    self.mp_config.set("DEFAULT", marketplace + "-" + key, str(value))
                if dryrun:
                    print

                mp_release.release_branch(dryrun=dryrun, upgrade_only=upgrade_only)
                self.mp_config.set(marketplace, "next_snapshot", "done")
                self.mp_config.set(marketplace, "branch", mp_release.maintenance_branch)
                branched = True
            except Exception, e:
                stack = traceback.format_exc()
                if hasattr(e, 'message') and e.message is not None:
                    stack = e.message + "\n" + stack
                log("[ERROR] %s" % stack)
                branched = False
                stack = stack.replace("%", "%%")
                self.mp_config.set(marketplace, "skip", "Failed! %s" % stack)
            self.mp_config.set(marketplace, "branched", str(branched))
            self.repo.save_mp_config(self.mp_config)
        os.chdir(cwd)

    # pylint: disable=R0914,C0103
    def perform(self, dryrun=False):
        """ Perform the release: push source, deploy artifacts and upload
        packages."""
        cwd = os.getcwd()
        marketplaces_skipped = []
        for marketplace in self.get_packages_list():
            log("")
            if self.mp_config.has_option(marketplace, "skip"):
                log("[WARN] Skipped '%s' (%s)" % (marketplace, self.mp_config.get(marketplace, "skip")))
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
                release_info = ReleaseInfo()
                release_info.read_release_log(ReleaseInfo.get_release_log(mp_repo.basedir))
                mp_release = Release(mp_repo, release_info)
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
                owner = None
                if self.mp_config.has_option(marketplace, "owner"):
                    owner = self.mp_config.get(marketplace, "owner")
                self.upload(CONNECT_PROD_URL, marketplace, dryrun=dryrun, owner=owner)
        os.chdir(cwd)

    def is_list(tab_mp_ulpoad):
      """ Check if instance of list """
      if isinstance(tab_mp, (list, tuple)):
        return(1);
      else:
        raise AssertionError(tab_mp_upload + " is not an instance of list")

    def upload(self, url, marketplace, dryrun=False, owner=None):
        """ Upload the given Marketplace package and update the config file."""
        uploaded = [url + ":"]
        mp_to_upload = self.mp_config.get(marketplace, "mp_to_upload")
        mp_to_upload = mp_to_upload.replace(" ", "")
        tab_mp_upload = mp_to_upload.split(',')
        if (is_list(tab_mp_upload)):
          for mp in tab_mp_upload:
            for pkg in glob.glob(mp):
                if os.path.isfile(pkg):
                    retcode = self.upload_file(url, pkg, dryrun=dryrun, owner=owner)
                    if retcode == 0:
                        uploaded.append(os.path.realpath(pkg))
        if len(uploaded) > 1:
            self.mp_config.set(marketplace, "uploaded", " ".join(uploaded))
            self.repo.save_mp_config(self.mp_config)

    def upload_file(self, url, mp_file, dryrun=False, owner=None):
        """ Upload the given mp_file on the given Connect URL."""
        cmd = "curl -i -n -F package=@%s %s%s" % (mp_file, url, "/site/marketplace/upload?batch=true")
        if owner:
            cmd += "&owner=%s" % (owner,)
        return system(cmd, failonerror=False, run=(not dryrun))

    def test(self):
        """For current script development purpose."""
        self.prepare(dryrun=True)


# pylint: disable=R0912,R0914,R0915
def main():
    assert_git_config()

    try:
        usage = ("""usage: %prog <command> [options]
       %prog clone [-r alias] [-m URL] [-d PATH]
       %prog branch [-r alias] [-m URL] [-d PATH] [--rf package] [--dryrun]
       %prog prepare [-r alias] [-m URL] [-d PATH] [--rf package] [--dryrun]
       %prog perform [-r alias] [-m URL] [-d PATH] [-o owner] [--rf package] [--dryrun]
\nCommands:
       clone: Clone or update Nuxeo Package repositories.
       branch: Create the release branch so that the branch to release is freed for ongoing development. Following \
'prepare' or 'perform' commands must use option '--next=done'. If kept, that branch will become the maintenance \
branch after release.
       prepare: Prepare the release (build, change versions, tag and package source and distributions). The release \
parameters are stored in release-<package name>.log files generated by the release.py script. \
The first call must provide a Nuxeo Packages configuration URL (option '-m') from which a 'release.ini' file is \
generated and will be reused for the next calls.
       perform: Perform the release (push sources, deploy artifacts and upload packages, tests are always skipped). \
If no parameter is given, they are read from the 'release.ini' file.""")
        description = """Release Nuxeo Packages.\n
You can initiate some parameters with a release log file (option '-d').\n
The 'release.ini' file contains informations about the release process:\n
- 'prepared = True' if the prepare task succeeded,\n
- 'performed = True' if the perform task succeeded,\n
- 'uploaded = ...' if an upload successfully happened,\n
- 'skip = Failed!' followed by a stack trace in case of error.\n
The script can be re-called: it will skip the packages with a skip value and skip the prepare (or perform) if
'prepared = True' (or 'performed = True').\n
Failed uploads are not retried and must be manually done."""
        help_formatter = IndentedHelpFormatterWithNL(max_help_position=7, width=get_terminal_size()[0])
        parser = optparse.OptionParser(usage=usage, description=description, formatter=help_formatter)
        parser.add_option('-r', action="store", type="string", dest='remote_alias', default='origin',
                          help="""The Git alias of remote URL. Default: '%default'""")
        parser.add_option('-d', "--default", action="store", type="string", dest='default_conf',
                          default=None, help="""The default configuration file (usually '/path/to/release-nuxeo.log').
Default: '%default'""")
        parser.add_option('-m', "--marketplace-conf", action="store", type="string", dest='marketplace_conf',
                          default=None, help="""The Nuxeo Packages configuration URL (usually named 'marketplace.ini').
You can use a local file URL ('file://').\n
If set to '' (empty string), then it will default to '""" + DEFAULT_MP_CONF_URL + """'. Default: '%default'""")
        parser.add_option('-o', "--owner", action="store", type="string", dest='owner',
                          default=None, help="""The Nuxeo Package owner, if the package is private.
This is the id of the connect client document on Connect. Sample value: 45a78af-7f83-44b2-79e1-f102abf7e435.""")
        parser.add_option('-i', '--interactive', action="store_true", dest='interactive', default=False,
                          help="""Not implemented (TODO NXP-8573). Interactive mode. Default: '%default'""")
        parser.add_option('--rf', '--restart-from', action="store", dest='restart_from', default=None,
                          help="""Restart from a package. Default: '%default'""")
        parser.add_option('--dryrun', action="store_true", dest='dryrun', default=False,
                          help="""Dry run mode. Default: '%default'""")
        (options, args) = parser.parse_args()
        if len(args) == 1:
            command = args[0]
        elif len(args) > 1:
            raise ExitException(1, "'command' must be a single argument. See usage with '-h'.")
        full_release = ReleaseMP(options.remote_alias, options.restart_from, options.default_conf,
                                 options.marketplace_conf)
        if "command" not in locals():
            raise ExitException(1, "Missing command. See usage with '-h'.")
        elif command == "clone":
            full_release.clone()
        elif command == "branch":
            full_release.release_branch(dryrun=options.dryrun)
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
