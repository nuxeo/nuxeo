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
##     Stefane Fermigier
##     Julien Carsique
##
## This script clone or update Nuxeo addons source code from Mercurial and GitHub
## repositories.
##

import re, os, sys, subprocess, urllib
#from pprint import pprint

git_url = "https://github.com/nuxeo"

def system(cmd):
    print "$> " + cmd
    retcode = os.system(cmd)

def hg_fetch(module, root_url=None):
    if root_url is None:
        hg_fetch(module, "https://hg.nuxeo.org/addons")
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

def git_fetch(module):
    if os.path.isdir(module):
        print "Updating " + module + "..."
        cwd = os.getcwd()
        os.chdir(module)
        retcode = system("git pull %s/%s.git %s" % (git_url, module, branch))
        os.chdir(cwd)
        if retcode != 0:
            cwd = os.getcwd()
            os.chdir(module)
            system("git pull %s/%s.git %s" % (git_url, module, "master"))
            os.chdir(cwd)
    else:
        print "Cloning " + module
        system("git clone -b %s %s/%s.git %s" % (branch, git_url, module, module))
    print

if len(sys.argv) == 2:
    branch = sys.argv[1]
else:
    branch = subprocess.check_output(["hg","id","-b"]).strip()

system("hg pull")
system("hg up %s" % branch)
print
lines = urllib.urlopen("https://hg.nuxeo.org/?sort=name").readlines()
hg_addons = []
for line in lines:
    if not line.startswith("<b>addons/"):
        continue
    hg_addon = line[len("<b>addons/"):-len("</b>\n")]
    hg_addons.append(hg_addon)
#print hg_addons

for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    addon = m.group(1)
    if addon in hg_addons:
        hg_fetch(addon)
    else:
        git_fetch(addon)
