#!/usr/bin/env python
##
## (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
##     Stefane Fermigier
##     Julien Carsique
##
## This script clones or updates Nuxeo source code from Git repositories.
##
from scripts.nxutils import ExitException
from scripts.nxutils import Repository
from scripts.nxutils import assert_git_config
from scripts.nxutils import check_output
from scripts.nxutils import log
from scripts.nxutils import long_path_workaround_cleanup
from scripts.nxutils import long_path_workaround_init
from scripts.nxutils import system
import optparse
import os
import platform
import re
import shlex
import subprocess
import sys
import time


def main():
    try:
        usage = "usage: %prog [options] version"
        parser = optparse.OptionParser(usage=usage, description=
"""clone or update Nuxeo source code.""")
        parser.add_option('-r', action="store", type="string",
                          dest='remote_alias', default='origin', help=
"""the Git alias of remote URL (default: %default)""")
        parser.add_option("-a", "--all", action="store_true",
                          dest="with_optionals", default=False,
                          help="include 'optional' addons (default: %default)")
        parser.add_option('-f', "--fallback", action="store", type="string",
                          dest='fallback_branch', default=None, help=
"""a branch to fallback on when the wanted branch doesn't exist locally neither
remotely (default: %default)""")
        parser.add_option('-n', "--nodrivemapping", action="store_true",
                          dest='no_drive_mapping', default=False, 
                          help="desactivate current directory mapping to a virtual drive on Windows")

        (options, args) = parser.parse_args()
        repo = Repository(os.getcwd(), options.remote_alias, not options.no_drive_mapping)
        if len(args) == 0:
            version = None
        elif len(args) == 1:
            version = args[0]
        else:
            raise ExitException(1, "'version' must be a single argument. "
                                "See usage with '-h'.")
        repo.clone(version, options.fallback_branch, options.with_optionals)
    except ExitException, e:
        if e.message is not None:
            log("[ERROR] %s" % e.message, sys.stderr)
        sys.exit(e.return_code)
    finally:
        if "repo" in locals():
            repo.cleanup()

if __name__ == '__main__':
    main()
