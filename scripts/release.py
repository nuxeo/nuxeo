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
from nxutils import assert_git_config
from nxutils import check_output
from nxutils import get_current_version
from nxutils import log
from nxutils import system
from datetime import datetime
import optparse
import os
import posixpath
import re
import subprocess
import sys
import time
import urllib
import xml.etree.ElementTree as etree


class Release(object):
    def __init__(self, repo, branch, snapshot, tag, next_snapshot, maintenance):
        self.repo = repo
        self.branch = branch
        self.snapshot = snapshot
        self.tag = tag
        self.next_snapshot = next_snapshot
        self.maintenance = maintenance

    def log_summary(self):
        log("Releasing from branch:".ljust(25) + branch)
        log("Current version:".ljust(25) + snapshot)
        log("Tag:".ljust(25) + tag)
        log("Next version:".ljust(25) + next_snapshot)
        if maintenance is None:
            log("No maintenance branch".ljust(25))
        else:
            log("Maintenance version:".ljust(25) + maintenance)

    def prepare(self):
        """ Prepare the release: build, change versions, tag and package source and
        distributions"""

        # check release-ability
        self.check()

        # ./clone.py branch supposed as already done
        # Checkout branches de release
        # update versions
        # commit
        # tag
        # if maintenance:
            # update version
            # commit
        # checkout master
        # if not maintenance:
            # delete branch de release
        # update versions
        # commit

        # checkout tag
        # mvn install
        # packaging
        # package sources

        # Synchronize repositories between slaves

    def perform(self):
        """ Perform the release: push source, deploy artifacts and upload
        packages"""
        # gitf push --all
        # gitf push --tags
        # checkout tag
        # deploy


    def check(self):
        """ Check the release is feasible"""
        # tag do not already exist



def get_current_snapshot():
    tree = etree.parse(os.path.join(basedir, "pom.xml"))
    version_elem = tree.getroot().find(ns_maven + "version")
    return version_elem.text


def get_tag(snapshot):
    if is_final:
        return snapshot.partition("-SNAPSHOT")[0]
    else:
        date = datetime.now().strftime("%Y%m%d_%H%M")
        return snapshot.replace("-SNAPSHOT", "-I" + date)


def get_next_snapshot():
    if is_final:
        snapshot_split = re.match("(^.*)(\d+)(-SNAPSHOT$)", snapshot)
        return (snapshot_split.group(1)
                + str(int(snapshot_split.group(2)) + 1)  # increment minor
                + snapshot_split.group(3))
    else:
        return snapshot


#def main():
basedir = os.getcwd()
assert_git_config()
if not os.path.isdir(".git"):
    log("That script must be ran from root of a Git repository", sys.stderr)

ns_maven = "{http://maven.apache.org/POM/4.0.0}"

usage = "usage: %prog [options]"
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
(options, args) = parser.parse_args()
alias = options.remote_alias
is_final = options.is_final
maintenance = options.maintenance
branch = options.branch
next_snapshot = options.next_snapshot
tag = options.tag

"""Eval default values"""
if branch is None:
    branch = get_current_version()
else:
    retcode = os.system("python clone.py -r %s %s" % (alias, branch))
snapshot = get_current_snapshot()
if tag is None:
    tag = get_tag(snapshot)
if next_snapshot is None:
    next_snapshot = get_next_snapshot()

repo = Repository(alias)
release = Release(repo, branch, snapshot, tag, next_snapshot, maintenance)
release.log_summary()
release.prepare()


## Merge maintenance branch on master before changing versions


#if __name__ == '__main__':
#    main()
