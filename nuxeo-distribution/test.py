#!/usr/bin/python

import os, sys, pexpect, urllib, time, getopt

# Use a local copy as it is not included in Debian
import curl
from optparse import OptionParser

VERBOSE = False
USAGE = """%prog [options]"""

# Some utility methods

def flush():
    sys.stdout.flush()

def system(cmd):
    print cmd
    sys.stdout.flush()
    status = os.system(cmd)
    if status != 0:
        sys.exit(1)

def mvn(args):
    if VERBOSE:
        system("mvn " + args)
    else:
        system("mvn -q " + args)

def clean():
    #mvn("clean")
    system("rm -rf test")

def fileExists(path):
   try:
       open(path)
       return True
   except:
       return False

def getZipFileFrom(path):
    """Return a zip file in the path or raise."""
    files = os.listdir(path)
    zipfiles = [f for f in files if f.endswith('.zip')]
    assert zipfiles, "No zip file found in %s." % path
    return zipfiles[0]

def consoleTest(p):
    print "Running basic console tests"
    flush()
    p.logfile = sys.stdout
    p.expect("Framework started in", 120)
    p.sendline("ls")
    p.expect("default-domain")
    p.sendline("cd default-domain")
    p.sendline("ls")
    p.expect("workspaces")
    p.sendline("quit")
    p.close(force=True)

def waitForServer(timeout=60):
    t0 = time.time()
    while True:
        time.sleep(1)
        try:
            log = open("server.log").read()
        except IOError:
            log = ""
        if "Framework started in" in log:
            ok = True
            break
        if time.time() - t0 > timeout:
            ok = False
            break
    assert ok
    # Just in case...
    time.sleep(5)

# Test scripts

def testCore():
    print "## Testing 'core' packaging"
    flush()

    clean()
    #mvn("install package -P core")
    print "Testing result of 'package -P core'"
    zipfile = getZipFileFrom('nuxeo-distribution-server/target/')

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-distribution-server/target/" + zipfile)

    print "Starting server, and running short tests"
    flush()

    p = pexpect.spawn("sh nxserver.sh -console", timeout=120)
    consoleTest(p)

    os.chdir("..")

def testShell():
    print "## Testing 'nxshell' packaging"
    flush()

    clean()
    #mvn("install package -P shell")
    print "Testing result of 'package -P shell'"

    zipfile = getZipFileFrom("nuxeo-distribution-shell/target/")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-distribution-shell/target/" + zipfile)
    os.chdir("nxshell")
    print "Starting server, and running short tests"
    flush()
    p = pexpect.spawn("sh nxclient.sh -console", timeout=120)
    p.logfile = sys.stdout
    p.expect("Framework started in")
    # No real tests since there is no real server to connect to (for now).
    p.sendline("quit")
    p.expect("Bye.")
    p.close(force=True)
    os.chdir("../..")

def testJetty():
    print "## Testing 'jetty' packaging"
    flush()

    clean()
    #mvn("install package -P jetty")
    print "Testing result of 'package -P jetty'"

    zipfile = getZipFileFrom("nuxeo-distribution-jetty-ep/target/")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-distribution-jetty-ep/target/" + zipfile)
    os.chdir("nxserver")

    p = pexpect.spawn("sh nxserver.sh -console", timeout=120)
    consoleTest(p)

    print "Starting server"
    flush()

    cmd = "sh nxserver.sh -console > server.log 2>&1"
    p = pexpect.spawn("sh", ["-c", cmd], timeout=120)
    waitForServer(timeout=120)

    print "Browsing a few pages"
    flush()

    c = curl.Curl()
    c.set_timeout(60)

    data = c.get("http://localhost:8080/")
    assert "Welcome to Nuxeo WebEngine!" in data

    # FIXME: disabled for now.
    #data = c.get("http://localhost:8080/help")
    #assert "Nuxeo WebEngine Documentation" in data

    #data = c.get("http://localhost:8080/about")
    #assert "License:" in data
    #assert "Team:" in data
    #assert "Modules:" in data

    p.sendline("quit")
    p.close(force=True)

    os.chdir("../..")

def testGf3():
    print "## Testing 'gf3' packaging"
    flush()

    clean()
    #mvn("install package -P gf3")
    print "Testing result of 'package -P gf3'"

    zipfile = getZipFileFrom("nuxeo-distribution-gf3/target/")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-distribution-gf3/target/" + zipfile)
    os.chdir("nxserver")

    p = pexpect.spawn("sh nxserver.sh -console", timeout=120)
    consoleTest(p)

    time.sleep(10)
    print "Starting server"
    flush()

    cmd = "sh nxserver.sh -console > server.log 2>&1"
    p = pexpect.spawn("sh", ["-c", cmd], timeout=1200)
    waitForServer(timeout=1200)

    print "Browsing a few pages"
    flush()

    c = curl.Curl()
    c.set_timeout(60)

    data = c.get("http://localhost:8080/")
    assert "Welcome to Nuxeo WebEngine." in data

    # FIXME: disabled for now.
    #data = c.get("http://localhost:8080/help")
    #assert "Nuxeo WebEngine Documentation" in data

    #data = c.get("http://localhost:8080/about")
    #assert "License:" in data
    #assert "Team:" in data
    #assert "Modules:" in data

    p.sendline("quit")
    p.close(force=True)
    os.chdir("../..")
    print "done"


def main(argv):
    parser = OptionParser(USAGE)
    parser.add_option("-v", "--verbose", action="store_true",
                      help="Verbose output", default=False)
    parser.add_option("-P", "--profile", type="string",
                      help="Test mvn profile.", default=None)
    parser.add_option("--java-home-for-gf3", dest="java_home", type="string",
                      help="The Java 6 home used for the gf3 profile.")
    options, args = parser.parse_args(argv)
    if options.verbose:
        global VERBOSE
        VERBOSE = True
    if options.java_home and options.profile.lower == 'gf3':
        # GF3 requires JAVA 6
        os.environ["JAVA_HOME"] = options.java_home
    if options.profile:
        method_name = 'test' + options.profile.capitalize()
        if globals().has_key(method_name):
            ret = globals()[method_name]()
        else:
            print "Profile %s(%s) not found." % (options.profile,
                                                 method_name)


    flush()

if __name__ == '__main__':
    main(sys.argv)
