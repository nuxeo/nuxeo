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
##     Julien Carsique
##
## This script clones or updates Nuxeo source code from Mercurial repositories.
##

import re, os, sys, shlex, subprocess, urlparse, posixpath

def log(message):
    sys.stdout.write(message + os.linesep)
    sys.stdout.flush()

def system(cmd):
    log("$> " + cmd)
    args = shlex.split(cmd)
    p = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out, err = p.communicate()
    sys.stdout.write(out)
    sys.stdout.flush()
    retcode = p.returncode
    if retcode != 0:
        log("Command returned non-zero exit code: %s" % (cmd,))
        sys.exit(retcode)

def check_output(cmd):
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = p.communicate()
    if err != None:
        log("[ERROR]: command", str(cmd), " returned an error:")
        log(err)
    return out.strip()

def fetch(module):
    cwd = os.getcwd()
    if os.path.isdir(module):
        log("Updating " + module + "...")
        os.chdir(module)
        system("hg pull")
    else:
        log("Cloning " + module + "...")
        system("hg clone %s/%s %s" % (root_url, module, module))
        os.chdir(module)
    system("hg up %s" % (branch))
    os.chdir(cwd)
    log("")

if len(sys.argv) > 1:
    branch = sys.argv[1]
else:
    t = check_output(["hg", "id", "-bt"]).split()
    branch = t[0]
    if (len(t) > 1):
        tag = t[1]
    if 'tag' in globals() and tag != "tip":
        branch = tag

def url_normpath(url):
    parsed = urlparse.urlparse(url)
    if parsed.path == "":
        path = ""
    else:
        path = posixpath.normpath(parsed.path)
    return urlparse.urlunparse(parsed[:2] + (path,) + parsed[3:])


log("Cloning/updating parent pom")
system("hg pull")
system("hg up %s" % branch)
log("")

root_url = url_normpath(check_output(["hg", "path", "default"]))

log("Using maven introspection of the pom.xml files"
    " to find the list of sub-repositories")
for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    module = m.group(1)
    fetch(module)

fetch("nuxeo-distribution")

if root_url.startswith("http"):
    root_url = url_normpath(root_url.replace("/nuxeo", ""))
fetch("addons")

cwd = os.getcwd()
os.chdir("addons")
log("$> cd addons; clone.py %s" % branch)
retcode = os.system("python clone.py %s" % branch)
if retcode != 0:
    log("Command returned non-zero exit code: %s" % (cmd,))
    sys.exit(retcode)
os.chdir(cwd)

