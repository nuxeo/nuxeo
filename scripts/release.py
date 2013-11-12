#!/usr/bin/env python
# #
# # (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
# #
# # All rights reserved. This program and the accompanying materials
# # are made available under the terms of the GNU Lesser General Public License
# # (LGPL) version 2.1 which accompanies this distribution, and is available at
# # http://www.gnu.org/licenses/lgpl.html
# #
# # This library is distributed in the hope that it will be useful,
# # but WITHOUT ANY WARRANTY; without even the implied warranty of
# # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# # Lesser General Public License for more details.
# #
# # Contributors:
# #     Julien Carsique
# #
# # This script manages releasing of Nuxeo source code.
# # It applies on current Git repository and its sub-repositories.
# #
# # Examples from Nuxeo root, checkout on master:
# # $ ./scripts/release.py
# # Releasing from branch:   master
# # Current version:         5.6-SNAPSHOT
# # Tag:                     5.6-I20120102_1741
# # Next version:            5.6-SNAPSHOT
# # No maintenance branch
# #
# # $ ./scripts/release.py -f
# # Releasing from branch:   master
# # Current version:         5.6-SNAPSHOT
# # Tag:                     5.6
# # Next version:            5.7-SNAPSHOT
# # No maintenance branch
# #
# # $ ./scripts/release.py -f -m 5.6.1-SNAPSHOT
# # Releasing from branch:   master
# # Current version:         5.6-SNAPSHOT
# # Tag:                     5.6
# # Next version:            5.7-SNAPSHOT
# # Maintenance version:     5.6.1-SNAPSHOT
# #
# # $ ./scripts/release.py -f -b 5.5.0
# # Releasing from branch:   5.5.0
# # Current version:         5.5.0-HF01-SNAPSHOT
# # Tag:                     5.5.0-HF01
# # Next version:            5.5.0-HF02-SNAPSHOT
# # No maintenance branch
# #
# # $ ./scripts/release.py -b 5.5.0 -t sometag
# # Releasing from branch 5.5.0
# # Current version:      5.5.0-HF01-SNAPSHOT
# # Tag:                  sometag
# # Next version:         5.5.0-HF01-SNAPSHOT
# # No maintenance branch
# #
from IndentedHelpFormatterWithNL import IndentedHelpFormatterWithNL
from collections import namedtuple
from datetime import datetime
from distutils.log import Log
from lxml import etree
from nxutils import ExitException
from nxutils import Repository
from nxutils import assert_git_config
from nxutils import check_output
from nxutils import extract_zip
from nxutils import log
from nxutils import make_zip
from nxutils import system
from optparse import IndentedHelpFormatter
from optparse import TitledHelpFormatter
from terminalsize import get_terminal_size
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
    "nuxeo-distribution-jboss-%s-nuxeo-cap.zip": "nuxeo-cap-%s-jboss",
    "nuxeo-distribution/nuxeo-distribution-jboss/target/"
    "nuxeo-distribution-jboss-%s-nuxeo-cap-ear.zip": "nuxeo-cap-%s-jboss-ear",
    # Tomcat packages
    "nuxeo-distribution/nuxeo-distribution-tomcat/target/"
    "nuxeo-distribution-tomcat-%s-coreserver.zip":
    "nuxeo-coreserver-%s-tomcat",
    "nuxeo-distribution/nuxeo-distribution-tomcat/target/"
    "nuxeo-distribution-tomcat-%s-nuxeo-cap.zip": "nuxeo-cap-%s-tomcat"
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


def etree_parse(file):
    """XML parsing with context error logging"""
    try:
        tree = etree.parse(file)
    except etree.XMLSyntaxError as e:
        raise ExitException(1, "XML syntax error on '%s'\n%s" %
                            (file, e.message))
    return tree


