#!/usr/bin/env python
##
## (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Contributors:
##     Julien Carsique
##
## This script manages releasing of Nuxeo source code.
## It applies on current Git repository and its sub-repositories.
##
## Examples from Nuxeo root, checkout on master:
## $ ./scripts/release.py
## Releasing from branch:   master
## Current version:         5.6-SNAPSHOT
## Tag:                     5.6-I20120102_1741
## Next version:            5.6-SNAPSHOT
## No maintenance branch
##
## $ ./scripts/release.py -f
## Releasing from branch:   master
## Current version:         5.6-SNAPSHOT
## Tag:                     5.6
## Next version:            5.7-SNAPSHOT
## No maintenance branch
##
## $ ./scripts/release.py -f -m 5.6.1-SNAPSHOT
## Releasing from branch:   master
## Current version:         5.6-SNAPSHOT
## Tag:                     5.6
## Next version:            5.7-SNAPSHOT
## Maintenance version:     5.6.1-SNAPSHOT
##
## $ ./scripts/release.py -f -b 5.5.0
## Releasing from branch:   5.5.0
## Current version:         5.5.0-HF01-SNAPSHOT
## Tag:                     5.5.0-HF01
## Next version:            5.5.0-HF02-SNAPSHOT
## No maintenance branch
##
## $ ./scripts/release.py -b 5.5.0 -t sometag
## Releasing from branch 5.5.0
## Current version:      5.5.0-HF01-SNAPSHOT
## Tag:                  sometag
## Next version:         5.5.0-HF01-SNAPSHOT
## No maintenance branch
##
from datetime import datetime
from lxml import etree
from nxutils import ExitException
from nxutils import Repository
from nxutils import assert_git_config
from nxutils import check_output
from nxutils import extract_zip
from nxutils import log
from nxutils import make_zip
from nxutils import system
import fnmatch
import hashlib
import optparse
import os
import posixpath
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import time
import urllib


PKG_RENAMINGS = {
    # JBoss packages
    "nuxeo-distribution/nuxeo-distribution-jboss/target/"
    "nuxeo-distribution-jboss-%s-nuxeo-dm.zip": "nuxeo-dm-%s-jboss",
    # Tomcat packages
    "nuxeo-distribution/nuxeo-distribution-tomcat/target/"
    "nuxeo-distribution-tomcat-%s-coreserver.zip":
    "nuxeo-coreserver-%s-tomcat",
    "nuxeo-distribution/nuxeo-distribution-tomcat/target/"
    "nuxeo-distribution-tomcat-%s-nuxeo-dm.zip": "nuxeo-dm-%s-tomcat"
}

PKG_RENAMINGS_OPTIONALS = {
    # Tomcat packages
    "nuxeo-distribution/nuxeo-distribution-tomcat/target/"
    "nuxeo-distribution-tomcat-%s-coreserver-sdk.zip":
    "nuxeo-coreserver-%s-tomcat-sdk",
    "nuxeo-distribution/nuxeo-distribution-tomcat/target/"
    "nuxeo-distribution-tomcat-%s-nuxeo-cap-sdk.zip":
    "nuxeo-cap-%s-tomcat-sdk"
}

MP_RENAMINGS = {
}


