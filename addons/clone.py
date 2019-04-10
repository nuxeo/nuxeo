#!/usr/bin/env python

import re, os, sys

BRANCH = "release-5.4.1"

if len(sys.argv) == 2:
    branch = sys.argv[1]
else:
    branch = BRANCH

def system(cmd):
    print cmd
    os.system(cmd)

system("hg pull; hg up %s" % branch)
for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    addon = m.group(1)

    if os.path.isdir(addon):
        print "Updating " + addon
        system("hg pull -R %s" % addon)
    else:
        print "Cloning " + addon
        system("hg clone https://hg.nuxeo.org/addons/%s %s" % (addon,addon))
    system("cd %s ; hg up %s" % (addon, branch))
