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

import re, os, sys, subprocess

def system(cmd):
    print "$> " + cmd
    retcode = os.system(cmd)

def check_output(cmd):
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = p.communicate()
    if err != None:
        print "[ERROR]: command", str(cmd), " returned an error:"
        print err
    return out.strip()

def fetch(module):
    cwd = os.getcwd()
    if os.path.isdir(module):
        print "Updating " + module + "..."
        os.chdir(module)
        system("hg pull")
    else:
        print "Cloning " + module + "..."
        system("hg clone %s/%s %s" % (root_url, module, module))
        os.chdir(module)
    system("hg up %s" % (branch))
    os.chdir(cwd)
    print

if len(sys.argv) > 1:
    branch = sys.argv[1]
else:
    branch = check_output(["hg", "id", "-b"])

system("hg pull")
system("hg up %s" % branch)
print

root_url = check_output(["hg", "path", "default"])

for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    module = m.group(1)
    fetch(module)

fetch("nuxeo-distribution")
if root_url.startswith("http"):
    root_url = root_url.replace("/nuxeo", "")
fetch("addons")

cwd = os.getcwd()
os.chdir("addons")
system("python clone.py %s" % branch)
os.chdir(cwd)

