#!/usr/bin/python

import os, sys, pexpect

def system(cmd):
    print cmd
    status = os.system(cmd)
    if status != 0:
        sys.exit(status)

def mvn(args):
    system("mvn " + args)

def clean():
    mvn("clean")
    system("rm -rf test")

# 

print "## Testing 'core' packaging"

clean()
mvn("package -P core")
system("mkdir test")
os.chdir("test")

system("unzip -q ../nuxeo-server-template/target/nuxeo-app.zip")
child = pexpect.spawn("sh nxserver.sh -console")
assert 0 == child.expect("Framework started in")
child.sendline("ls")
assert 0 == child.expect("default-domain")
child.sendline("cd default-domain")
child.sendline("ls")
assert 0 == child.expect("workspaces")
child.sendline("quit")

os.chdir("..")

#

print "## Testing 'nxshell' packaging"
clean()
mvn("package -P nxshell")

#

print "## Testing 'jetty' packaging"
clean()
mvn("package -P jetty")

# TODO: add some interactions here

# 

# TODO: not working yet
#print "## Testing 'gf3' packaging"
#clean()
#mvn("package -P gf3")
