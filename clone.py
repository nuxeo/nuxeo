#!/usr/bin/env python
##
## (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import re, os, sys, shlex, subprocess, platform, urllib, posixpath, time, optparse

driveletter = None
basedir = os.getcwd()

def log(message):
    sys.stdout.write(message + os.linesep)
    sys.stdout.flush()

def system(cmd, failonerror=True):
    log("$> " + cmd)
    args = shlex.split(cmd)
    p = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out, err = p.communicate()
    sys.stdout.write(out)
    sys.stdout.flush()
    retcode = p.returncode
    if retcode != 0:
        log("[ERROR]: command returned non-zero exit code: %s" % cmd)
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
            log("Error executing %s - retrying in 10 seconds..." % cmd)
            time.sleep(10)

def long_path_workaround_init():
    global driveletter
    # On Windows, try to map the current directory to an unused drive letter to shorten path names
    if platform.system() != "Windows": return
    for letter in "GHIJKLMNOPQRSTUVWXYZ":
        if not os.path.isdir("%s:\\" % (letter,)):
            driveletter = letter
            cwd = os.getcwd()
            system("SUBST %s: \"%s\"" % (driveletter, cwd))
            time.sleep(10)
            os.chdir("%s:\\" % (driveletter,))
            break

def long_path_workaround_cleanup():
    global driveletter
    if driveletter != None:
        os.chdir(basedir)
        system("SUBST %s: /D" % (driveletter,), False)

def check_output(cmd):
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = p.communicate()
    if err != None:
        log("[ERROR]: command", str(cmd), " returned an error:")
        log(err)
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
        system("git clone %s" % (repo_url))
        os.chdir(module)

    if version in check_output(["git", "tag"]).split():
        # the version is a tag name
        system("git checkout %s" % version)
    elif version not in check_output(["git", "branch"]).split():
        # create the local branch if missing
        system("git checkout -b %s %s/%s" % (version, alias, version))
    else:
        # reuse local branch
        system("git checkout %s" % version)
        log("Updating branch")
        system("git merge %s/%s" % (alias, version))
    os.chdir(cwd)
    log("")

def get_current_version():
    t = check_output(["git", "describe", "--all"]).split("/")
    return t[1]


long_path_workaround_init()

parser = optparse.OptionParser(description='Clone or update Nuxeo source code from Git repositories.')
parser.add_option('-r', action="store", type="string", dest='remote_alias', default='origin', help='The Git alias of remote URL (default: %default)')
parser.add_option("-a", "--all", action="store_true", dest="with_optionals", default=False, help="Include 'optional' addons (default: %default)")

(options, args) = parser.parse_args()
alias = options.remote_alias
with_optionals = options.with_optionals
if len(args) == 0:
    version = get_current_version()
elif len(args) == 1:
    version = args[0]
else:
    log("Error: version must be a single argument")
    sys.exit(1)

log("Cloning/updating parent pom")
system("git fetch %s" % (alias))
if version in check_output(["git", "tag"]).split():
    # the version is a tag name
    system("git checkout %s" % version)
elif version not in check_output(["git", "branch"]).split():
    # create the local branch if missing
    system("git checkout -b %s %s/%s" % (version, alias, version))
else:
    # reuse local branch
    system("git checkout %s" % version)
    log("Updating branch")
    system("git merge %s/%s" % (alias, version))
log("")

# find the remote URL
remote_lines = check_output(["git", "remote", "-v"]).split("\n")
for remote_line in remote_lines:
    remote_alias, remote_url, _ = remote_line.split()
    if alias == remote_alias:
        break

is_online = remote_url.endswith("/nuxeo.git")
if is_online:
    url_pattern = re.sub("(.*)nuxeo", r"\1module", remote_url)
else:
    url_pattern = remote_url + "/module"

log("Using maven introspection of the pom.xml files"
    " to find the list of sub-repositories")
for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    module = m.group(1)
    git_fetch(module)

cwd = os.getcwd()
log("$> cd addons; ./clone.py -r %s %s" % (alias, version))
os.chdir("addons")
if with_optionals:
    retcode = os.system("python clone.py -a -r %s %s" % (alias, version))
else:
    retcode = os.system("python clone.py -r %s %s" % (alias, version))
os.chdir(cwd)
if retcode != 0:
    log("[ERROR]: cloning addons failed.")
    sys.exit(retcode)

long_path_workaround_cleanup()
