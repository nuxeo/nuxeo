#!/usr/bin/env python
"""
(C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and contributors.

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

This script manages releasing of Nuxeo source code.
It applies on current Git repository and its sub-repositories.

Examples from Nuxeo root, checkout on master:
$ ./scripts/release.py
Releasing from branch:   master
Current version:         5.6-SNAPSHOT
Tag:                     5.6-I20120102_1741
Next version:            5.6-SNAPSHOT
No maintenance branch

$ ./scripts/release.py -f
Releasing from branch:   master
Current version:         5.6-SNAPSHOT
Tag:                     5.6
Next version:            5.7-SNAPSHOT
No maintenance branch

$ ./scripts/release.py -f -m 5.6.1-SNAPSHOT
Releasing from branch:   master
Current version:         5.6-SNAPSHOT
Tag:                     5.6
Next version:            5.7-SNAPSHOT
Maintenance version:     5.6.1-SNAPSHOT

$ ./scripts/release.py -f -b 5.5.0
Releasing from branch:   5.5.0
Current version:         5.5.0-HF01-SNAPSHOT
Tag:                     5.5.0-HF01
Next version:            5.5.0-HF02-SNAPSHOT
No maintenance branch

$ ./scripts/release.py -b 5.5.0 -t sometag
Releasing from branch 5.5.0
Current version:      5.5.0-HF01-SNAPSHOT
Tag:                  sometag
Next version:         5.5.0-HF01-SNAPSHOT
No maintenance branch"""

from collections import namedtuple
from datetime import datetime
import fnmatch
import optparse
import os
import re
import shutil
import sys
import tempfile
from lxml import etree
from IndentedHelpFormatterWithNL import IndentedHelpFormatterWithNL
from nxutils import ExitException, Repository, assert_git_config, extract_zip, \
    log, make_zip, system
from terminalsize import get_terminal_size


PKG_RENAMINGS = {
    # Tomcat packages
    "nuxeo-distribution/nuxeo-server-tomcat/target/nuxeo-server-tomcat-%s.zip": "nuxeo-server-%s-tomcat"
}

PKG_RENAMINGS_OPTIONALS = {
    # Tomcat packages
    "nuxeo-distribution/nuxeo-server-tomcat/target/nuxeo-server-tomcat-%s-sdk.zip": "nuxeo-server-%s-tomcat-sdk"
}

PKG_MP = {
    "nuxeo-distribution/nuxeo-marketplace-jsf-ui/target/nuxeo-marketplace-jsf-ui-%s.zip": "nuxeo-jsf-ui-%s.zip"
}

NAMESPACES = {"pom": "http://maven.apache.org/POM/4.0.0"}
etree.register_namespace('pom', 'http://maven.apache.org/POM/4.0.0')


# pylint: disable=C0103
def etree_parse(xmlfile):
    """XML parsing with context error logging"""
#     if 'done' not in locals():
#         etree.register_namespace('pom', 'http://maven.apache.org/POM/4.0.0')
#         done = True
    try:
        tree = etree.parse(xmlfile)
    except etree.XMLSyntaxError as e:
        raise ExitException(1, "XML syntax error on '%s'\n%s" %
                            (xmlfile, e.message))
    return tree

# pylint: disable-msg=too-many-instance-attributes, too-few-public-methods
class ReleaseInfo(object):
    """Release information. This object is used to construct a Release as well as to (de)serialize it."""

    # pylint: disable-msg=too-many-arguments, too-many-locals
    def __init__(self, module=None, remote_alias=None, branch=None, tag="auto",
                 next_snapshot=None, maintenance_version="discard", is_final=False, skip_tests=False,
                 skip_its=False, profiles='', other_versions=None, files_pattern=None, props_pattern=None,
                 msg_commit='', msg_tag='', auto_increment_policy='auto_patch', deploy = False, dryrun = False,
                 interactive = False):
        self.module = module
        self.remote_alias = remote_alias
        self.branch = branch
        self.tag = tag
        self.next_snapshot = next_snapshot
        """Backward compat for maintenance_version = auto"""
        self.maintenance_version = "discard" if maintenance_version == "auto" else maintenance_version
        self.is_final = is_final
        self.skip_tests = skip_tests
        self.skip_its = skip_its
        self.profiles = profiles
        self.other_versions = other_versions
        self.files_pattern = files_pattern
        self.props_pattern = props_pattern
        self.msg_commit = msg_commit
        self.msg_tag = msg_tag
        self.auto_increment_policy = auto_increment_policy
        self.deploy = deploy
        self.dryrun = dryrun

    def compute_other_versions(self):
        self.other_versions = ':'.join((self.files_pattern, self.props_pattern, self.other_versions))
        if self.other_versions == "::":
            self.other_versions = None

    @staticmethod
    def get_release_log(path=os.getcwd()):
        """Return the path for the file containing the release parameters
given the path parameter.

        'path': root path of the repository being released."""
        return os.path.abspath(os.path.join(path, os.pardir,
                               "release-%s.log" % os.path.basename(path)))

    # pylint: disable=R0914
    def read_release_log(self, release_log):
        """Read release parameters generated for the given path.

        'release_log': path to a release log file."""
        log("Reading parameters from %s ..." % release_log)
        with open(release_log, "rb") as f:
            for line in f:
                (key, value) = line.split("=", 1)
                key = key.strip()
                value = value.strip()
                if key == "MODULE":
                    self.module = value
                elif key == "REMOTE":
                    self.remote_alias = value
                elif key == "BRANCH":
                    self.branch = value
                elif key == "SNAPSHOT":
                    self.snapshot = value
                elif key == "TAG":
                    self.tag = value
                elif key == "NEXT_SNAPSHOT":
                    self.next_snapshot = value
                elif key == "MAINTENANCE":
                    self.maintenance_version = value
                elif key == "FINAL":
                    self.is_final = value == "True"
                elif key == "SKIP_TESTS":
                    self.skip_tests = value == "True"
                elif key == "SKIP_ITS":
                    self.skip_its = value == "True"
                elif key == "PROFILES":
                    self.profiles = value
                elif key == "OTHER_VERSIONS":
                    self.other_versions = value
                elif key == "FILES_PATTERN":
                    self.files_pattern = value
                elif key == "PROPS_PATTERN":
                    self.props_pattern = value
                elif key == "MSG_COMMIT":
                    self.msg_commit = value
                elif key == "MSG_TAG":
                    self.msg_tag = value
                elif key == "AUTO_INCREMENT_POLICY":
                    self.auto_increment_policy = value
                elif key == "DRY_RUN":
                    self.dryrun = value == "True"
                else:
                    log("[WARN] Release info parsing failure for file %s on line: %s" % (release_log, line), sys.stderr)
        self.compute_other_versions()

