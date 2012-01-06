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
from nxutils import get_current_version
from nxutils import log
from nxutils import system
import fnmatch
import optparse
import os
import posixpath
import re
import subprocess
import sys
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

    def test(self, root, name):
        tree = etree.parse(os.path.join(root, name))
        # Parent POM version
        parent = tree.getroot().find("pom:parent", namespaces)
        if parent is not None:
            elem = parent.find("pom:version", namespaces)
            if elem is not None and elem.text == self.snapshot:
                elem.text = self.tag
        # POM version
        elem = tree.getroot().find("version")
        if elem is not None and elem.text == self.snapshot:
            elem.text = self.tag
        # Properties
        prop_pattern = re.compile("{" + namespaces.get("pom") + "}nuxeo\..*version")
        properties = tree.getroot().find("pom:properties", namespaces)
        if properties is not None:
            for property in properties.getchildren():
                log(property)
                if (not isinstance(property, etree._Comment)
                    and prop_pattern.match(property.tag)
                    and property.text == self.snapshot):
                    property.text = self.tag
        log(os.path.join(root, name))
        tree.write_c14n(os.path.join(root, name))

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
        # TODO NXP-8570 packaging
        # TODO NXP-8571 package sources
        os.chdir(cwd)

    def perform(self):
        """ Perform the release: push source, deploy artifacts and upload
        packages"""
        cwd = os.getcwd()
        os.chdir(self.repo.basedir)
        self.repo.clone(self.branch)
        self.repo.system_recurse("git push --all")
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

    if not os.path.isdir(".git"):
        log("That script must be ran from root of a Git repository", sys.stderr)
        sys.exit(1)
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
    parser.add_option("-b", "--branch", action="store", type="string", default=None,
                      help='branch to release (default: current branch)',
                      dest="branch")
    parser.add_option("-t", "--tag", action="store", type="string", default=None,
                      help="""if final option is True, then the default tag is the
    current version minus '-SNAPSHOT', else the 'SNAPSHOT' keyword is replaced with
    a date (aka 'date-based release')""", dest="tag")
    parser.add_option("-n", "--next", action="store", type="string", default=None,
                      help="""next snapshot. If final option is True, then the
    next snapshot is the current one increased, else it is equal to the current""",
                      dest="next_snapshot")
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
    try:
        repo = Repository(os.getcwd(), options.remote_alias)
        system("git fetch %s" % (options.remote_alias))
        repo.git_update(options.branch)
        release = Release(repo, options.branch, options.tag,
                          options.next_snapshot, options.maintenance,
                          options.is_final)
        release.log_summary()
        if "command" not in locals():
            log("[ERROR] Missing command: prepare or perform", sys.stderr)
            sys.exit(1)
        elif command == "prepare":
            release.prepare()
        elif command == "perform":
            release.perform()
        #elif command == "test":
        #release.test("/tmp/nuxeo", "pom.xml")
        else:
            log("[ERROR] Unknown command! Available commands: 'prepare', 'perform'",
                sys.stderr)
            sys.exit(1)
    except ExitException, e:
        sys.exit (e.return_code)
    finally:
        repo.cleanup()

if __name__ == '__main__':
    main()
