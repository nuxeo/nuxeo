#!/usr/bin/env python

import re, os, sys

BRANCH = "5.4"

if (sys.argv) == 2:
    branch = sys.argv[1]
else:
    branch = BRANCH

def system(cmd):
    print cmd
    os.system(cmd)

for line in os.popen("mvn -N help:effective-pom"):
    line = line.strip()
    print line
    m = re.match("<module>(.*?)</module>", line)
    if not m:
        continue
    addon = m.group(1)

    if not os.path.isdir(addon):
        print "Cloning " + addon
        system("hg clone https://hg.nuxeo.org/addons/%s" % addon)
        system("cd %s ; hg up %s" % (addon, branch))


