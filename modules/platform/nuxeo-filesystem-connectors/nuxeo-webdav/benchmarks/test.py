#!/usr/bin/env python

ROOT = "/Volumes/workspaces/"
ROOT = "/Volumes/default/"
MY_STUFF = "/Users/fermigier/Music/"

import random, time, os, string, threading

from multiprocessing import Process

def testFolders(n):
    test_folder = ROOT + str(time.time()) + "-" + str(random.randint(0, 1000000))
    os.mkdir(test_folder)
    for i in range(0, n):
        fn = fn = test_folder + "/" + str(i)
        os.mkdir(fn)
        os.rmdir(fn)

        os.mkdir(fn)
        os.mkdir(fn + "/subfolder")
        os.rmdir(fn + "/subfolder")
        os.rmdir(fn)


def testSimpleText(n):
    testWithGenerator(n, randomText)

def testBinary(n):
    testWithGenerator(n, randomBinary)

def testWithGenerator(n, g):
    test_folder = ROOT + str(time.time()) + "-" + str(random.randint(0, 1000000))
    os.mkdir(test_folder)
    for i in range(0, n):
        fn = test_folder + "/" + str(i) + ".txt"
        print "Creating file: " + fn

        data = g(i * 1000)

        fd = open(fn, "wc")
        fd.write(data)
        fd.close()
        data2 = open(fn).read()
        assert data == data2

        fd = open(fn, "wc")
        fd.write(data)
        fd.close()
        data2 = open(fn).read()
        assert data == data2

        fn2 = test_folder + "/" + str(i) + "-1.txt"
        os.rename(fn, fn2)
        data2 = open(fn2).read()
        assert data == data2
        os.rename(fn2, fn)

        os.unlink(fn)
    os.rmdir(test_folder)

def testSimpleTextParallel(n, p):
    def slave():
        testSimpleText(n)
    threads = []
    for i in range(0, p):
        t = threading.Thread(target=slave)
        t.start()
        threads.append(t)
    for t in threads:
        t.join()

def testSimpleTextMultiProcess(n, p):
    processes = []
    for i in range(0, p):
        t = Process(target=testSimpleText, args=(n,))
        t.start()
        processes.append(t)
    for t in processes:
        t.join()


def testWithExistingContent(source, max):
    test_folder = ROOT + str(time.time()) + "-" + str(random.randint(0, 1000000))
    os.mkdir(test_folder)
    files = os.popen("find %s -type f" % source).readlines()
    i = 0
    for sourcepath in files:
        if i > max:
            break
        sourcepath = sourcepath.strip()
        data = open(sourcepath).read()
        fn = test_folder + "/" + os.path.basename(sourcepath)
        print "Creating file: %s of size: %d kB, from: %s" % (fn, len(data)/1000, sourcepath)
        fd = open(fn, "wc")
        fd.write(data)
        fd.close()
        data2 = open(fn).read()
        assert data == data2
        os.unlink(fn)
        i += 1
    os.rmdir(test_folder)

def testWithExistingContentMultiProcess(source, max, p):
    processes = []
    for i in range(0, p):
        t = Process(target=testWithExistingContent, args=(source, max))
        t.start()
        processes.append(t)
    for t in processes:
        t.join()

def randomText(n):
    chars = string.letters + string.digits + string.punctuation + " "
    l = [ random.choice(chars) for i in range(0, n) ]
    return "".join(l)


def randomBinary(n):
    chars = [ chr(i) for i in range(0, 256) ]
    l = [ random.choice(chars) for i in range(0, n) ]
    return "".join(l)


def main():
    N = 10

    print "Test folders"
    testFolders(N)

    print "Test simple text files, serial"
    testSimpleText(N)

    print "Test binary files, serial"
    testBinary(N)

    print "Test simple text files, parallel (x2)"
    testSimpleTextMultiProcess(N, 2)

    print "Test simple text files, parallel (x10)"
    testSimpleTextMultiProcess(N/10, 10)

    print "Test simple text files, parallel (x100)"
    testSimpleTextMultiProcess(N/100, 100)

    print "Test existing (big) files, serial"
    testWithExistingContent(MY_STUFF, N)

    print "Test existing (big) files, parellel (x10)"
    testWithExistingContentMultiProcess(MY_STUFF, N, 10)

if __name__ == "__main__":
    main()