class Release(object):
    """Nuxeo release manager.

    See 'self.perpare()', 'self.perform()'."""
    def __init__(self, repo, branch, tag, next_snapshot, maintenance_version="auto",
                 is_final=False, skipTests=False, other_versions=None,
                 profiles='', msg_commit='', msg_tag=''):
        self.repo = repo
        self.branch = branch
        self.is_final = is_final
        self.maintenance_version = maintenance_version
        self.skipTests = skipTests
        if profiles:
            self.profiles = ',' + profiles
        else:
            self.profiles = profiles
        self.msg_commit = msg_commit
        self.msg_tag = msg_tag
        # Evaluate default values, if not provided
        self.set_other_versions_and_patterns(other_versions)
        self.set_snapshot()
        self.set_tag(tag)
        self.set_next_snapshot(next_snapshot)
        self.maintenance_branch = self.tag
        if self.branch == self.maintenance_branch:
            self.maintenance_branch += ".0"
        # Detect if working on Nuxeo main sources
        tree = etree_parse(os.path.join(self.repo.basedir, "pom.xml"))
        artifact_id = tree.getroot().find("pom:artifactId", namespaces)
        self.repo.is_nuxeoecm = "nuxeo-ecm" == artifact_id.text
        if self.repo.is_nuxeoecm:
            log("Working on Nuxeo main repository...")
        else:
            log("Working on custom repository...")

    def set_other_versions_and_patterns(self, other_versions=None):
        """Set other versions and replacement patterns"""
        Patterns = namedtuple('Patterns', ['files', 'props'])
        self.default_patterns = Patterns(
            # Files extentions
            "^.*\\.(xml|properties|txt|defaults|sh|html|nxftl)$",
            # Properties like nuxeo.*.version
            "{%s}(nuxeo|marketplace)\..*version" % namespaces.get("pom"))
        custom_files_pattern = ""
        custom_props_pattern = ""
        other_versions_split = []
        if other_versions:
            # Parse custom patterns
            other_versions_split = other_versions.split(':')
            if (len(other_versions_split) == 3):
                if other_versions_split[0]:
                    custom_files_pattern = other_versions_split[0]
                    try:
                        re.compile(custom_files_pattern)
                    except re.error, e:
                        raise ExitException(1, "Bad pattern: '%s'\n%s" %
                                            (custom_files_pattern, e.message))
                if other_versions_split[1]:
                    try:
                        re.compile(other_versions_split[1])
                    except re.error, e:
                        raise ExitException(e, 1, "Bad pattern: '%s'\n/s" %
                                            other_versions_split[1], e.message)
                    custom_props_pattern = "{%s}%s" % (namespaces.get("pom"),
                                                       other_versions_split[1])
                other_versions = other_versions_split[2]
            elif len(other_versions_split) == 1:
                other_versions = other_versions_split[0]
            else:
                raise ExitException(1, "Could not parse other_versions parameter %s."
                                    % other_versions)
            # Parse version replacements
            other_versions_split = []
            for other_version in other_versions.split(","):
                other_version_split = other_version.split("/")
                if (len(other_version_split) < 2 or
                    len(other_version_split) > 3 or
                    other_version_split.count(None) > 0 or
                    other_version_split.count("") > 0):
                    raise ExitException(1,
                        "Could not parse other_versions parameter %s."
                        % other_versions)
                other_versions_split.append(other_version_split)
        self.other_versions = other_versions_split
        self.custom_patterns = Patterns(custom_files_pattern,
                                        custom_props_pattern)

    def set_snapshot(self):
        """Set current version from root POM."""
        tree = etree_parse(os.path.join(self.repo.basedir, "pom.xml"))
        version_elem = tree.getroot().find("pom:version", namespaces)
        if version_elem is None:
            version_elem = tree.getroot().find("pom:parent/pom:version",
                                               namespaces)
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
        log("Tag:".ljust(25) + "release-" + self.tag)
        log("Next version:".ljust(25) + self.next_snapshot)
        if self.maintenance_version == "auto":
            log("Maintenance branch deleted".ljust(25))
        else:
            log("Maintenance branch:".ljust(25) + self.maintenance_branch)
            log("Maintenance version:".ljust(25) + self.maintenance_version)
        if self.skipTests:
            log("Tests execution is skipped")
        if self.custom_patterns.files:
            log("Custom files pattern: ".ljust(25) +
                self.custom_patterns.files)
        if self.custom_patterns.props:
            log("Custom props pattern: ".ljust(25) +
                self.custom_patterns.props[35:])
        if self.other_versions:
            for other_version in self.other_versions:
                log("Also replace version:".ljust(25) + '/'.join(other_version))
        if store_params:
            release_log = os.path.abspath(os.path.join(self.repo.basedir,
                                                       os.pardir,
                                                       "release.log"))
            with open(release_log, "wb") as f:
                f.write("REMOTE=%s\nBRANCH=%s\nTAG=%s\nNEXT_SNAPSHOT=%s\n"
                        "MAINTENANCE=%s\nFINAL=%s\nSKIP_TESTS=%s\n"
                        "PROFILES=%s\nOTHER_VERSIONS=%s\nFILES_PATTERN=%s\n"
                        "PROPS_PATTERN=%s\nMSG_COMMIT=%s\nMSG_TAG=%s\n" %
                        (self.repo.alias, self.branch, self.tag,
                         self.next_snapshot, self.maintenance_version, self.is_final,
                         self.skipTests, self.profiles,
                         (','.join('/'.join(other_version)
                                   for other_version in self.other_versions)),
                         self.custom_patterns.files,
                         self.custom_patterns.props[35:],
                         self.msg_commit, self.msg_tag))
            log("Parameters stored in %s" % release_log)
        log("")

    def update_versions(self, old_version, new_version):
        """Update all occurrences of 'old_version' with 'new_version'.
        Return True if there was some change."""
        changed = False
        if old_version == new_version:
            return changed
        log("[INFO] Replacing occurrences of %s with %s" % (old_version,
                                                            new_version))
        if self.custom_patterns.files:
            files_pattern = re.compile("(%s)|(%s)" % (self.default_patterns.files,
                                 self.custom_patterns.files))
        else:
            files_pattern = re.compile(self.default_patterns.files)
        if self.custom_patterns.props:
            props_pattern = re.compile("(%s)|(%s)" % (self.default_patterns.props,
                                 self.custom_patterns.props))
        else:
            props_pattern = re.compile(self.default_patterns.props)

        for root, dirs, files in os.walk(os.getcwd(), True, None, True):
            for dir in set(dirs) & set([".git", "target"]):
                dirs.remove(dir)
            for name in files:
                replaced = False
                if fnmatch.fnmatch(name, "pom*.xml"):
                    tree = etree_parse(os.path.join(root, name))
                    # Parent POM version
                    parent = tree.getroot().find("pom:parent", namespaces)
                    if parent is not None:
                        elem = parent.find("pom:version", namespaces)
                        if elem is not None and elem.text == old_version:
                            elem.text = new_version
                            replaced = True
                    # POM version
                    elem = tree.getroot().find("pom:version", namespaces)
                    if elem is not None and elem.text == old_version:
                        elem.text = new_version
                        replaced = True
                    properties = tree.getroot().find("pom:properties",
                                                     namespaces)
                    if properties is not None:
                        for property in properties.getchildren():
                            if (not isinstance(property, etree._Comment)
                                and props_pattern.match(property.tag)
                                and property.text == old_version):
                                property.text = new_version
                                replaced = True
                    tree.write_c14n(os.path.join(root, name))
                elif files_pattern.match(name):
                    with open(os.path.join(root, name), "rb") as f:
                        content = f.read()
                        if content.find(old_version) > -1:
                            replaced = True
                        content = content.replace(old_version, new_version)
                    with open(os.path.join(root, name), "wb") as f:
                        f.write(content)
                if replaced:
                    log("Edited '%s'" % os.path.join(root, name))
                    changed = True
        return changed

    def test(self):
        """For current script development purpose."""
        self.prepare(dryrun=True)
