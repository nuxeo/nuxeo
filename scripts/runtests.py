#!/usr/bin/env python

import os, sys, glob

TEST_DIR = "nuxeo-distribution/nuxeo-distribution-dm/ftest/selenium"
DISTRIB_TARGETS = {
    'tomcat': "nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-*-SNAPSHOT-tomcat",
    'cap': "nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-cap-*-SNAPSHOT-tomcat",
}

if len(sys.argv) == 1:
    distrib = "tomcat"
else:
    distrib = sys.argv[1]

target_dir = glob.glob(DISTRIB_TARGETS[distrib])[0]

def system(cmd):
    print cmd
    os.system(cmd)

def start_server():
    system(target_dir + "/bin/nuxeoctl start")

def stop_server():
    system(target_dir + "/bin/nuxeoctl stop")

def run_tests():
    if os.path.isfile("/usr/bin/xvfb-run"):
        status = system("cd %s ;" % TEST_DIR + "HIDE_FF=true xvfb-run ./run.sh")
    else:
        status = system("cd %s ;" % TEST_DIR + "HIDE_FF=true ./run.sh")

def main():
    status = 0
    try:
        start_server()
        status = run_tests()
    finally:
        stop_server()
    sys.exit(status)

main()

