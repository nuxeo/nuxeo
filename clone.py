#!/usr/bin/env python

# Convenience script to call the correct clone script depending on the platform
# This allows us to launch the cloning process on all platforms with the same command

import platform, os, sys, subprocess

Platform_String = platform.system()
platform_string = Platform_String.lower()

print "Platform:", Platform_String

script_dir = os.path.dirname(os.path.abspath(sys.argv[0]))

if platform_string.find("windows")!=-1 or platform_string.find("cygwin")!=-1:
    proc = os.path.join(script_dir, "clone.bat")
else:
    proc = os.path.join(script_dir, "clone.sh")

p = subprocess.Popen(proc, cwd=script_dir)
stdout, stderr = p.communicate()

