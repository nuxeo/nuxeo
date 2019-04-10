#!/usr/bin/env python
"""
(C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Stefane Fermigier
    Julien Carsique

This script clones or updates Nuxeo addons source code from Git repositories"""

import errno
import sys
import os
import re
import shlex
import subprocess
import platform
import time
import optparse
from distutils.version import LooseVersion


REQUIRED_GIT_VERSION = "1.8.4"


class ExitException(Exception):
    def __init__(self, return_code, message=None):
        self.return_code = return_code
        self.message = message


# pylint: disable=C0103
driveletter = None
basedir = os.getcwd()


def log(message, out=sys.stdout):
    out.write(message + os.linesep)
    out.flush()


# pylint: disable=C0103
def system(cmd, failonerror=True):
    """Shell execution.

    'cmd': the command to execute.
    If 'failonerror', command execution failure raises an ExitException."""
    log("$> " + cmd)
    cmdargs = shlex.split(cmd)
    p = subprocess.Popen(cmdargs, stdin=subprocess.PIPE,
                         stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out, _ = p.communicate()
    sys.stdout.write(out)
    sys.stdout.flush()
    retcode = p.returncode
    if retcode != 0:
        log("[ERROR]: command returned non-zero exit code: %s" % cmd,
            sys.stderr)
        if failonerror:
            long_path_workaround_cleanup()
            sys.exit(retcode)
    return retcode


def system_with_retries(cmd, failonerror=True):
    retries = 0
    while True:
        retries += 1
        retcode = system(cmd, False)
        if retcode == 0:
            return 0
        elif retries > 10:
            return system(cmd, failonerror)
        else:
            log("Error executing %s - retrying in 10 seconds..." % cmd,
                sys.stderr)
            time.sleep(10)


def long_path_workaround_init():
    global driveletter
    global basedir
    # map the current directory to an unused drive letter to shorten path names
    if platform.system() != "Windows" or len(basedir) < 20:
        return
    for letter in "GHIJKLMNOPQRSTUVWXYZ":
        if not os.path.isdir("%s:\\" % (letter,)):
            driveletter = letter
            cwd = os.getcwd()
            system("SUBST %s: \"%s\"" % (driveletter, cwd))
            time.sleep(10)
            os.chdir("%s:\\" % (driveletter,))
            break


def long_path_workaround_cleanup():
    """Windows only. Cleanup the directory mapping if any."""
    global driveletter
    if driveletter is not None:
        os.chdir(basedir)
        system("SUBST %s: /D" % (driveletter,), failonerror=False)


def check_output(cmd):
    """Return Shell command output."""
    try:
        p = subprocess.Popen(cmd, stdin=subprocess.PIPE,
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                             shell=platform.system() == "Windows")
    # pylint: disable=C0103
    except OSError, e:
        log("$> " + str(cmd))
        if e.errno == errno.ENOENT:
            raise ExitException(1, "Command not found: '%s'" % cmd[0])
        else:
            # re-raise unexpected exception
            raise
    out, err = p.communicate()
    retcode = p.returncode
    if retcode != 0:
        if err is None or err == "":
            err = out
        raise ExitException(retcode,
                            "Command '%s' returned non-zero exit code (%s)\n%s"
                            % (str(cmd), retcode, err))
    return out.strip()


def git_fetch(module):
    repo_url = url_pattern.replace("module", module)
    cwd = os.getcwd()
    if os.path.isdir(module):
        log("Updating " + module + "...")
        os.chdir(module)
        system("git fetch %s" % (alias))
    else:
        log("Cloning " + module + "...")
        system("git clone %s --origin %s" % (repo_url, alias))
        os.chdir(module)
    git_update()
    os.chdir(cwd)


def git_update():
    """Git update using checkout, stash (if needed) and rebase."""
    is_tag = version in check_output(["git", "tag", "--list", version]).split()
    is_local_branch = version in check_output(["git", "branch", "--list", version]).split()
    is_remote_branch = "%s/%s" % (alias, version) in check_output([
                       "git", "branch", "-r", "--list", "%s/%s" % (alias, version)]).split()
    if is_tag:
        system("git checkout %s -q" % version)
    elif is_local_branch:
        system("git checkout %s -q" % version)
        if is_remote_branch:
            system("git rebase -q --autostash %s/%s" % (alias, version))
    elif is_remote_branch:
        system("git checkout --track -b %s %s/%s -q" % (version, alias, version))
    else:
        log("Branch %s not found" % version)
    log("")


def get_current_version():
    t = check_output(["git", "describe", "--all"]).split("/")
    return t[-1]


def assert_git_config():
    """Check Git configuration."""
    t = check_output(["git", "--version"]).split()[-1]
    if LooseVersion(t) < LooseVersion(REQUIRED_GIT_VERSION):
        raise ExitException(1, "Requires Git version %s+ (detected %s)" % (REQUIRED_GIT_VERSION, t))
    try:
        t = check_output(["git", "config", "--get-all", "color.branch"])
    except ExitException, e:
        # Error code 1 is fine (default value)
        if e.return_code > 1:
            log("[WARN] %s" % e.message, sys.stderr)
    try:
        t += check_output(["git", "config", "--get-all", "color.status"])
    except ExitException, e:
        # Error code 1 is fine (default value)
        if e.return_code > 1:
            log("[WARN] %s" % e.message, sys.stderr)
    if "always" in t:
        raise ExitException(1, "The git color mode must not be always, try:" +
                            "\n git config --global color.branch auto" +
                            "\n git config --global color.status auto")


assert_git_config()
long_path_workaround_init()

usage = "usage: %prog [options] version"
parser = optparse.OptionParser(
    usage=usage,
    description='Clone or update Nuxeo source code from Git repositories.')
parser.add_option(
    '-r', action="store", type="string", dest='remote_alias', default='origin',
    help='The Git alias of remote URL (default: %default)')
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

log("Cloning/updating addons parent pom")
system("git fetch %s" % (alias))
git_update()

# find the remote URL
remote_lines = check_output(["git", "remote", "-v"]).split("\n")
for remote_line in remote_lines:
    remote_alias, remote_url, _ = remote_line.split()
    if alias == remote_alias:
        break

is_online = remote_url.endswith("/addons.git")
if is_online:
    url_pattern = re.sub("(.*)addons", r"\1module", remote_url)
else:
    url_pattern = remote_url + "/module"

log("Using maven introspection of the pom.xml files"
    " to find the list of addons")
all_lines = os.popen("mvn -N help:effective-pom").readlines()
if with_optionals:
    all_lines += os.popen("mvn -N help:effective-pom -f pom-optionals.xml").\
        readlines()

for line in all_lines:
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    addon = m.group(1)
    git_fetch(addon)

long_path_workaround_cleanup()
