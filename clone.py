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

import re, os, sys, subprocess, urllib
#from pprint import pprint

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

def hg_fetch(module):
    cwd = os.getcwd()
    if os.path.isdir(module):
        print "Updating " + module + "..."
        os.chdir(module)
        system("hg pull")
    else:
        print "Cloning " + module + "..."
        system("hg clone %s/%s %s" % (hg_url, module, module))
        os.chdir(module)
    system("hg up %s" % (branch))
    os.chdir(cwd)
    print

def git_fetch(module):
    cwd = os.getcwd()
    if os.path.isdir(module):
        print "Updating " + module + "..."
        os.chdir(module)
        system("git fetch")
    else:
        print "Cloning " + module
        if git_url.startswith("http"):
            system("git clone %s/%s.git %s" % (git_url, module, module))
        else:
            system("git clone %s/%s %s" % (git_url, module, module))
        os.chdir(module)
    retcode = system("git checkout %s" % (branch))
    if retcode != 0:
        print branch + " not found. Fallback on master"
        system("git checkout %s" % ("master"))
    os.chdir(cwd)
    print

if len(sys.argv) > 1:
    branch = sys.argv[1]
else:
    branch = check_output(["hg", "id", "-b"])

system("hg pull")
system("hg up %s" % branch)
print

hg_url = check_output(["hg", "path", "default"])
if hg_url.startswith("http"):
    git_url = hg_url.replace("hg.nuxeo.org/addons", "github.com/nuxeo")
else:
    git_url = hg_url

lines = urllib.urlopen("https://hg.nuxeo.org/?sort=name").readlines()
hg_addons = []
for line in lines:
    if not line.startswith("<b>addons/"):
        continue
    hg_addon = line[len("<b>addons/"):-len("</b>\n")]
    hg_addons.append(hg_addon)
#print hg_addons

all_lines = os.popen("mvn -N help:effective-pom").readlines()
all_lines += os.popen("mvn -N help:effective-pom -f pom-optionals.xml").readlines()

for line in all_lines:
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    addon = m.group(1)
    if addon in hg_addons:
        hg_fetch(addon)
    else:
        git_fetch(addon)
