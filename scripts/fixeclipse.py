#!/usr/bin/env python
##
## (C) Copyright 2011-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl-2.1.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Bash script ./fixeclipse translated to fixeclipse.py
## to make it usable from windows command line.
##
## Contributors:
##    Edgar Geisler
##    Julien Carsique
##
from os import path, walk, devnull
from re import compile, match, M as Multiline
from shutil import copy
from subprocess import call


MAX_DEPTH = 4


def get_main_pattern():
    """Creates compiled multiline pattern for MAIN

    returns Tuple of patterns"""
    return (compile('(\spath=\")' + OLD_MAIN + '(\")', flags=Multiline),
            r'\1' + NEW_MAIN + r'\2')


def get_test_pattern():
    """Creates compiled multiline pattern for TEST

    returns Tuple of patterns"""
    return (compile('(\soutput=\")' + OLD_TEST + '(\")', flags=Multiline),
            r'\1' + NEW_TEST + r'\2')


#pylint: disable=C0103
def read_file_content(filename):
    """Reads file content.

    filename: File name
    returns File content as string
    exception IOError"""
    content = None
    try:
        f = open(filename, "rb")
        content = f.read()
    except IOError, e:
        msg = "Cannot read file content from " + filename
        if e.message is not None:
            msg = msg + ": " + e.message
        print msg
        raise
    finally:
        f.close()
    return content


def write_file_content(filename, content):
    """Reads file content.

    filename: File name
    content: New file content
    exception IOError"""
    try:
        f = open(filename, "wb")
        f.write(content)
    except IOError, e:
        msg = "Cannot write file content to " + filename
        if e.message is not None:
            msg = msg + ": " + e.message
        print msg
        raise
    finally:
        f.close()


def find_files_by_pattern(pattern):
    """Creates file name generator.

    Starts recursively collecting files from the current directory.
    pattern: File name pattern
    returns File collection generator"""
    for root, dirs, files in walk('.'):
        if root.count(path.sep) >= MAX_DEPTH:
            del dirs[:]
        for name in files:
            if match(pattern, name) is not None:
                yield path.join(root, name)


def fix_classpath():
    """Fixes .classpath files"""
    print "Fixing Eclipse classpath to use bin instead of target directory..."

    test_pattern = get_test_pattern()
    main_pattern = get_main_pattern()

    # For each '.classpath' file...
    for cpfile in find_files_by_pattern('\.classpath$'):
        try:
            content = read_file_content(cpfile)

            # Replace OLD_MAIN by NEW_MAIN
            content = main_pattern[0].subn(main_pattern[1], content)
            changed = content[1] > 0

            # Replace OLD_TEST by NEW_TEST
            content = test_pattern[0].subn(test_pattern[1], content[0])
            changed = changed or content[1] > 0

            # Content has been changed
            if changed:
                write_file_content(cpfile, content[0])
                print cpfile

        except IOError:
            print "Cannot process file " + cpfile

    print "Done."


def replace_files():
    """Replaces missing or badly generated files"""
    print "Replacing missing or badly generated files with .*.ok files..."

    try:
        # Redirect diff output to null device.
        nulldevice = open(devnull, "w")

        # For each .*.ok file...
        for okfile in find_files_by_pattern('\.[^\.]+\.ok$'):
            # Base file name without '.ok' extension
            newfile = okfile[:-3]

            # Replace file if something's changed.
            # Diff utility is expected (not pre-installed on Windows!)
            # http://gnuwin32.sourceforge.net/packages/diffutils.htm
            # FIXME: use python difflib instead of forcing (Windows) users to
            #          install diff utilities.
            cmd = ['diff', '-qbB', okfile, newfile]
            if 0 != call(cmd, stdout=nulldevice, stderr=nulldevice,
                         shell=False):
                copy(okfile, newfile)
                print newfile

    finally:
        nulldevice.close()

    print "Done."

if __name__ == '__main__':
    OLD_MAIN = "target/classes"
    NEW_MAIN = "bin/main"
    OLD_TEST = "target/test-classes"
    NEW_TEST = "bin/test"
    fix_classpath()
    replace_files()
