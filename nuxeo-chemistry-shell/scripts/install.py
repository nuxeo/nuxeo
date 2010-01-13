#!/usr/bin/python

import os, sys, shutil

DEFAULT_BIN = "/usr/local/bin"
DEFAULT_LIB = "/usr/local/lib/cmissh"

if sys.platform == 'darwin':
    PLATFORM = "Mac OS"
elif sys.platform == 'linux2':
    PLATFORM = "Linux"
else:
    PLATFORM = "Something else (%s)" % sys.platform

def expandTilde(path):
    if path.startswith("~"):
        return os.environ['HOME'] + path[1:]

print "Welcome to the CMIS Shell intaller."
print "It should work on Linux and Mac OS."
print "Please help with a Windows port if you can."
print

print "Note that this software is provided AS IS, with no expressed"
print "or implied warranty."
print

print "Your OS seems to be:", PLATFORM
print

msg = "Destination directory for executables?\n(Default: %s): " % DEFAULT_BIN
bin_dir = raw_input(msg).strip() or DEFAULT_BIN
bin_dir = expandTilde(bin_dir)

print

msg = "Destination directory for libraries?\n(Default: %s): " % DEFAULT_LIB
lib_dir = raw_input(msg).strip() or DEFAULT_LIB
lib_dir = expandTilde(lib_dir)

print "(If installation fails, you may want to rerun with sudo)."
print

print "Installing nuxeo-chemistry-shell.jar to %s..." % lib_dir
shutil.copy("nuxeo-chemistry-shell.jar", lib_dir + "/nuxeo-chemistry-shell.jar")

print "Installing cmissh to %s..." % bin_dir
target = open("%s/cmissh" % bin_dir, "wc")
for line in open("cmissh").readlines():
    if line.startswith("LIB_DIR"):
        target.write('LIB_DIR="%s"\n' % lib_dir)
    else:
        target.write(line)
shutil.copymode("cmissh", "%s/cmissh" % bin_dir)