# pylint: disable=R0902
class Release(object):
    """Nuxeo release manager.

    See 'self.perpare()', 'self.perform()'."""

    # pylint: disable=R0913
    def __init__(self, repo, release_info):
        if release_info.dryrun:
            print "DEBUG -- init Release with:"
            for key, value in vars(release_info).iteritems():
                print "DEBUG: %s=%s" % (key, value)
            print

        self.repo = repo
        self.params = release_info
        # TODO NXP-21007: Use release_info field instead of copy?
        self.branch = release_info.branch
        self.is_final = release_info.is_final
        self.skip_tests = release_info.skip_tests
        self.skip_its = release_info.skip_its
        self.auto_increment_policy = release_info.auto_increment_policy
        if release_info.profiles:
            self.profiles = ',' + release_info.profiles
        else:
            self.profiles = release_info.profiles
        self.msg_commit = release_info.msg_commit
        self.msg_tag = release_info.msg_tag
        # Evaluate default values, if not provided
        self.set_other_versions_and_patterns(release_info)
        self.set_snapshot()
        self.set_tag(release_info)
        self.set_next_snapshot(release_info)
        self.set_maintenance_version(release_info)

        # Detect if working on Nuxeo main sources
        tree = etree_parse(os.path.join(self.repo.basedir, "pom.xml"))
        artifact_id = tree.getroot().find("pom:artifactId", NAMESPACES)
        self.repo.is_nuxeoecm = "nuxeo-ecm" == artifact_id.text
        if self.repo.is_nuxeoecm:
            log("Working on Nuxeo main repository...")
        else:
            log("Working on custom repository...")

        if release_info.dryrun:
            print "\nDEBUG -- Release initialized with:"
            for key, value in vars(self).iteritems():
                print "DEBUG: %s=%s" % (key, value)
            print

    def set_other_versions_and_patterns(self, release_info):
        """Set other versions and replacement patterns"""
        Patterns = namedtuple('Patterns', ['files', 'props'])
        self.default_patterns = Patterns(
            # Files extensions
            "^.*\\.(xml|properties|txt|defaults|sh|html|nxftl|md)$",
            # Properties like nuxeo.*.version
            "{%s}(nuxeo|marketplace)\\..*version" % NAMESPACES.get("pom"))
        other_versions = release_info.other_versions
        custom_files_pattern = ""
        custom_props_pattern = ""
        other_versions_split = []
        if other_versions:
            # Parse custom patterns
            other_versions_split = other_versions.split(':')
            if len(other_versions_split) == 3:
                if other_versions_split[0]:
                    custom_files_pattern = other_versions_split[0]
                    try:
                        re.compile(custom_files_pattern)
                    except re.error, e:
                        raise ExitException(1, "Bad pattern: '%s'\n%s" % (custom_files_pattern, e.message))
                if other_versions_split[1]:
                    try:
                        re.compile(other_versions_split[1])
                    except re.error, e:
                        raise ExitException(1, message = "Bad pattern: '%s'\n%s" % (other_versions_split[1], e.message))
                    custom_props_pattern = "{%s}%s" % (NAMESPACES.get("pom"), other_versions_split[1])
                other_versions = other_versions_split[2]
            elif len(other_versions_split) == 1:
                other_versions = other_versions_split[0]
            else:
                raise ExitException(1, "Could not parse other_versions parameter '%s'." % other_versions)
            other_versions_split = []
            if other_versions:
                # Parse version replacements
                for other_version in other_versions.split(","):
                    other_version_split = other_version.split("/")
                    if (len(other_version_split) < 2 or
                        len(other_version_split) > 3 or
                        other_version_split.count(None) > 0 or
                            other_version_split.count("") > 0):
                        raise ExitException(
                            1, "Could not parse other_versions parameter '%s'."
                            % other_versions)
                    other_versions_split.append(other_version_split)
        self.other_versions = other_versions_split
        self.custom_patterns = Patterns(custom_files_pattern,
                                        custom_props_pattern)

    def set_snapshot(self):
        """Set current version from root POM."""
        tree = etree_parse(os.path.join(self.repo.basedir, "pom.xml"))
        version_elem = tree.getroot().find("pom:version", NAMESPACES)
        if version_elem is None:
            version_elem = tree.getroot().find("pom:parent/pom:version",
                                               NAMESPACES)
        self.snapshot = version_elem.text

    def set_tag(self, release_info):
        """Set evaluated tag based on 'self.snapshot'."""
        if release_info.tag != "auto":
            self.tag = release_info.tag
        elif self.is_final is True:
            self.tag = self.snapshot.partition("-SNAPSHOT")[0]
        else:
            date = datetime.now().strftime("%Y%m%d_%H%M")
            self.tag = self.snapshot.replace("-SNAPSHOT", "-I" + date)

    @staticmethod
    def auto_increment(current_version, policy_in):
        """Returns the next version following 'current_version' with 'policy_in' apply"""
        next_version = current_version
        policy = re.sub('_(no_zero)', '', policy_in)
        semver = re.compile('^(?P<major>(?:0|[1-9][0-9]*))'
                            '(?:\\.(?P<minor>(?:0|[1-9][0-9]*)))?'
                            '(?:\\.(?P<patch>(?:0|[1-9][0-9]*)))?')
        verinfo = semver.match(current_version).groupdict()
        for key in ['patch', 'minor', 'major']:
            if (verinfo[key] and policy == 'auto_last') or policy == 'auto_%s' % key:
                verinfo[key] = (int(verinfo[key]) if verinfo[key] else 0) + 1
                break
        if verinfo['patch']:
            if policy in ['auto_minor', 'auto_major']:
                verinfo['patch'] = 0
            next_version = '%d.%d.%d-SNAPSHOT' % (
                int(verinfo['major']),
                int(verinfo['minor'] if verinfo['minor'] else 0),
                int(verinfo['patch']))
        elif verinfo['minor']:
            if policy == 'auto_major':
                verinfo['minor'] = 0 if policy_in != 'auto_major_no_zero' else 1
            next_version = '%d.%d-SNAPSHOT' % (
                int(verinfo['major']),
                int(verinfo['minor']))
        elif verinfo['major']:
            next_version = '%d-SNAPSHOT' % (
                int(verinfo['major']))
        return next_version

    def set_maintenance_version(self, release_info):
        """Set evaluated next maintenance snapshot version based on:
        - 'release_info.maintenance_version'
        - 'release_info.is_final' (TODO: not yet but it should)
        and the maintenance branch name based on:
        - 'self.tag'
        - 'self.next_snapshot'
        - 'self.branch'"""
        matches = re.match('^auto_(patch|minor|major)$', release_info.maintenance_version)
        if matches:
            self.maintenance_version = self.auto_increment(self.snapshot, release_info.maintenance_version)
        else:
            self.maintenance_version = release_info.maintenance_version

        self.maintenance_branch = self.tag
        if self.next_snapshot != 'done' and self.branch == self.maintenance_branch:
            self.maintenance_branch += ".0"

    def set_next_snapshot(self, release_info):
        """Set evaluated next snapshot version based on:
        - 'release_info.next_snapshot'
        - 'release_info.is_final'
        - 'release_info.auto_increment_policy'"""
        if release_info.next_snapshot is not None and release_info.next_snapshot != "auto":
            self.next_snapshot = release_info.next_snapshot
        elif release_info.is_final:
            self.next_snapshot = self.auto_increment(self.snapshot, release_info.auto_increment_policy)
        else:
            self.next_snapshot = self.snapshot

    # TODO NXP-21007: Use release_info field instead of copy?
    def log_summary(self, store_params=True):
        """Log summary of configuration for current release. Returns the log filename"""
        log("Releasing from branch:".ljust(25) + self.branch)
        log("Current version:".ljust(25) + self.snapshot)
        log("Tag:".ljust(25) + "release-" + self.tag)
        log("Next version:".ljust(25) + self.next_snapshot)
        if self.maintenance_version == "discard":
            log("Maintenance branch %s deleted".ljust(25) % self.tag)
        else:
            log("Maintenance branch:".ljust(25) + self.maintenance_branch)
            log("Maintenance version:".ljust(25) + self.maintenance_version)
        if self.skip_tests:
            log("Tests execution is skipped")
        elif self.skip_its:
            log("Integration Tests execution is skipped")
        if self.custom_patterns.files:
            log("Custom files pattern: ".ljust(25) +
                self.custom_patterns.files)
        if self.custom_patterns.props:
            log("Custom props pattern: ".ljust(25) +
                self.custom_patterns.props[35:])
        if self.other_versions:
            for other_version in self.other_versions:
                log("Also replace version:".ljust(25) +
                    '/'.join(other_version))
        if store_params:
            release_log = os.path.abspath(os.path.join(
                self.repo.basedir, os.pardir,
                "release-%s.log" % os.path.basename(self.repo.basedir)))
            with open(release_log, "wb") as f:
                f.write("MODULE=%s\n" % os.path.basename(self.repo.basedir) +
                        "REMOTE=%s\n" % self.repo.alias +
                        "BRANCH=%s\n" % self.branch +
                        "SNAPSHOT=%s\n" % self.snapshot +
                        "TAG=%s\n" % self.tag +
                        "NEXT_SNAPSHOT=%s\n" % self.next_snapshot +
                        "MAINTENANCE=%s\n" % self.maintenance_version +
                        "FINAL=%s\n" % self.is_final +
                        "SKIP_TESTS=%s\n" % self.skip_tests +
                        "SKIP_ITS=%s\n" % self.skip_its +
                        "PROFILES=%s\n" % self.profiles +
                        "OTHER_VERSIONS=%s\n" % (','.join('/'.join(other_version)
                                                          for other_version in self.other_versions)) +
                        "FILES_PATTERN=%s\n" % self.custom_patterns.files +
                        "PROPS_PATTERN=%s\n" % self.custom_patterns.props[35:] +
                        "MSG_COMMIT=%s\n" % self.msg_commit +
                        "MSG_TAG=%s\n" % self.msg_tag +
                        "AUTO_INCREMENT_POLICY=%s\n" % self.auto_increment_policy +
                        "DRY_RUN=%s\n" % self.params.dryrun)
            log("Parameters stored in %s" % release_log)
        log("")
        return release_log if store_params else None

    # pylint: disable=R0912,R0914
    def update_versions(self, old_version, new_version):
        """Update all occurrences of 'old_version' with 'new_version'.
        Return True if there was some change."""
        changed = False
        if old_version == new_version:
            return changed
        log("[INFO] Replacing occurrences of %s with %s" % (old_version,
                                                            new_version))
        if self.custom_patterns.files:
            files_pattern = re.compile("(%s)|(%s)" %
                                       (self.default_patterns.files,
                                        self.custom_patterns.files))
        else:
            files_pattern = re.compile(self.default_patterns.files)
        if self.custom_patterns.props:
            props_pattern = re.compile("(%s)|(%s)" %
                                       (self.default_patterns.props,
                                        self.custom_patterns.props))
        else:
            props_pattern = re.compile(self.default_patterns.props)

        for root, dirs, files in os.walk(os.getcwd(), True, None, True):
            for subdir in set(dirs) & set([".git", "target"]):
                dirs.remove(subdir)
            for name in files:
                replaced = False
                if fnmatch.fnmatch(name, "pom*.xml"):
                    tree = etree_parse(os.path.join(root, name))
                    # Parent POM version
                    parent = tree.getroot().find("pom:parent", NAMESPACES)
                    if parent is not None:
                        elem = parent.find("pom:version", NAMESPACES)
                        if elem is not None and elem.text == old_version:
                            elem.text = new_version
                            replaced = True
                    # POM version
                    elem = tree.getroot().find("pom:version", NAMESPACES)
                    if elem is not None and elem.text == old_version:
                        elem.text = new_version
                        replaced = True
                    properties = tree.getroot().find("pom:properties",
                                                     NAMESPACES)
                    if properties is not None:
                        for prop in properties.getchildren():
                            if (not isinstance(prop, etree._Comment)
                                and props_pattern.match(prop.tag)
                                    and prop.text == old_version):
                                prop.text = new_version
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

    def package_all(self, version=None):
        """Repackage files to be uploaded.

        'version': version to package; defaults to the current tag (without the
        'release-' prefix."""
        if not self.repo.is_nuxeoecm:
            log("Skip packaging step (not a main Nuxeo repository).")
            return
        self.archive_dir = os.path.abspath(
            os.path.join(self.repo.basedir, os.pardir, "archives"))
        if os.path.isdir(self.archive_dir):
            shutil.rmtree(self.archive_dir)
        os.mkdir(self.archive_dir)
        self.tmpdir = tempfile.mkdtemp()

        if version is None:
            version = self.tag

        # Default packages
        for old, new in PKG_RENAMINGS.items():
            self.package(old % version, new % version)
        # Tomcat SDK packages
        for old, new in PKG_RENAMINGS_OPTIONALS.items():
            self.package(old % version, new % version, False)
        # MP
        for old, new in PKG_MP.items():
            self.package_mp(old % version, new % version, False)

        self.package_sources(version)
        shutil.rmtree(self.tmpdir)

    def package(self, old_archive, new_name, failonerror=True):
        """Repackage a ZIP following the rules:
            - have a parent directory with the same name as the archive name
            - set executable bit on scripts in bin/
            - activate the setup wizard

        If 'failonerror', raise an ExitException in case of missing filef."""
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
            for filef in ls:
                shutil.move(filef, os.path.join(new_name, filef))

        files = os.listdir(os.path.join(new_name, "bin"))
        for filename in (fnmatch.filter(files, "*ctl") +
                         fnmatch.filter(files, "*.sh") +
                         fnmatch.filter(files, "*.command")):
            os.chmod(os.path.join(new_name, "bin", filename), 0744)
        with open(os.path.join(new_name, "bin", "nuxeo.conf"), "a") as f:
            f.write("nuxeo.wizard.done=false\n")
        make_zip(os.path.join(
                 self.archive_dir, new_name + ".zip"), os.getcwd(), new_name)
        os.chdir(cwd)
        # Cleanup temporary directory
        shutil.rmtree(os.path.join(self.tmpdir, new_name))

    def package_sources(self, version):
        sources_archive_name = "nuxeo-%s-sources.zip" % version
        self.repo.archive(os.path.join(self.archive_dir, sources_archive_name))

    def package_mp(self, old_archive, new_name, failonerror=True):
        if not os.path.isfile(old_archive):
            if failonerror:
                raise ExitException(1, "Could not find %s" % old_archive)
            else:
                log("[WARN] Could not find %s" % old_archive, sys.stderr)
                return
        shutil.copy(old_archive, os.path.join(self.archive_dir, new_name))

    @staticmethod
    def get_message(message, additional_message):
        """Returns a message prefixing the additional message by ': ' if both
        parameters are filled."""
        if message and message.strip():
            if additional_message and additional_message.strip():
                return "%s: %s" % (message, additional_message)
            return message
        return additional_message

    def get_commit_message(self, additional_message):
        """Returns the given message prefixed with the commit message if
        any."""
        return self.get_message(self.msg_commit, additional_message)

    def get_tag_message(self, additional_message):
        """Returns the given message prefixed with tag message if any, or with
        the message commit if any."""
        message = self.msg_tag
        if not message or not message.strip():
            message = self.msg_commit
        return self.get_message(message, additional_message)

    # pylint: disable=R0915
    def prepare(self, dodeploy=False, doperform=False, dryrun=False, upgrade_only=False):
        """ Prepare the release: build, change versions, tag and package source and distributions.
        If the release branch has been previously created with the 'branch' command, the next snapshot (option '-n')
        must be set to 'done'.

        'dodeploy': staging mode with 'nightly' Maven profile activated
        'doperform': onestep process = prepare+perform without staging
        'dryrun': dry run mode (no Maven deployment, nor Git push)
        'upgrade_only': only upgrade other versions (used to keep Marketplace Packages aligned even if not released)"""
        if upgrade_only and self.next_snapshot == 'done':
            log("[INFO] NO OP")
            return
        if dryrun:
            log("[INFO] #### DRY RUN MODE ####")
        self.check_branch_to_release()
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch, with_optionals=True)
        log("[INFO] Check release-ability...")
        self.check()

        if not upgrade_only:
            if self.next_snapshot != 'done':
                log("\n[INFO] Releasing branch {0}, create maintenance branch {1},"
                    " update versions, commit and tag as release-{2}..."
                    .format(self.branch, self.maintenance_branch, self.tag))
                self.repo.git_recurse("checkout -b %s" % self.maintenance_branch, with_optionals=True)
            else:
                log("\n[INFO] Releasing branch {0}, update versions, commit and tag as release-{1}..."
                    .format(self.branch, self.tag))
            msg_commit = "Release %s, update %s to %s" % (self.branch, self.snapshot, self.tag)
            self.update_versions(self.snapshot, self.tag)
            for other_version in self.other_versions:
                if len(other_version) > 0:
                    self.update_versions(other_version[0], other_version[1])
                    msg_commit += ", update %s to %s" % (other_version[0], other_version[1])
            self.repo.git_recurse("commit -m'%s' -a" % (self.get_commit_message(msg_commit)), with_optionals=True)
            msg_tag = "Release release-%s from %s on %s" % (self.tag, self.snapshot, self.branch)
            self.repo.git_recurse("tag -a release-%s -m'%s'" % (self.tag, self.get_tag_message(msg_tag)))

            # TODO NXP-8569 Optionally merge maintenance branch on source
            if self.maintenance_version != "discard":
                # Maintenance branches are kept, so update their versions
                log("\n[INFO] Maintenance branch...")
                msg_commit = "Update %s to %s" % (self.tag, self.maintenance_version)
                self.update_versions(self.tag, self.maintenance_version)
                self.repo.git_recurse("commit -m'%s' -a" % (self.get_commit_message(msg_commit)), with_optionals=True)

            log("\n[INFO] Released branch %s (update version and commit)..." % self.branch)

        if self.next_snapshot != 'done':
            self.repo.git_recurse("checkout -f %s" % self.branch, with_optionals=True)
            # Update released branches with next versions
            if not upgrade_only:
                msg_commit = "Post release %s" % self.tag
                post_release_change = self.update_versions(self.snapshot, self.next_snapshot)
                if post_release_change:
                    msg_commit += "\nUpdate %s to %s" % (self.snapshot, self.next_snapshot)
            else:
                msg_commit = ''
                post_release_change = False
            for other_version in self.other_versions:
                if len(other_version) == 3 and self.update_versions(other_version[0], other_version[2]):
                    post_release_change = True
                    msg_commit += "\nUpdate %s to %s" % (other_version[0], other_version[2])
            if post_release_change:
                self.repo.git_recurse("commit -m'%s' -a" % (self.get_commit_message(msg_commit)), with_optionals=True)

        if upgrade_only and doperform:
            self.perform(skip_tests=self.skip_tests, skip_ITs=self.skip_its, dryrun=dryrun, upgrade_only=True)

        if not upgrade_only:
            log("\n[INFO] Build and package release-%s..." % self.tag)
            self.repo.git_recurse("checkout release-%s" % self.tag)
            if dodeploy:
                log("\n[INFO] Staging mode: deploy artifacts...")
                commands = "clean deploy"
                profiles = "release,-qa,nightly" + self.profiles
            else:
                commands = "clean install"
                profiles = "release,-qa" + self.profiles
            if doperform:
                self.perform(skip_tests=self.skip_tests, skip_ITs=self.skip_its, dryrun=dryrun)
            else:
                self.repo.mvn(commands, skip_tests=self.skip_tests, skip_ITs=self.skip_its, profiles=profiles,
                              dryrun=dryrun)
            if not dryrun:
                self.package_all()
            # TODO NXP-8571 package sources
        os.chdir(cwd)

    def release_branch(self, dryrun=False, upgrade_only=False):
        """ Create the release branch, also change versions on the source branch.
        The release will then have to be "prepared" and "performed" from that branch using the '--next=done' option.
        If kept, it will become the maintenance branch after release.

        'dryrun': dry run mode (no Maven deployment, nor Git push)
        'upgrade_only': only upgrade other versions (used to keep Marketplace Packages aligned even if not released)"""
        if dryrun:
            log("[INFO] #### DRY RUN MODE ####")
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch, with_optionals=True)

        log("[INFO] Check release-ability...")
        self.check()

        if not upgrade_only:
            log("\n[INFO] Creating release branch {0} from branch {1}..."
                .format(self.maintenance_branch, self.branch))
            self.repo.git_recurse("checkout -b %s" % self.maintenance_branch, with_optionals=True)

        self.repo.git_recurse("checkout -f %s" % self.branch, with_optionals=True)
        # Update source branch with next versions
        if not upgrade_only:
            msg_commit = "Post release %s" % self.tag
            post_release_change = self.update_versions(self.snapshot, self.next_snapshot)
            if post_release_change:
                msg_commit += "\nUpdate %s to %s" % (self.snapshot, self.next_snapshot)
        else:
            msg_commit = ''
            post_release_change = False
        for other_version in self.other_versions:
            if len(other_version) == 3 and self.update_versions(other_version[0], other_version[2]):
                post_release_change = True
                msg_commit += "\nUpdate %s to %s" % (other_version[0], other_version[2])
        if post_release_change:
            self.repo.git_recurse("commit -m'%s' -a" % (self.get_commit_message(msg_commit)), with_optionals=True)
        if dryrun:
            dry_option = "-n"
        else:
            dry_option = ""
        self.repo.git_recurse("push %s %s %s" % (dry_option, self.repo.alias, self.branch), with_optionals=True)
        if not upgrade_only:
            self.repo.git_recurse("push %s %s %s" % (dry_option, self.repo.alias, self.maintenance_branch),
                                  with_optionals=True)
        log("[INFO] Set next snapshot option ('-n') to 'done' on next calls to 'prepare' or 'perform' commands.")
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
                log("Also replace version:".ljust(25) +
                    '/'.join(other_version))
        log("")

        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.git_recurse("checkout release-%s" % self.tag)

        log("[INFO] Check release-ability...")
        self.check()

        log("\n[INFO] Creating maintenance branch {0} from {1}, "
            "update versions and commit...".format(self.branch, self.tag))
        self.repo.git_recurse("checkout -b %s" % self.branch)
        msg_commit = "Update %s to %s" % (self.tag, self.maintenance_version)
        self.update_versions(self.tag, self.maintenance_version)
        for other_version in self.other_versions:
            if len(other_version) > 0:
                self.update_versions(other_version[0], other_version[1])
                msg_commit += ", update %s to %s" % (other_version[0],
                                                     other_version[1])
        self.repo.git_recurse("commit -m'%s' -a" %
                              (self.get_commit_message(msg_commit)))
        os.chdir(cwd)

    def perform(self, skip_tests=True, skip_ITs=True, dryrun=False, upgrade_only=False):
        """ Perform the release: push source, deploy artifacts and upload packages.
        If the release branch has been previously created with the 'branch' command, the next snapshot (option '-n')
        must be set to 'done'.

        'skip_tests': whether to run tests during Maven deployment
        'skip_ITs': whether to run Integration Tests during Maven deployment
        'dryrun': dry run mode (no Maven deployment, nor Git push)
        'upgrade_only': only upgrade other versions (used to keep Marketplace Packages aligned even if not released)"""
        if upgrade_only and self.next_snapshot == 'done':
            log("[INFO] NO OP")
            return
        if dryrun:
            log("[INFO] #### DRY RUN MODE ####")
        self.check_branch_to_release()
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch, with_optionals=True)
        if dryrun:
            dry_option = "-n"
        else:
            dry_option = ""
        if self.next_snapshot != 'done':
            self.repo.git_recurse("push --porcelain %s %s %s" % (dry_option, self.repo.alias, self.branch),
                                  with_optionals=True)
        if not upgrade_only:
            if self.maintenance_version != "discard":
                self.repo.git_recurse("push --porcelain %s %s %s" % (dry_option, self.repo.alias,
                                                                     self.maintenance_branch), with_optionals=True)
            elif self.next_snapshot == 'done':
                log("\n[INFO] Delete remote maintenance branch %s..." % self.maintenance_branch)
                if not dryrun:
                    self.repo.git_recurse("push --porcelain %s %s :%s" % (dry_option, self.repo.alias,
                                                                          self.maintenance_branch), with_optionals=True)
            self.repo.git_recurse("push --porcelain %s %s release-%s" % (dry_option, self.repo.alias, self.tag))
            self.repo.git_recurse("checkout release-%s" % self.tag)
            self.repo.mvn("clean deploy", skip_tests=skip_tests, skip_ITs=skip_ITs,
                          profiles="release,-qa" + self.profiles, dryrun=dryrun)
        os.chdir(cwd)

    def check_branch_to_release(self):
        """ Check the parameters: if the branch to release has been previously created (the next snapshot is then set to
        'done'), then the current branch is expected to equal to the current version minus '-SNAPSHOT'."""
        if self.next_snapshot == 'done' and self.branch != self.snapshot.partition("-SNAPSHOT")[0]:
            raise ExitException(1, "When releasing from an already created release branch (command 'branch'), the"
                                " branch to release (option '-b') must equal to the current version minus '-SNAPSHOT':"
                                " %s != %s" % (self.branch, self.snapshot.partition("-SNAPSHOT")[0]))

    def check(self):
        """ Check the release is feasible"""
        # TODO NXP-8573 tag and release branch do not already exist
        # TODO NXP-8573 all POMs have a namespace


