#!/usr/bin/env python
##
## (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
##
## All rights reserved. This program and the accompanying materials
## are made available under the terms of the GNU Lesser General Public License
## (LGPL) version 2.1 which accompanies this distribution, and is available at
## http://www.gnu.org/licenses/lgpl.html
##
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## Contributors:
##     Julien Carsique
##
## Utilities for Python scripts.
##
import optparse
import os
import platform
import re
import shlex
import subprocess
import sys
import time


class ExitException(Exception):
    def __init__(self, return_code):
        self.return_code = return_code


class Repository(object):
    def __init__(self, basedir, alias):
        assert_git_config()
        self.driveletter = long_path_workaround_init()
        self.basedir = basedir
        self.alias = alias
        # find the remote URL
        remote_lines = check_output(["git", "remote", "-v"]).split("\n")
        for remote_line in remote_lines:
            remote_alias, remote_url, _ = remote_line.split()
            if alias == remote_alias:
                break

        self.is_online = remote_url.endswith("/nuxeo.git")
        if self.is_online:
            self.url_pattern = re.sub("(.*)nuxeo", r"\1module", remote_url)
        else:
            self.url_pattern = remote_url + "/module"
        self.modules = []
        self.addons = []

    def cleanup(self):
        if hasattr(self, "driveletter"):
            long_path_workaround_cleanup(self.driveletter, self.basedir)

    def eval_modules(self):
        os.chdir(self.basedir)
        self.modules = []
        log("Using maven introspection of the pom.xml files"
            " to find the list of sub-repositories")
        for line in os.popen("mvn -N help:effective-pom"):
            line = line.strip()
            m = re.match("<module>(.*?)</module>", line)
            if not m:
                continue
            self.modules.append(m.group(1))

    def eval_addons(self, with_optionals=False):
        os.chdir(os.path.join(self.basedir, "addons"))
        self.addons = []
        log("Using maven introspection of the pom.xml files"
            " to find the list of addons")
        all_lines = os.popen("mvn -N help:effective-pom").readlines()
        if with_optionals:
            all_lines += os.popen("mvn -N help:effective-pom -f pom-optionals.xml").readlines()
        for line in all_lines:
            line = line.strip()
            m = re.match("<module>(.*?)</module>", line)
            if not m:
                continue
            self.addons.append(m.group(1))

    def git_pull(self, module, version):
        repo_url = self.url_pattern.replace("module", module)
        cwd = os.getcwd()
        if os.path.isdir(module):
            log("Updating " + module + "...")
            os.chdir(module)
            system("git fetch %s" % (self.alias))
        else:
            log("Cloning " + module + "...")
            system("git clone %s" % (repo_url))
            os.chdir(module)
        self.git_update(version)
        os.chdir(cwd)

    def system_recurse(self, command):
        """Execute the given command on all repositories"""
        cwd = os.getcwd()
        os.chdir(self.basedir)
        system(command)
        if not self.modules:
            self.eval_modules()
        for module in self.modules:
            os.chdir(os.path.join(self.basedir, module))
            system(command)
        if not self.addons:
            self.eval_addons()
        for addon in self.addons:
            os.chdir(os.path.join(self.basedir, "addons", addon))
            system(command)
        os.chdir(cwd)

    def git_update(self, version):
        if version in check_output(["git", "tag"]).split():
            # the version is a tag name
            system("git checkout %s" % version)
        elif version not in check_output(["git", "branch"]).split():
            # create the local branch if missing
            system("git checkout --track -b %s %s/%s" % (version, self.alias, version))
        else:
            # reuse local branch
            system("git checkout %s" % version)
            log("Updating branch")
            system("git rebase %s/%s" % (self.alias, version))
        log("")

    def clone(self, version, with_optionals=False):
        log("Cloning/updating parent pom")
        system("git fetch %s" % (self.alias))
        self.git_update(version)

        # Main modules
        self.eval_modules()
        for module in self.modules:
            self.git_pull(module, version)

        # Addons
        cwd = os.getcwd()
        os.chdir(os.path.join(self.basedir, "addons"))
        self.eval_addons(with_optionals)
        if not self.is_online:
            self.url_pattern = self.url_pattern.replace("module", "addons/module")
        for addon in self.addons:
            self.git_pull(addon, version)
        if not self.is_online:
            self.url_pattern = self.url_pattern.replace("addons/module", "module")
        os.chdir(cwd)


def log(message, out=sys.stdout):
    out.write(message + os.linesep)
    out.flush()


def system(cmd, failonerror=True):
    log("$> " + cmd)
    args = shlex.split(cmd)
    p = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT)
    out, err = p.communicate()
    sys.stdout.write(out)
    sys.stdout.flush()
    retcode = p.returncode
    if retcode != 0:
        log("[ERROR] Command returned non-zero exit code: %s" % cmd, sys.stderr)
        if failonerror:
            raise ExitException(retcode)
    return retcode


def system_with_retries(cmd, failonerror=True):
    retries = 0
    while True:
        retries += 1
        retcode = system(cmd, False)
        if retcode == 0:
            return 0
        elif retries > 10:
            return system(cmd, failonerror)
        else:
            log("Error executing %s - retrying in 10 seconds..." % cmd, sys.stderr)
            time.sleep(10)


def long_path_workaround_init():
    # On Windows, try to map the current directory to an unused drive letter to
    # shorten path names
    if platform.system() != "Windows":
        return
    for letter in "GHIJKLMNOPQRSTUVWXYZ":
        if not os.path.isdir("%s:\\" % (letter,)):
            driveletter = letter
            cwd = os.getcwd()
            system("SUBST %s: \"%s\"" % (driveletter, cwd))
            time.sleep(10)
            os.chdir("%s:\\" % (driveletter,))
            break
    return driveletter


def long_path_workaround_cleanup(driveletter, basedir):
    if driveletter != None:
        os.chdir(basedir)
        system("SUBST %s: /D" % (driveletter,), False)


def check_output(cmd):
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = p.communicate()
    if err != None:
        log("[ERROR] Command", str(cmd), " returned an error:", sys.stderr)
        log(err, sys.stderr)
    return out.strip()


def get_current_version():
    t = check_output(["git", "describe", "--all"]).split("/")
    return t[1]


def assert_git_config():
    t = check_output(["git", "config", "--get", "color.branch"])
    if "always" in t:
        log("[ERROR] The git color mode should be auto not always, try:", sys.stderr)
        log(" git config --global color.branch auto", sys.stderr)
        log(" git config --global color.status auto", sys.stderr)
        raise ExitException(1)

