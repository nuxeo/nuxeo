#!/usr/bin/env python
##
## (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
## This script clone or update Nuxeo source code from Mercurial and GitHub
## repositories.
##

import re, os, sys, commands

def system(cmd):
    print "$> " + cmd
    retcode = os.system(cmd)

def fetch(module, root_url=None):
    if root_url is None:
        fetch(module, "https://hg.nuxeo.org/nuxeo")
        return

    if os.path.isdir(module):
        print "Updating " + module + "..."
        cwd = os.getcwd()
        os.chdir(module)
        system("hg pull")
        os.chdir(cwd)
    else:
        print "Cloning " + module + "..."
        system("hg clone %s/%s %s" % (root_url, module, module))
    cwd = os.getcwd()
    os.chdir(module)
    system("hg up %s" % (branch))
    os.chdir(cwd)
    print

if len(sys.argv) == 2:
    branch = sys.argv[1]
else:
    branch = commands.getoutput("hg id -b")

system("hg pull")
system("hg up %s" % branch)
print
for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    module = m.group(1)
    fetch(module)

fetch("nuxeo-distribution")
fetch("addons", "https://hg.nuxeo.org")

system("cd addons ; python clone.py %s" % branch)
