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
from scripts.nxutils import assert_git_config
from scripts.nxutils import check_output
from scripts.nxutils import get_current_version
from scripts.nxutils import log
from scripts.nxutils import long_path_workaround_cleanup
from scripts.nxutils import long_path_workaround_init
from scripts.nxutils import system
from scripts.nxutils import Repository
import optparse
import os
import platform
import re
import shlex
import subprocess
import sys
import time


basedir = os.getcwd()
assert_git_config()
driveletter = long_path_workaround_init()

usage = "usage: %prog [options] version"
parser = optparse.OptionParser(
    usage=usage,
    description='Clone or update Nuxeo source code from Git repositories.')
parser.add_option(
    '-r', action="store", type="string", dest='remote_alias',
    default='origin', help='The Git alias of remote URL (default: %default)')
parser.add_option(
    "-a", "--all", action="store_true", dest="with_optionals", default=False,
    help="Include 'optional' addons (default: %default)")

(options, args) = parser.parse_args()
alias = options.remote_alias
with_optionals = options.with_optionals
if len(args) == 0:
    version = get_current_version()
elif len(args) == 1:
    version = args[0]
else:
    log("[ERROR]: version must be a single argument", sys.stderr)
    sys.exit(1)

repo = Repository(alias)
repo.clone(version, with_optionals)

long_path_workaround_cleanup(driveletter)
