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
## This script clones or updates Nuxeo addons source code from Mercurial and GitHub
## repositories.
##

import re, os, sys, shlex, subprocess, platform, urllib, urlparse, posixpath, time

driveletter = "G"

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
    # On Windows, try to map the current directory to an unused drive letter to shorten path names
    if platform.system() != "Windows": return
    for letter in "GHIJKLMNOPQRSTUVWXYZ":
        if not os.path.isdir("%s:\\" % (letter,)):
            driveletter = letter
            cwd = os.getcwd()
            system("SUBST %s: \"%s\"" % (driveletter, cwd))
            os.chdir("%s:\\" % (driveletter,))
            break

def long_path_workaround_cleanup():
    if platform.system() != "Windows": return
    system("SUBST %s: /D" % (driveletter,), False)

def check_output(cmd):
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = p.communicate()
    if err != None:
        log("[ERROR]: command", str(cmd), " returned an error:")
        log(err)
    return out.strip()


def hg_fetch(module, branch):
    cwd = os.getcwd()
    if os.path.isdir(module):
        log("Updating " + module + "...")
        os.chdir(module)
        system_with_retries("hg pull")
    else:
        log("Cloning " + module + "...")
        system_with_retries("hg clone %s/%s %s" % (hg_url, module, module))
        os.chdir(module)
    system("hg up %s" % branch)
    os.chdir(cwd)
    log("")


def git_fetch(module, branch):
    cwd = os.getcwd()
    if git_url.startswith("git@github.com"):
        repo_url = "%s/%s.git" % (git_url, module)
        # backward compliance with repositories cloned with HTTP URL
        repo_url_http = "%s/%s.git" % (git_url_http, module)
    else:
        repo_url = "%s/%s" % (git_url, module)
    if os.path.isdir(module):
        log("Updating " + module + "...")
    else:
        log("Cloning " + module + "...")
        retcode = system("git clone %s %s" % (repo_url, module), False)
        if retcode != 0:
            log("[WARN]: you need a GitHub account to clone from " + repo_url)
            system("git clone %s %s" % (repo_url_http, module))

    os.chdir(module)

    # find the nuxeo repo alias
    remote_lines = check_output(["git", "remote", "-v"]).split("\n")
    alias = None
    for remote_line in remote_lines:
        remote_alias, remote_url, _ = remote_line.split()
        if repo_url == remote_url:
            alias = remote_alias
            log("Using alias '%s' for %s" % (alias, repo_url))
            break
        elif repo_url_http == remote_url:
            log("[WARN]: fallback on %s (%s is recommended for contributing)." % (repo_url_http, repo_url))
            alias = remote_alias
            log("Using alias '%s' for %s" % (alias, repo_url_http))
            break
    if alias is None:
        raise ValueError("Failed to find remote repository alias for " + repo_url)

    # check whether we should use a specific branch or the master
    # (assumed to be the main development branch for git repos)
    if branch not in check_output(["git", "ls-remote", alias]).split("/"):
        #log(branch + " not found on remote repo: fallback on master.")
        branch = "master"

    # the branch is a tag name
    if branch in check_output(["git", "tag"]).split():
        system("git checkout %s" % branch)
    # create the local branch if missing
    elif branch not in check_output(["git", "branch"]).split():
        system("git checkout -b %s %s/%s" % (branch, alias, branch))
    # reuse local branch
    else:
        system("git checkout %s" % branch)
        log("Updating branch")
        system("git pull %s %s" % (alias, branch))

    os.chdir(cwd)
    log("")


def url_normpath(url):
    parsed = urlparse.urlparse(url)
    if parsed.path == "":
        path = ""
    else:
        path = posixpath.normpath(parsed.path)
    return urlparse.urlunparse(parsed[:2] + (path,) + parsed[3:])


long_path_workaround_init()

if len(sys.argv) > 1:
    branch = sys.argv[1]
else:
    t = check_output(["hg", "id", "-bt"]).split()
    branch = t[0]
    if (len(t) > 1):
        tag = t[1]
    if 'tag' in globals() and tag != "tip":
        branch = tag

log("Cloning/updating addons pom")
system("hg pull")
system("hg up %s" % branch)
log("")

hg_url = url_normpath(check_output(["hg", "path", "default"]))
if hg_url.startswith("http"):
    git_url_http = url_normpath(hg_url.replace("hg.nuxeo.org/addons", "github.com/nuxeo"))
    # prefer use of git@github.com:nuxeo/addon.git instead of https://github.com/nuxeo/addon.git
    git_url = "git@github.com:nuxeo"
else:
    # use filesystem path as URL
    git_url = hg_url

retries = 0
while True:
    retries += 1
    lines = urllib.urlopen("https://hg.nuxeo.org/?sort=name").readlines()
    if len(lines) > 1000: break
    if retries > 10:
        log("[ERROR]: could not retrieve addons list from hg.nuxeo.org - giving up.")
        sys.exit(-1)
    log("Error retrieving addons list from hg.nuxeo.org - retrying in 10 seconds...")
    time.sleep(10)

hg_addons = []
for line in lines:
    if not line.startswith("<b>addons/"):
        continue
    hg_addon = line[len("<b>addons/"):-len("</b>\n")]
    hg_addons.append(hg_addon)
#log(hg_addons)

log("Using maven introspection of the pom.xml files"
    " to find the list of addons")
all_lines = os.popen("mvn -N help:effective-pom").readlines()
all_lines += os.popen("mvn -N help:effective-pom -f pom-optionals.xml").readlines()

for line in all_lines:
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    addon = m.group(1)
    if addon in hg_addons:
        hg_fetch(addon, branch)
    else:
        git_fetch(addon, branch)

long_path_workaround_cleanup()

