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
from archive_util import make_archive
from datetime import datetime
from lxml import etree
from nxutils import ExitException
from nxutils import Repository
from nxutils import assert_git_config
from nxutils import check_output
from nxutils import get_current_version
from nxutils import log
from nxutils import system
from zipfile import ZIP_DEFLATED
from zipfile import ZipFile
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


class Release(object):
    def __init__(self, repo, branch, tag, next_snapshot, maintenance,
                 is_final=False):
        self.repo = repo
        self.branch = branch
        self.tag = tag
        self.next_snapshot = next_snapshot
        self.maintenance = maintenance
        self.is_final = is_final
        # Evaluate default values, if not provided
        self.snapshot = self.get_current_snapshot()
        if tag is None:
            self.tag = self.get_tag()
        if next_snapshot is None:
            self.next_snapshot = self.get_next_snapshot()

    def get_current_snapshot(self):
        tree = etree.parse(os.path.join(self.repo.basedir, "pom.xml"))
        version_elem = tree.getroot().find("pom:version", namespaces)
        return version_elem.text

    def get_tag(self):
        if self.is_final:
            return self.snapshot.partition("-SNAPSHOT")[0]
        else:
            date = datetime.now().strftime("%Y%m%d_%H%M")
            return self.snapshot.replace("-SNAPSHOT", "-I" + date)

    def get_next_snapshot(self):
        if self.is_final:
            snapshot_split = re.match("(^.*)(\d+)(-SNAPSHOT$)", self.snapshot)
            return (snapshot_split.group(1)
                    + str(int(snapshot_split.group(2)) + 1)  # increment minor
                    + snapshot_split.group(3))
        else:
            return self.snapshot

    def log_summary(self):
        log("Releasing from branch:".ljust(25) + self.branch)
        log("Current version:".ljust(25) + self.snapshot)
        log("Tag:".ljust(25) + self.tag)
        log("Next version:".ljust(25) + self.next_snapshot)
        if self.maintenance is None:
            log("No maintenance branch".ljust(25))
        else:
            log("Maintenance version:".ljust(25) + self.maintenance)
        log("")

    def update_versions(self, old_version, new_version):
        log("Replacing occurrences of %s with %s" % (old_version, new_version))
        pattern = re.compile("^.*\\.(xml|properties|txt|defaults|sh|html)$")
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
                    prop_pattern = re.compile("{" + namespaces.get("pom") + "}nuxeo\..*version")
                    properties = tree.getroot().find("pom:properties", namespaces)
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
        self.tag = "5.6-SNAPSHOT"
        self.package_all()

    def package_all(self):
        """Repackage files to be uploaded"""
        self.archive_dir = os.path.abspath(os.path.join(self.repo.basedir,
                                                   os.pardir, "archives"))
        if not os.path.isdir(self.archive_dir):
            os.mkdir(self.archive_dir)
        self.tmpdir = tempfile.mkdtemp()
        # JBoss packages
        self.package("nuxeo-distribution/nuxeo-distribution-jboss/target/" +
                     "nuxeo-distribution-jboss-%s-nuxeo-cap.zip" % self.tag,
                     "nuxeo-cap-%s-jboss" % self.tag)
        self.package("nuxeo-distribution/nuxeo-distribution-jboss/target/" +
                     "nuxeo-distribution-jboss-%s-nuxeo-cap-ear.zip" % self.tag,
                     "nuxeo-cap-%s-jboss-ear" % self.tag)
        self.package("nuxeo-distribution/nuxeo-distribution-jboss/target/" +
                     "nuxeo-distribution-jboss-%s-nuxeo-dm.zip" % self.tag,
                     "nuxeo-dm-%s-jboss" % self.tag)
        self.package("nuxeo-distribution/nuxeo-distribution-jboss/target/" +
                     "nuxeo-distribution-jboss-%s-nuxeo-dm-ear.zip" % self.tag,
                     "nuxeo-dm-%s-jboss-ear" % self.tag)
        # Tomcat packages
        self.package("nuxeo-distribution/nuxeo-distribution-tomcat/target/" +
                     "nuxeo-distribution-tomcat-%s-coreserver.zip" % self.tag,
                     "nuxeo-coreserver-%s-tomcat" % self.tag)
        self.package("nuxeo-distribution/nuxeo-distribution-tomcat/target/" +
                     "nuxeo-distribution-tomcat-%s-coreserver-sdk.zip" % self.tag,
                     "nuxeo-coreserver-%s-tomcat-sdk" % self.tag, False)
        self.package("nuxeo-distribution/nuxeo-distribution-tomcat/target/" +
                     "nuxeo-distribution-tomcat-%s-nuxeo-cap.zip" % self.tag,
                     "nuxeo-cap-%s-tomcat" % self.tag)
        self.package("nuxeo-distribution/nuxeo-distribution-tomcat/target/" +
                     "nuxeo-distribution-tomcat-%s-nuxeo-cap-sdk.zip" % self.tag,
                     "nuxeo-cap-%s-tomcat-sdk" % self.tag, False)
        # Online/light Tomcat package
        offline_name = "nuxeo-cap-%s-tomcat" % self.tag
        offline_zip = ZipFile(os.path.join(self.archive_dir,
                                           offline_name + ".zip"), "r")
        offline_zip.extractall(self.tmpdir)
        offline_zip.close()
        online_name = "nuxeo-cap-%s-tomcat-online" % self.tag
        # Keep packages.xml file
        os.rename(os.path.join(self.tmpdir, offline_name,
                                   "setupWizardDownloads", "packages.xml"),
                  os.path.join(self.archive_dir, "packages.xml"))
        # Remove Marketplace packages
        shutil.rmtree(os.path.join(self.tmpdir, offline_name,
                                   "setupWizardDownloads"))
        os.rename(os.path.join(self.tmpdir, offline_name),
                  os.path.join(self.tmpdir, online_name))
        shutil.make_archive(os.path.join(self.archive_dir, online_name), "zip",
                            os.path.join(self.tmpdir, online_name), online_name)
        # Marketplace packages
        archive_mp_dir = os.path.join(self.archive_dir, "mp")
        if not os.path.isdir(archive_mp_dir):
            os.mkdir(archive_mp_dir)
        os.rename("nuxeo-distribution/nuxeo-marketplace-cmf/target/" +
                  "nuxeo-marketplace-cmf-%s.zip" % self.tag,
                  os.path.join(archive_mp_dir, "nuxeo-cmf-%s.zip" % self.tag))
        os.rename("nuxeo-distribution/nuxeo-marketplace-content-browser/target/" +
                  "nuxeo-marketplace-content-browser-%s.zip" % self.tag,
                  os.path.join(archive_mp_dir,
                               "nuxeo-content-browser-%s.zip" % self.tag))
        os.rename("nuxeo-distribution/nuxeo-marketplace-content-browser/target/" +
                  "nuxeo-marketplace-content-browser-%s-cmf.zip" % self.tag,
                  os.path.join(archive_mp_dir,
                               "nuxeo-content-browser-cmf-%s.zip" % self.tag))
        os.rename("nuxeo-distribution/nuxeo-marketplace-dam/target/" +
                  "nuxeo-marketplace-dam-%s.zip" % self.tag,
                  os.path.join(archive_mp_dir, "nuxeo-dam-%s.zip" % self.tag))
        os.rename("nuxeo-distribution/nuxeo-marketplace-dm/target/" +
                  "nuxeo-marketplace-dm-%s.zip" % self.tag,
                  os.path.join(archive_mp_dir, "nuxeo-dm-%s.zip" % self.tag))
        os.rename("nuxeo-distribution/nuxeo-marketplace-social-collaboration/target/" +
                  "nuxeo-marketplace-social-collaboration-%s.zip" % self.tag,
                  os.path.join(archive_mp_dir, "nuxeo-sc-%s.zip" % self.tag))
        log("Checking packages integrity...")
        for package in os.listdir(archive_mp_dir):
            m = hashlib.md5()
            with open(os.path.join(archive_mp_dir, package), "rb") as f:
                m.update(f.read())
            package_md5 = m.hexdigest()
            found_package = False
            found_package_md5 = False
            for line in open(os.path.join(self.archive_dir, "packages.xml")):
                if package in line:
                    found_package = True
                if package_md5 in line:
                    found_package_md5 = True
                if found_package and found_package_md5:
                    break
            if not found_package:
                log("[ERROR] Could not find %s in packages.xml" % package,
                sys.stderr)
            if not found_package_md5:
                log("[ERROR] %s MD5 did not match packages.xml information"
                    % package, sys.stderr)
        log("Done.")
        shutil.rmtree(self.tmpdir)

    def package(self, old_archive, new_name, failonerror=True):
        """Repackage a ZIP following the rules:
            - have a parent directory with the same name as the archive name
            - set executable bit on scripts in bin/
            - activate the setup wizard
        """
        if not os.path.isfile(old_archive):
            if failonerror:
                raise ExitException(1, "Could not find %s" % old_archive)
            else:
                log("[WARN] Could not find %s" % old_archive, sys.stderr)
                return
        new_archive = os.path.join(self.archive_dir, new_name + ".zip")
        oldzip = ZipFile(old_archive, "r")
        oldzip.extractall(os.path.join(self.tmpdir, new_name))
        oldzip.close()
        log("Packaging %s ..." % new_archive)
        cwd = os.getcwd()
        os.chdir(os.path.join(self.tmpdir, new_name))
        ls = os.listdir(os.curdir)
        if len(ls) == 1:
            os.rename(ls[0], new_name)
        else:
            os.mkdir(new_name)
            for file in ls:
                os.rename(file, os.path.join(new_name, file))

        files = os.listdir(os.path.join(new_name, "bin"))
        for filename in (fnmatch.filter(files, "*ctl") +
                        fnmatch.filter(files, "*.sh") +
                        fnmatch.filter(files, "*.command")):
            os.chmod(os.path.join(new_name, "bin", filename), 0744)
        with open(os.path.join(new_name, "bin", "nuxeo.conf"), "a") as f:
            f.write("nuxeo.wizard.done=false\n")
        make_archive(os.path.join(self.archive_dir, new_name), "zip",
                            os.getcwd(), new_name)
        os.chdir(cwd)
        shutil.rmtree(os.path.join(self.tmpdir, new_name))

    def prepare(self):
        """ Prepare the release: build, change versions, tag and package source and
        distributions"""
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

        if self.maintenance is not None:
            # Maintenance branches are kept, so update their versions
            self.update_versions(self.tag, self.maintenance)
            self.repo.system_recurse("git commit -m'Post release %s' -a" % self.tag)

        # Update released branches
        self.repo.system_recurse("git checkout %s" % self.branch)
        self.update_versions(self.snapshot, self.next_snapshot)
        self.repo.system_recurse("git commit -m'Post release %s' -a" % self.tag)

        if self.maintenance is None:
            # Delete maintenance branches
            self.repo.system_recurse("git branch -D %s" % self.tag)

        # Build, test and package
        self.repo.system_recurse("git checkout release-%s" % self.tag)
        system("mvn %s clean install -Dmaven.test.skip=true \
                -Prelease,addons,distrib,all-distributions,-qa" % mvn_opts)
        self.package_all()
        # TODO NXP-8571 package sources
        os.chdir(cwd)

    def perform(self):
        """ Perform the release: push source, deploy artifacts and upload
        packages"""
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch)
        self.repo.system_recurse("git push %s %s" % (self.repo.alias,
                                                     self.branch))
        if self.maintenance is not None:
            self.repo.system_recurse("git push %s %s" % (self.repo.alias,
                                                         self.tag))
        self.repo.system_recurse("git push --tags")
        self.repo.system_recurse("git checkout release-%s" % self.tag)
        system("mvn %s clean deploy -Dmaven.test.skip=true \
                -Prelease,addons,distrib,all-distributions,-qa" % mvn_opts)
        os.chdir(cwd)

    def check(self):
        """ Check the release is feasible"""
        # TODO NXP-8573 tag and release branch do not already exist
        # TODO NXP-8573 all POMs have a namespace