class Release(object):
    """Nuxeo release manager.

    See 'self.perpare()', 'self.perform()'."""
    def __init__(self, repo, branch, tag, next_snapshot, maintenance="auto",
                 is_final=False):
        self.repo = repo
        self.branch = branch
        self.is_final = is_final
        self.maintenance = maintenance
        # Evaluate default values, if not provided
        self.set_snapshot()
        self.set_tag(tag)
        self.set_next_snapshot(next_snapshot)

    def set_snapshot(self):
        """Set current version from root POM."""
        tree = etree.parse(os.path.join(self.repo.basedir, "pom.xml"))
        version_elem = tree.getroot().find("pom:version", namespaces)
        self.snapshot = version_elem.text

    def set_tag(self, tag="auto"):
        """Return calculated tag. Requires 'self.snapshot' being set."""
        if tag != "auto":
            self.tag = tag
        elif self.is_final:
            self.tag = self.snapshot.partition("-SNAPSHOT")[0]
        else:
            date = datetime.now().strftime("%Y%m%d_%H%M")
            self.tag = self.snapshot.replace("-SNAPSHOT", "-I" + date)

    def set_next_snapshot(self, next_snapshot="auto"):
        """Return calculated next snapshot. Requires 'self.snapshot' being set.
        """
        if next_snapshot != "auto":
            self.next_snapshot = next_snapshot
        elif self.is_final:
            snapshot_split = re.match("(^.*)(\d+)(-SNAPSHOT$)", self.snapshot)
            self.next_snapshot = (snapshot_split.group(1)
                    + str(int(snapshot_split.group(2)) + 1)  # increment minor
                    + snapshot_split.group(3))
        else:
            self.next_snapshot = self.snapshot

    def log_summary(self, store_params=True):
        """Log summary of configuration for current release."""
        log("Releasing from branch:".ljust(25) + self.branch)
        log("Current version:".ljust(25) + self.snapshot)
        log("Tag:".ljust(25) + self.tag)
        log("Next version:".ljust(25) + self.next_snapshot)
        if self.maintenance == "auto":
            log("No maintenance branch".ljust(25))
        else:
            log("Maintenance version:".ljust(25) + self.maintenance)
        if store_params:
            release_log = os.path.abspath(os.path.join(self.repo.basedir, os.pardir,
                                                   "release.log"))
            with open(release_log, "wb") as f:
                f.write("REMOTE=%s\nBRANCH=%s\nTAG=%s\nNEXT_SNAPSHOT=%s\n"
                        "MAINTENANCE=%s\nFINAL=%s" %
                        (self.repo.alias, self.branch, self.tag,
                         self.next_snapshot, self.maintenance, self.is_final))
            log("Parameters stored in %s" % release_log)
        log("")

    def update_versions(self, old_version, new_version):
        """Update all occurrences of 'old_version' with 'new_version'."""
        log("Replacing occurrences of %s with %s" % (old_version, new_version))
        pattern = re.compile("^.*\\.(xml|properties|txt|defaults|sh|html|nxftl)$")
        for root, dirs, files in os.walk(os.getcwd(), True, None, True):
            for dir in set(dirs) & set([".git", "target"]):
                dirs.remove(dir)
            for name in files:
                if fnmatch.fnmatch(name, "pom*.xml"):
                    log(os.path.join(root, name))
                    tree = etree.parse(os.path.join(root, name))
                    # Parent POM version
                    parent = tree.getroot().find("pom:parent", namespaces)
                    if parent is not None:
                        elem = parent.find("pom:version", namespaces)
                        if elem is not None and elem.text == old_version:
                            elem.text = new_version
                    # POM version
                    elem = tree.getroot().find("pom:version", namespaces)
                    if elem is not None and elem.text == old_version:
                        elem.text = new_version
                    # Properties like nuxeo.*.version
                    prop_pattern = re.compile("{" + namespaces.get("pom") +
                                              "}nuxeo\..*version")
                    properties = tree.getroot().find("pom:properties",
                                                     namespaces)
                    if properties is not None:
                        for property in properties.getchildren():
                            if (not isinstance(property, etree._Comment)
                                and prop_pattern.match(property.tag)
                                and property.text == old_version):
                                property.text = new_version
                    tree.write_c14n(os.path.join(root, name))
                elif pattern.match(name):
                    log(os.path.join(root, name))
                    with open(os.path.join(root, name), "rb") as f:
                        content = f.read()
                        content = content.replace(old_version, new_version)
                    with open(os.path.join(root, name), "wb") as f:
                        f.write(content)

    def test(self):
        """For current script development purpose."""
        self.package_all(self.snapshot)

    def package_all(self, version=None):
        """Repackage files to be uploaded.

        'version': version to package; defaults to the current tag (without the
        'release-' prefix."""
        self.archive_dir = os.path.abspath(os.path.join(self.repo.basedir,
                                                   os.pardir, "archives"))
        if os.path.isdir(self.archive_dir):
            shutil.rmtree(self.archive_dir)
        os.mkdir(self.archive_dir)
        self.tmpdir = tempfile.mkdtemp()

        if version is None:
            version = self.tag

        # Tomcat and JBoss packages
        for old, new in PKG_RENAMINGS.items():
            self.package(old % version, new % version)
        # Tomcat SDK packages
        for old, new in PKG_RENAMINGS_OPTIONALS.items():
            self.package(old % version, new % version, False)

        self.package_sources(version)
        shutil.rmtree(self.tmpdir)

    def package(self, old_archive, new_name, failonerror=True):
        """Repackage a ZIP following the rules:
            - have a parent directory with the same name as the archive name
            - set executable bit on scripts in bin/
            - activate the setup wizard

        If 'failonerror', raise an ExitException in case of missing file."""
        if not os.path.isfile(old_archive):
            if failonerror:
                raise ExitException(1, "Could not find %s" % old_archive)
            else:
                log("[WARN] Could not find %s" % old_archive, sys.stderr)
                return
        new_archive = os.path.join(self.archive_dir, new_name + ".zip")
        extract_zip(old_archive, os.path.join(self.tmpdir, new_name))
        log("Packaging %s ..." % new_archive)
        cwd = os.getcwd()
        os.chdir(os.path.join(self.tmpdir, new_name))
        ls = os.listdir(os.curdir)
        if len(ls) == 1:
            if ls[0] != new_name:
                shutil.move(ls[0], new_name)
        else:
            os.mkdir(new_name)
            for file in ls:
                shutil.move(file, os.path.join(new_name, file))

        files = os.listdir(os.path.join(new_name, "bin"))
        for filename in (fnmatch.filter(files, "*ctl") +
                        fnmatch.filter(files, "*.sh") +
                        fnmatch.filter(files, "*.command")):
            os.chmod(os.path.join(new_name, "bin", filename), 0744)
        with open(os.path.join(new_name, "bin", "nuxeo.conf"), "a") as f:
            f.write("nuxeo.wizard.done=false\n")
        make_zip(os.path.join(self.archive_dir, new_name + ".zip"),
                            os.getcwd(), new_name)
        os.chdir(cwd)
        # Cleanup temporary directory
        shutil.rmtree(os.path.join(self.tmpdir, new_name))

    def package_sources(self, version):
        sources_archive_name = "nuxeo-%s-sources.zip" % version
        self.repo.archive(os.path.join(self.archive_dir, sources_archive_name))

    def prepare(self, dodeploy):
        """ Prepare the release: build, change versions, tag and package source
        and distributions."""
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch)

        # check release-ability
        self.check()

        # Create release branches, update version, commit and tag
        self.repo.system_recurse("git checkout -b %s" % self.tag)
        self.update_versions(self.snapshot, self.tag)
        self.repo.system_recurse("git commit -m'Release %s' -a" % self.tag)
        self.repo.system_recurse("git tag release-%s" % self.tag)

        ## TODO NXP-8569 Optionally merge maintenance branch on source

        if self.maintenance != "auto":
            # Maintenance branches are kept, so update their versions
            self.update_versions(self.tag, self.maintenance)
            self.repo.system_recurse("git commit -m'Post release %s' -a" %
                                     self.tag)

        self.repo.system_recurse("git checkout %s" % self.branch)
        if self.snapshot != self.next_snapshot:
            # Update released branches
            self.update_versions(self.snapshot, self.next_snapshot)
            self.repo.system_recurse("git commit -m'Post release %s' -a" %
                                     self.tag)

        if self.maintenance == "auto":
            # Delete maintenance branches
            self.repo.system_recurse("git branch -D %s" % self.tag)

        # Build and package
        self.repo.system_recurse("git checkout release-%s" % self.tag)
        if dodeploy == True:
            self.repo.mvn("clean deploy", skip_tests=True,
                        profiles="release,-qa,nightly")
        else:
            self.repo.mvn("clean install", skip_tests=True,
                        profiles="release,-qa")
        self.package_all()
        # TODO NXP-8571 package sources
        os.chdir(cwd)

    def perform(self):
        """ Perform the release: push source, deploy artifacts and upload
        packages."""
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch)
        self.repo.system_recurse("git push %s %s" % (self.repo.alias,
                                                     self.branch))
        if self.maintenance != "auto":
            self.repo.system_recurse("git push %s %s" % (self.repo.alias,
                                                         self.tag))
        self.repo.system_recurse("git push --tags")
        self.repo.system_recurse("git checkout release-%s" % self.tag)
        self.repo.mvn("clean deploy", skip_tests=True,
                        profiles="release,-qa")
        os.chdir(cwd)

    def check(self):
        """ Check the release is feasible"""
        # TODO NXP-8573 tag and release branch do not already exist
        # TODO NXP-8573 all POMs have a namespace


