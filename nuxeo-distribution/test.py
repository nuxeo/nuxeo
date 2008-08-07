#!/usr/bin/python

import os, sys, pexpect, urllib, time
# import curl

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
    system("mvn -q " + args)

def clean():
    mvn("clean")
    system("rm -rf test")

def fileExists(path):
   try:
       open(path)
       return True
   except:
       return False

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
    
# Test scripts 

def testCore():
    print "## Testing 'core' packaging"
    flush()

    clean()
    mvn("install package -P core")
    assert fileExists("nuxeo-server-template/target/nuxeo-app.zip")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-server-template/target/nuxeo-app.zip")

    print "Starting server, and running short tests"
    flush()

    p = pexpect.spawn("sh nxserver.sh -console", timeout=120)
    consoleTest(p)

    os.chdir("..")

def testShell():
    print "## Testing 'nxshell' packaging"
    flush()

    clean()
    mvn("install package -P nxshell")
    assert fileExists("nuxeo-shell-template/target/nuxeo-app.zip")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-shell-template/target/nuxeo-app.zip")
    os.chdir("nxshell")

    p = pexpect.spawn("sh nxclient.sh", timeout=120)
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
    mvn("install package -P jetty")
    assert fileExists("nuxeo-jetty-template/target/nuxeo-app.zip")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-jetty-template/target/nuxeo-app.zip")
    os.chdir("nxserver")

    p = pexpect.spawn("sh nxserver.sh -console", timeout=120)
    consoleTest(p)

    #print "Starting server"
    #flush()
    
    #p = pexpect.spawn("sh nxserver.sh -console")
    #p.logfile = sys.stdout
    #p.expect("Framework started in", 240)

    #c = curl.Curl()
    #c.set_timeout(60)
    #data = c.get("http://localhost:8080/")
    #print data
    
    #system("curl -m 10 http://localhost:8080/ > curl.res &")
    #p.read()
    
    #c = pexpect.spawn("curl -m 10 http://localhost:8080/")
    #c.close()

    #c = pexpect.spawn("curl -m 10 http://localhost:8080/docs/index.ftl")
    #c.expect("Nuxeo WebEngine")
    #c.close()

    #data = urllib.urlopen("http://localhost:8080/").read()
    #data = urllib.urlopen("http://localhost:8080/docs/index.ftl").read()
    #assert "Nuxeo WebEngine" in data
    #p.sendline("quit")
    #p.close(force=True)

    os.chdir("../..")

def testGF3():
    print "## Testing 'gf3' packaging"
    flush()

    clean()
    mvn("install package -P gf3")
    assert fileExists("nuxeo-gf3-template/target/nuxeo-app.zip")

    os.mkdir("test")
    os.chdir("test")
    system("unzip -q ../nuxeo-gf3-template/target/nuxeo-app.zip")
    os.chdir("nxserver")

    p = pexpect.spawn("sh nxserver.sh -console", timeout=120)
    consoleTest(p)

    #print "Starting server"
    #p = pexpect.spawn("sh nxserver.sh -console")
    #p.expect("Framework started in", 120)
    #data = urllib.urlopen("http://localhost:8080/").read()
    #data = urllib.urlopen("http://localhost:8080/docs/index.ftl").read()
    #assert "Nuxeo WebEngine" in data
    #p.sendline("quit")
    #p.close(force=True)

    os.chdir("../..")

#

testCore()
testShell()
testJetty()
testGF3()
print "The end"
flush()