#         self.package_all(self.snapshot)

    def package_all(self, version=None):
        """Repackage files to be uploaded.

        'version': version to package; defaults to the current tag (without the
        'release-' prefix."""
        if not self.repo.is_nuxeoecm:
            log("Skip packaging step (not a main Nuxeo repository).")
            return
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

    def prepare(self, dodeploy=False, dryrun=False):
        """ Prepare the release: build, change versions, tag and package source
        and distributions."""
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch)

        log("[INFO] Check release-ability...")
        self.check()

        log("\n[INFO] Releasing branch {0}, create maintenance branch {1}, "
            "update versions, commit and tag as release-{2}..."
            .format(self.branch, self.maintenance_branch, self.tag))
        msg_commit = "Release %s, update %s to %s" % (self.branch, self.snapshot,
                                                      self.tag)
        self.repo.system_recurse("git checkout -b %s" % self.maintenance_branch)
        self.update_versions(self.snapshot, self.tag)
        for other_version in self.other_versions:
            if len(other_version) > 0:
                self.update_versions(other_version[0], other_version[1])
                msg_commit += ", update %s to %s" % (other_version[0],
                                                     other_version[1])
        self.repo.system_recurse("git commit -m'%s %s' -a" % (self.msg_commit,
                                                              msg_commit))
        msg_tag = "Release release-%s from %s on %s" % (self.tag, self.snapshot,
                                                        self.branch)
        self.repo.system_recurse("git tag -a release-%s -m'%s %s'" % (self.tag,
                                                        self.msg_tag, msg_tag))

        # TODO NXP-8569 Optionally merge maintenance branch on source
        if self.maintenance_version != "auto":
            # Maintenance branches are kept, so update their versions
            log("\n[INFO] Maintenance branch (update version and commit)...")
            msg_commit = "Update %s to %s" % (self.tag, self.maintenance_version)
            self.update_versions(self.tag, self.maintenance_version)
            self.repo.system_recurse("git commit -m'%s' -a" % msg_commit)

        log("\n[INFO] Released branch %s (update version and commit)..." %
            self.branch)
        self.repo.system_recurse("git checkout %s" % self.branch)
        # Update released branches with next versions
        post_release_change = self.update_versions(self.snapshot, self.next_snapshot)
        msg_commit = "Post release %s." % self.branch
        if post_release_change:
            msg_commit = " Update %s to %s" % (self.snapshot, self.next_snapshot)
        for other_version in self.other_versions:
            if (len(other_version) == 3 and
                self.update_versions(other_version[0], other_version[2])):
                post_release_change = True
                msg_commit += ", update %s to %s" % (other_version[0],
                                                     other_version[2])
        if post_release_change:
            self.repo.system_recurse("git commit -m'%s' -a" % msg_commit)

        if self.maintenance_version == "auto":
            log("\n[INFO] Delete maintenance branch %s..." %
                self.maintenance_branch)
            self.repo.system_recurse("git branch -D %s" %
                                     self.maintenance_branch)

        log("\n[INFO] Build and package release-%s..." % self.tag)
        self.repo.system_recurse("git checkout release-%s" % self.tag)
        if dryrun:
            return
        if dodeploy:
            self.repo.mvn("clean deploy", skip_tests=self.skipTests,
                        profiles="release,-qa,nightly" + self.profiles)
        else:
            self.repo.mvn("clean install", skip_tests=self.skipTests,
                        profiles="release,-qa" + self.profiles)
        self.package_all()
        # TODO NXP-8571 package sources
        os.chdir(cwd)

    def maintenance(self):
        """ Create the maintenance branch starting from a release tag."""
        log("Tag:".ljust(25) + "release-" + self.tag)
        log("Current version:".ljust(25) + self.snapshot)
        log("Maintenance branch:".ljust(25) + self.branch)
        log("Maintenance version:".ljust(25) + self.maintenance_version)
        if self.custom_patterns.files:
            log("Custom files pattern: ".ljust(25) +
                self.custom_patterns.files)
        if self.custom_patterns.props:
            log("Custom props pattern: ".ljust(25) +
                self.custom_patterns.props[35:])
        if self.other_versions:
            for other_version in self.other_versions:
                log("Also replace version:".ljust(25) + '/'.join(other_version))
        log("")

        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.system_recurse("git checkout release-%s" % self.tag)

        log("[INFO] Check release-ability...")
        self.check()

        log("\n[INFO] Creating maintenance branch {0} from {1}, update versions "
            "and commit...".format(self.branch, self.tag))
        self.repo.system_recurse("git checkout -b %s" % self.branch)
        msg_commit = "Update %s to %s" % (self.tag, self.maintenance_version)
        self.update_versions(self.tag, self.maintenance_version)
        for other_version in self.other_versions:
            if len(other_version) > 0:
                self.update_versions(other_version[0], other_version[1])
                msg_commit += ", update %s to %s" % (other_version[0],
                                                     other_version[1])
        self.repo.system_recurse("git commit -m'%s' -a" % msg_commit)
        os.chdir(cwd)

    def perform(self):
        """ Perform the release: push source, deploy artifacts and upload
        packages."""
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch)
        self.repo.system_recurse("git push %s %s" % (self.repo.alias,
                                                     self.branch))
        if self.maintenance_version != "auto":
            self.repo.system_recurse("git push %s %s" % (self.repo.alias,
                                                         self.maintenance_branch))
        self.repo.system_recurse("git push --tags")
        self.repo.system_recurse("git checkout release-%s" % self.tag)
        self.repo.mvn("clean deploy", skip_tests=True,
                        profiles="release,-qa" + self.profiles)
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
        usage = ("""usage: %prog <command> [options]
       %prog prepare [-r alias] [-f] [-d] [--skipTests] [-p profiles] \
[-b branch] [-t tag] [-n next_snapshot] [-m maintenance] \
[--arv versions_replacements] [--mc msg_commit] [--mt msg_tag]
       %prog perform [-r alias] [-f] [-p profiles] [-b branch] [-t tag] [-m maintenance]
       %prog package [--skipTests] [-p profiles]
       %prog maintenance [-r alias] [-b branch] [-t tag] [-m maintenance] [--arv versions_replacements]
\nCommands:
  prepare: Prepare the release (build, change versions, tag and package source and distributions). The release  parameters are stored in a release.log file.
  perform: Perform the release (push sources, deploy artifacts and upload packages, tests are always skipped). If no parameter is given, they are read from the release.log file.
  package: Package distributions and source code in the archives directory.
  maintenance: Create a maintenance branch from an existing tag.""")
        description = """Release Nuxeo from a given branch, tag the release, then
set the next SNAPSHOT version. If a maintenance version was provided, then a
maintenance branch is kept, else it is deleted after release."""
        help_formatter = IndentedHelpFormatterWithNL(
#                 max_help_position=6,
                 width=get_terminal_size()[0])
        parser = optparse.OptionParser(usage=usage, description=description,
                                       formatter=help_formatter)
        parser.add_option('-r', action="store", type="string",
                          dest='remote_alias',
                          default='origin',
                          help="""The Git alias of remote URL.
Default: '%default'""")
        parser.add_option('-i', '--interactive', action="store_true",
                          dest='interactive', default=False,
                          help="""Not implemented (TODO NXP-8573). Interactive
mode. Default: '%default'""")
        parser.add_option('-d', '--deploy', action="store_true",
                          dest='deploy', default=False,
                          help="""Deploy artifacts to nightly/staging repository
by activating the 'nightly' Maven profile. Default: '%default'""")
        parser.add_option('--skipTests', action="store_true",
                          dest='skipTests', default=False,
                          help="""Skip tests execution (but compile them).
Default: '%default'""")
        parser.add_option('-p', '--profiles', action="store", type="string",
                          dest='profiles',
                          default='',
                          help="""Additional Maven profiles.
Default: '%default'.
Those profiles are also always activated (unless deactivated by that parameter):\n
 - 'addons,distrib,all-distributions' for all commands\n
 - 'release,-qa' for prepare command (plus 'nightly' if deploy option passed),\n
 - 'release,-qa' for perform command,\n
 - 'qa' for package command.
""")
        versioning_options = optparse.OptionGroup(parser, 'Version policy')
        versioning_options.add_option('-f', '--final', action="store_true",
            dest='is_final', default=False,
            help="Is it a final release? Default: '%default'")
        versioning_options.add_option("-b", "--branch", action="store",
            type="string", dest="branch", default="auto",
            help="""Branch to release. Default: '%default' = the current branch""")
        versioning_options.add_option("-t", "--tag", action="store",
            type="string", dest="tag", default="auto",
            help="""Released version. SCM tag is 'release-$TAG'.
Default: '%default'\n
In mode 'auto', if final option is True, then the default value is the current
version minus '-SNAPSHOT', else the 'SNAPSHOT' keyword is replaced with a date
(aka 'date-based release').""")
        versioning_options.add_option("-n", "--next", action="store",
            type="string", dest="next_snapshot", default="auto",
            help="""Version post-release. Default: '%default'\n
In mode 'auto', if final option is True, then the next snapshot is the current
one increased, else it is equal to the current.""")
        versioning_options.add_option('-m', '--maintenance', action="store",
            dest='maintenance_version', default="auto",
            help="""Maintenance version. Default: '%default'\n
The maintenance branch is always named like the tag without the 'release-'
prefix. If set, the version will be used on the maintenance branch, else, in
mode 'auto', the maintenance branch is deleted after release.""")
        versioning_options.add_option('--arv', '--also-replace-version',
            action="store", dest='other_versions', default=None,
            help="""Other version(s) to replace. Default: '%default'\n
Use a slash ('/') as a separator between old and new version: '1.0-SNAPSHOT/1.0'.\n
A version post-release can also be specified: '1.0-SNAPSHOT/1.0/1.0.1-SNAPSHOT'.\n
Multiple versions can be replaced using a coma (',') separator:
'1.0-SNAPSHOT/1.0/1.0.1-SNAPSHOT,0.0-SNAPSHOT/2.0.1'.\n
It only applies on files named with xml, properties, txt, defaults, sh, html or
nxftl extension and on 'pom.xml' files. On POM files, it only applies on the
parent version, the POM version and the properties named
'nuxeo|marketplace.*version'. Other patterns for filename extensions and
properties can be provided using two colon (':') separators:
'.*\\.customextension:my.property:1.0-SNAPSHOT/1.0', '.*\\.text::',
':my.property:1.0-SNAPSHOT/1.0/1.1-SNAPSHOT', ...
Those patterns are common to all replacements, including the released version.\n
Default files and properties patterns are respectively:
'^.*\\.(xml|properties|txt|defaults|sh|html|nxftl)$' and
'(nuxeo|marketplace)\..*version'. They can't be removed.""")
        versioning_options.add_option('--mc', '--msg-commit', action="store",
            type="string", dest='msg_commit', default='',
            help="""Additional release commit message.
Default: 'Release $BRANCH, update $SNAPSHOT to $TAG, update ...'.\n
Post-release commits' messages are not customizable.
""")
        versioning_options.add_option('--mt', '--msg-tag', action="store",
            type="string", dest='msg_tag', default='',
            help="""Additional tag message.
Default: 'Release release-$TAG from $SNAPSHOT on $BRANCH'.
""")
        parser.add_option_group(versioning_options)
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
                options.maintenance_version = f.readline().split("=")[1].strip()
                options.is_final = f.readline().split("=")[1].strip() == "True"
                options.skipTests = f.readline().split("=")[1].strip() == "True"
                options.profiles = f.readline().split("=")[1].strip()
                other_versions = f.readline().split("=")[1].strip()
                files_pattern = f.readline().split("=")[1].strip()
                props_pattern = f.readline().split("=")[1].strip()
                options.other_versions = ':'.join((files_pattern, props_pattern,
                                                   other_versions))
                options.msg_commit = f.readline().split("=")[1].strip()
                options.msg_tag = f.readline().split("=")[1].strip()
            if options.other_versions == "::":
                options.other_versions = None

        repo = Repository(os.getcwd(), options.remote_alias)
        system("git fetch %s" % (options.remote_alias))
        if "command" in locals() and command == "maintenance":
            if options.tag == "auto":
                options.tag = repo.get_current_version()[8:]
            if options.tag == "":
                raise ExitException(1, "Couldn't guess tag name from %s" %
                                    repo.get_current_version())
            if options.branch == "auto":
                options.branch = options.tag
            repo.git_update("release-%s" % options.tag)
            options.is_final = True
            if options.maintenance_version == "auto":
                options.maintenance_version = options.tag + ".1-SNAPSHOT"
            options.next_snapshot = None
        else:
            if options.branch == "auto":
                options.branch = repo.get_current_version()
            repo.git_update(options.branch)
        release = Release(repo, options.branch, options.tag,
                          options.next_snapshot, options.maintenance_version,
                          options.is_final, options.skipTests,
                          options.other_versions, options.profiles,
                          options.msg_commit, options.msg_tag)
        if "command" not in locals() or command != "maintenance":
            release.log_summary("command" in locals() and command != "perform")
        if "command" not in locals():
            raise ExitException(1, "Missing command. See usage with '-h'.")
        elif command == "prepare":
            release.prepare(options.deploy)
        elif command == "maintenance":
            release.maintenance()
        elif command == "perform":
            release.perform()
        elif command == "package":
            repo.clone()
            # workaround for NXBT-121: use install instead of package
            if options.profiles:
                repo.mvn("clean install", skip_tests=options.skipTests,
                         profiles="qa," + options.profiles)
            else:
                repo.mvn("clean install", skip_tests=options.skipTests,
                         profiles="qa")
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