def main():
    global namespaces
    assert_git_config()
    namespaces = {"pom": "http://maven.apache.org/POM/4.0.0"}
    etree.register_namespace('pom', 'http://maven.apache.org/POM/4.0.0')

    try:
        if not os.path.isdir(".git"):
            raise ExitException(1, "That script must be ran from root of a Git"
                                + " repository")
        usage = ("usage: %prog [options] <command>\n\nCommands:\n"
                 "  prepare: Prepare the release (build, change versions, tag "
                 "and package source and distributions). The release "
                 "parameters are stored in a release.log file.\n"
                 "  perform: Perform the release (push sources, deploy "
                 "artifacts and upload packages). If no parameter is given, "
                 "they are read from the release.log file.\n"
                 "  package: Package distributions and source code in the "
                 "archives directory.")
        parser = optparse.OptionParser(usage=usage,
                                       description="""Release Nuxeo from
a given branch, tag the release, then set the next SNAPSHOT version.  If a
maintenance version was provided, then a maintenance branch is kept, else it is
deleted after release.""")
        parser.add_option('-r', action="store", type="string",
                          dest='remote_alias',
                          default='origin',
                          help="""the Git alias of remote URL
                          (default: %default)""")
        parser.add_option('-f', '--final', action="store_true",
                          dest='is_final', default=False,
                          help='is it a final release? (default: %default)')
        parser.add_option("-b", "--branch", action="store", type="string",
                          help='branch to release (default: current branch)',
                          dest="branch", default="auto")
        parser.add_option("-t", "--tag", action="store", type="string",
                          dest="tag", default="auto",
                          help="""if final option is True, then the default tag
is the current version minus '-SNAPSHOT', else the 'SNAPSHOT' keyword is
replaced with a date (aka 'date-based release')""")
        parser.add_option("-n", "--next", action="store", type="string",
                          dest="next_snapshot", default="auto",
                          help="""next snapshot. If final option is True, then
the next snapshot is the current one increased, else it is equal to the current
""")
        parser.add_option('-m', '--maintenance', action="store",
                          dest='maintenance', default="auto",
                          help="""maintenance version (by default, the
maintenance branch is deleted after release)""")
        parser.add_option('-i', '--interactive', action="store_true",
                          dest='interactive', default=False,
                          help="""Not implemented (TODO NXP-8573). Interactive
mode.""")
        parser.add_option('-d', '--deploy', action="store_true",
                          dest='deploy', default=False,
                          help="""deploy artifacts to nightly repository""")
        (options, args) = parser.parse_args()
        if len(args) == 1:
            command = args[0]
        elif len(args) > 1:
            raise ExitException(1, "'command' must be a single argument. "
                                "See usage with '-h'.")

        release_log = os.path.abspath(os.path.join(os.getcwd(), os.pardir,
                                               "release.log"))
        if ("command" in locals() and command == "perform"
            and os.path.isfile(release_log)
            and options == parser.get_default_values()):
            log("Reading parameters from %s ..." % release_log)
            with open(release_log, "rb") as f:
                options.remote_alias = f.readline().split("=")[1].strip()
                options.branch = f.readline().split("=")[1].strip()
                options.tag = f.readline().split("=")[1].strip()
                options.next_snapshot = f.readline().split("=")[1].strip()
                options.maintenance = f.readline().split("=")[1].strip()
                options.is_final = f.readline().split("=")[1].strip() == "True"

        repo = Repository(os.getcwd(), options.remote_alias)
        if options.branch == "auto":
            options.branch = repo.get_current_version()
        system("git fetch %s" % (options.remote_alias))
        repo.git_update(options.branch)
        release = Release(repo, options.branch, options.tag,
                          options.next_snapshot, options.maintenance,
                          options.is_final)
        release.log_summary("command" in locals() and command != "perform")
        if "command" not in locals():
            raise ExitException(1, "Missing command. See usage with '-h'.")
        elif command == "prepare":
            release.prepare(options.deploy)
        elif command == "perform":
            release.perform()
        elif command == "package":
            repo.clone()
            # workaround for NXBT-121: use install instead of package
            repo.mvn("clean install", skip_tests=True, profiles="qa")
            release.package_all(release.snapshot)
        elif command == "test":
            release.test()
        else:
            raise ExitException(1, "Unknown command! See usage with '-h'.")
    except ExitException, e:
        if e.message is not None:
            log("[ERROR] %s" % e.message, sys.stderr)
        sys.exit(e.return_code)
    finally:
        if "repo" in locals():
            repo.cleanup()

if __name__ == '__main__':
    main()