# pylint: disable=R0912,R0914,R0915
def main():
    assert_git_config()

    try:
        if not os.path.isdir(".git"):
            raise ExitException(1, "That script must be ran from root of a Git"
                                + " repository")
        usage = ("""usage: %prog <command> [options]
       %prog branch [-r alias] [-f] [-b branch] [-t tag] [-n next_snapshot] [-m maintenance] [--dryrun] \
[--arv versions_replacements] [--mc msg_commit]
       %prog prepare [-r alias] [-f] [-d] [--skipTests] [--skipITs] \
[-p profiles] [-b branch] [-t tag] [-n next_snapshot] [-m maintenance] \
[--dryrun] [--arv versions_replacements] [--mc msg_commit] [--mt msg_tag]
       %prog perform [-r alias] [-f] [-p profiles] [-b branch] [-t tag] \
[-m maintenance] [--dryrun]
       %prog package [-b branch] [-t tag] [--skipTests] [--skipITs] \
[-p profiles]
       %prog maintenance [-r alias] [-b branch] [-t tag] [-m maintenance] \
[--arv versions_replacements]
       %prog onestep [-r alias] [-f] [--skipTests] [--skipITs] [-p profiles] \
[-b branch] [-t tag] [-n next_snapshot] [-m maintenance] [--dryrun] \
[--arv versions_replacements] [--mc msg_commit] [--mt msg_tag]
\nCommands:
       branch: Create the release branch so that the branch to release is freed for ongoing development. Following \
'prepare' or 'perform' commands must use option '--next=done'. If kept, that branch will become the maintenance \
branch after release.
       prepare: Prepare the release (build, change versions, tag and package source and distributions). The release \
parameters are stored in a release-nuxeo.log file.
       perform: Perform the release (push sources, deploy artifacts and upload \
packages, tests are always skipped). If no parameter is given, they are read \
from the release-*.log file.
       package: Package distributions and source code in the archives directory.
       maintenance: Create a maintenance branch from an existing tag.
       onestep: Prepare and perform the release in one unique step. No staging is \
possible. The release-*.log file is not read.""")
        description = """Release Nuxeo from a given branch, tag the release, \
then set the next SNAPSHOT version. If a maintenance version was provided, \
then a maintenance branch is kept, else it is deleted after release."""
        help_formatter = IndentedHelpFormatterWithNL(
            # max_help_position=6,
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
                          help="""Deploy artifacts to nightly/staging \
repository (activating the 'nightly' Maven profile). \
Default: '%default'""")
        parser.add_option('--dryrun', action="store_true",
                          dest='dryrun', default=False,
                          help="""Dry run mode. Default: '%default'""")
        parser.add_option('--skipTests', action="store_true",
                          dest='skip_tests', default=False,
                          help="""Skip tests execution (but compile them).
Default: '%default'""")
        parser.add_option('--skipITs', action="store_true",
                          dest='skip_its', default=False,
                          help="""Skip Integration Tests execution (but compile
them). Default: '%default'""")
        parser.add_option('-p', '--profiles', action="store", type="string",
                          dest='profiles',
                          default='',
                          help="""Comma-separated additional Maven profiles.
Default: '%default'\n
Those profiles are activated by default (unless overriden by that parameter):\n
 - 'addons,distrib' for all commands executed on nuxeo-ecm,\n
 - 'release,-qa' for 'prepare', 'perform' and 'onestep' commands,\n
 - 'qa' for 'package' command,\n
 - 'nightly' if 'deploy' option is used.
""")
        versioning_options = optparse.OptionGroup(parser, 'Version policy')
        versioning_options.add_option(
            '-f', '--final', action="store_true", dest='is_final',
            default=False, help="Is it a final release? Default: '%default'")
        versioning_options.add_option(
            "-b", "--branch", action="store", type="string", dest="branch",
            default="auto", help="""Branch to release. \
Default: '%default' = the current branch""")
        versioning_options.add_option(
            "-t", "--tag", action="store", type="string", dest="tag",
            default="auto", help="""Released version. SCM tag is 'release-$TAG'.
Default: '%default'\n
In mode 'auto', if final option is True, then the default value is the current
version minus '-SNAPSHOT', else the 'SNAPSHOT' keyword is replaced with a date
(aka 'date-based release').""")
        versioning_options.add_option(
            "-n", "--next", action="store", type="string",
            dest="next_snapshot", default="auto",
            help="""Version post-release. Default: '%default'\n
In mode 'auto', if final option is True, then the next snapshot is the current
one increased, else it is equal to the current.""")
        versioning_options.add_option(
            '-m', '--maintenance', action="store", dest='maintenance_version',
            default="discard", help="""Maintenance version. Default: '%default'\n
If set, the version will be used on the maintenance branch, else, in 'discard' mode, the maintenance branch is deleted
after release. Instead of an explicit version, an auto increment policy can be used (see '--aip' option).\n
Note that the maintenance branch is always named like the tag without the 'release-'
prefix.""")
        versioning_options.add_option(
            '--arv', '--also-replace-version', action="store",
            dest='other_versions', default=None,
            help="""Other version(s) to replace. Default: '%default'\n
Use a slash ('/') as a separator between old and new version: \
'1.0-SNAPSHOT/1.0'.\n
A version post-release can also be specified: \
'1.0-SNAPSHOT/1.0/1.0.1-SNAPSHOT'.\n
Multiple versions can be replaced using a coma (',') separator:
'1.0-SNAPSHOT/1.0/1.0.1-SNAPSHOT,0.0-SNAPSHOT/2.0.1'.\n
It only applies on files named with xml, properties, txt, defaults, sh, html or
nxftl extension and on 'pom.xml' files. On POM files, it only applies on the
parent version, the POM version and the properties named
'nuxeo|marketplace.*version'. Other patterns for filename extensions and
properties can be provided using two colon (':') separators:
'.*\\.customextension:my.property:1.0-SNAPSHOT/1.0', '.*\\.text::',
':my.property:1.0-SNAPSHOT/1.0/1.1-SNAPSHOT', ... Those patterns are common \
to all replacements, including the released version.\n
Default files and properties patterns are respectively:
'^.*\\.(xml|properties|txt|defaults|sh|html|nxftl|md)$' and
'(nuxeo|marketplace)\\..*version'. They can't be removed.""")
        versioning_options.add_option(
            '--mc', '--msg-commit', action="store", type="string",
            dest='msg_commit', default='',
            help="""Additional message to put in front of default commit \
messages:\n
'Release $BRANCH, update $SNAPSHOT to $TAG, update ...'.\n
'Release release-$TAG from $SNAPSHOT on $BRANCH'
""")
        versioning_options.add_option(
            '--mt', '--msg-tag', action="store", type="string", dest='msg_tag',
            default='',
            help="""Like '--mc' option but specific to tag messages. \
Default tag message:\n
'Release release-$TAG from $SNAPSHOT on $BRANCH'.
""")
        versioning_options.add_option(
            '--aip', '--auto-increment-policy', action="store", type="string", dest='auto_increment_policy',
            default='auto_patch',
            help="""Version increment policy when in 'auto' mode. Default: 'auto_patch'\n
Available options:\n
 - 'auto_last':\tincrement last explicitly defined number (1.2.3 => 1.2.4, 1.2 => 1.3, 1 => 2)\n
 - 'auto_major':\t1.2.3 => 2.0.0\n
 - 'auto_major_no_zero':\t1.2.3 => 2.0.1\n
 - 'auto_minor':\t1.2.3 => 1.3.0\n
 - 'auto_patch':\t1.2.3 => 1.2.4\n
Note: 'auto_last' is not recommended since 1 = 1.0 = 1.0.0, then the zero being explicit or not should not intervene.
""")
        parser.add_option_group(versioning_options)
        (options, args) = parser.parse_args()
        if len(args) == 1:
            command = args[0]
        elif len(args) > 1:
            raise ExitException(
                1, "'command' must be a single argument: '%s'." % (args)
                + " See usage with '-h'.")

        release_info = ReleaseInfo(**vars(options))
        if ("command" in locals() and command == "perform" and os.path.isfile(ReleaseInfo.get_release_log(os.getcwd()))
            and options == parser.get_default_values()):
            release_info.read_release_log(ReleaseInfo.get_release_log(os.getcwd()))

        repo = Repository(os.getcwd(), release_info.remote_alias)
        system("git fetch %s" % (release_info.remote_alias))
        if "command" in locals():
            if command == "onestep" and options.deploy:
                parser.error("command 'onestep' and option 'deploy' are"
                             " mutually exclusive")
            if command == "maintenance":
                if release_info.tag == "auto":
                    release_info.tag = repo.get_current_version()[8:]
                if release_info.tag == "":
                    raise ExitException(1, "Couldn't guess tag name from %s" %
                                        repo.get_current_version())
                if release_info.branch == "auto":
                    release_info.branch = release_info.tag
                repo.git_update("release-%s" % release_info.tag)
                release_info.is_final = True
                if release_info.maintenance_version == "discard":
                    release_info.maintenance_version = release_info.tag + ".1-SNAPSHOT"
                release_info.next_snapshot = None
            if command == "package" and release_info.tag != "auto":
                release_info.branch = release_info.tag
            if command != "maintenance":
                if release_info.branch == "auto":
                    release_info.branch = repo.get_current_version()
                repo.git_update(release_info.branch)
        release = Release(repo, release_info)
        if ("command" not in locals() or
                command != "maintenance" and command != "package"):
            release.log_summary("command" in locals() and command != "perform")
        if "command" not in locals():
            raise ExitException(1, "Missing command. See usage with '-h'.")
        elif command == "branch":
            release.release_branch(dryrun=options.dryrun)
        elif command == "prepare":
            release.prepare(dodeploy=options.deploy, dryrun=options.dryrun)
        elif command == "maintenance":
            release.maintenance()
        elif command == "perform":
            release.perform(dryrun=options.dryrun)
        elif command == "onestep":
            release.prepare(doperform=True, dryrun=options.dryrun)
        elif command == "package":
            log("Packaging %s from %s" % (release.snapshot, release.branch))
            repo.clone(release.branch)
            if release_info.profiles:
                profiles = "qa," + release_info.profiles
            else:
                profiles = "qa"
            # TODO NXP-21007: NXBT-121 resolved as "won't fix", remove that workaround?
            # workaround for NXBT-121: use install instead of package
            repo.mvn("clean install", profiles=profiles,
                     skip_tests=release_info.skip_tests,
                     skip_ITs=release_info.skip_its)
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