def main():
    global mvn_opts
    global namespaces
    assert_git_config()
    namespaces = {"pom": "http://maven.apache.org/POM/4.0.0"}
    etree.register_namespace('pom', 'http://maven.apache.org/POM/4.0.0')

    try:
        if not os.path.isdir(".git"):
            raise ExitException(1, "That script must be ran from root of a Git repository")
        usage = "usage: %prog [options] <command=prepare|perform>"
        parser = optparse.OptionParser(usage=usage, description="""Release Nuxeo from
        a given branch, tag the release, then set the next SNAPSHOT version.
        If a maintenance version was provided, then a maintenance branch is kept, else
        it is deleted after release.""")
        parser.add_option('-r', action="store", type="string", dest='remote_alias',
                          default='origin',
                          help='the Git alias of remote URL (default: %default)')
        parser.add_option('-f', '--final', action="store_true", dest='is_final',
                          default=False,
                          help='is it a final release? (default: %default)')
        parser.add_option("-b", "--branch", action="store", type="string",
                          help='branch to release (default: current branch)',
                          dest="branch", default=None)
        parser.add_option("-t", "--tag", action="store", type="string",
                          help="""if final option is True, then the default tag is the
        current version minus '-SNAPSHOT', else the 'SNAPSHOT' keyword is replaced with
        a date (aka 'date-based release')""", dest="tag", default=None)
        parser.add_option("-n", "--next", action="store", type="string",
                          help="""next snapshot. If final option is True, then the
        next snapshot is the current one increased, else it is equal to the current""",
                          dest="next_snapshot", default=None)
        parser.add_option('-m', '--maintenance', action="store", dest='maintenance',
                          help="""maintenance version (by default, the maintenance
                          branch is deleted after release)""", default=None)
        parser.add_option('--mvn_opts', action="store", dest='mvn_opts',
                          help="Maven options (default: '%default')", default='')
        parser.add_option('-i', '--interactive', action="store_true",
                          dest='interactive', default=False,
                          help="""Not implemented (TODO NXP-8573). Interactive mode.""")
        (options, args) = parser.parse_args()
        if len(args) > 0:
            command = args[0]
        mvn_opts = options.mvn_opts
        if options.branch is None:
            options.branch = get_current_version()
        repo = Repository(os.getcwd(), options.remote_alias)
        system("git fetch %s" % (options.remote_alias))
        repo.git_update(options.branch)
        release = Release(repo, options.branch, options.tag,
                          options.next_snapshot, options.maintenance,
                          options.is_final)
        release.log_summary()
        if "command" not in locals():
            raise ExitException(1, "Missing command. See usage for commands list.")
        elif command == "prepare":
            release.prepare()
        elif command == "perform":
            release.perform()
        elif command == "test":
            release.test()
        else:
            raise ExitException(1, "Unknown command! See usage for commands list.")
    except ExitException, e:
        if e.message is not None:
            log("[ERROR] %s" % e.message, sys.stderr)
        sys.exit (e.return_code)
    finally:
        if "repo" in locals():
            repo.cleanup()

if __name__ == '__main__':
    main()
