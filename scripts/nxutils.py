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
from zipfile import ZIP_DEFLATED
from zipfile import ZipFile
import optparse
import os
import platform
import re
import shlex
import subprocess
import sys
import time
import shutil


class ExitException(Exception):
    def __init__(self, return_code, message=None):
        self.return_code = return_code
        self.message = message


class Repository(object):
    """Nuxeo repository manager.

    Provides recursive Git and Shell functions."""

    def __init__(self, basedir, alias):
        assert_git_config()
        (self.basedir, self.driveletter,
         self.oldbasedir) = long_path_workaround_init(basedir)
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
        long_path_workaround_cleanup(self.driveletter, self.oldbasedir)

    def eval_modules(self):
        """Set the list of Nuxeo addons in 'self.modules'."""
        os.chdir(self.basedir)
        self.modules = []
        log("Using Maven introspection of the POM file"
            " to find the list of modules...")
        for line in os.popen("mvn -N help:effective-pom"):
            line = line.strip()
            m = re.match("<module>(.*?)</module>", line)
            if not m:
                continue
            self.modules.append(m.group(1))

    def eval_addons(self, with_optionals=False):
        """Set the list of Nuxeo addons in 'self.addons'.

        If 'with_optionals', add "optional" addons to the list."""
        os.chdir(os.path.join(self.basedir, "addons"))
        self.addons = []
        log("Using Maven introspection of the POM files"
            " to find the list of addons...")
        all_lines = os.popen("mvn -N help:effective-pom").readlines()
        if with_optionals:
            all_lines += os.popen("mvn -N help:effective-pom " +
                                  "-f pom-optionals.xml").readlines()
        for line in all_lines:
            line = line.strip()
            m = re.match("<module>(.*?)</module>", line)
            if not m:
                continue
            self.addons.append(m.group(1))

    def git_pull(self, module, version, fallback_branch=None):
        """Git clone or fetch, then update.

        'module': the Git module to run on.
        'version': the version to checkout.
        'fallback_branch': the branch to fallback on when 'version' is not
        found locally or remotely."""
        repo_url = self.url_pattern.replace("module", module)
        cwd = os.getcwd()
        log("[%s]" % module)
        if os.path.isdir(module):
            os.chdir(module)
            system_with_retries("git fetch %s" % (self.alias))
        else:
            system_with_retries("git clone %s" % (repo_url))
            os.chdir(module)
        self.git_update(version, fallback_branch)
        os.chdir(cwd)

    def system_recurse(self, command, with_optionals=False):
        """Execute the given command on current and sub-repositories.

        'command': the command to execute.
        If 'with_optionals', also recurse on "optional" addons."""
        cwd = os.getcwd()
        os.chdir(self.basedir)
        log("[.]")
        system(command)
        if not self.modules:
            self.eval_modules()
        for module in self.modules:
            os.chdir(os.path.join(self.basedir, module))
            log("[%s]" % module)
            system(command)
        if not self.addons:
            self.eval_addons(with_optionals)
        for addon in self.addons:
            os.chdir(os.path.join(self.basedir, "addons", addon))
            log("[%s]" % addon)
            system(command)
        os.chdir(cwd)

    def archive(self, archive, version=None, with_optionals=False):
        """Archive the sources of current and sub-repositories.

        'archive': full path of archive to generate.
        'version': version to archive, defaults to current version.
        If 'with_optionals', also recurse on "optional" addons."""
        if version is None:
            version = self.get_current_version()
        archive_dir = os.path.join(os.path.dirname(archive), "sources")
        cwd = os.getcwd()
        os.chdir(self.basedir)
        if os.path.isdir(archive_dir):
            shutil.rmtree(archive_dir)
        os.mkdir(archive_dir)
        log("[.]")
        p = system("git archive %s" % version, run=False)
        system("tar -C %s -xf -" % archive_dir, stdin=p.stdout)
        if not self.modules:
            self.eval_modules()
        for module in self.modules:
            os.chdir(os.path.join(self.basedir, module))
            log("[%s]" % module)
            p = system("git archive --prefix=%s/ %s" % (module, version),
                       run=False)
            system("tar -C %s -xf -" % archive_dir, stdin=p.stdout)
        if not self.addons:
            self.eval_addons(with_optionals)
        for addon in self.addons:
            os.chdir(os.path.join(self.basedir, "addons", addon))
            log("[%s]" % addon)
            p = system("git archive --prefix=addons/%s/ %s" % (addon, version),
                     run=False)
            system("tar -C %s -xf -" % archive_dir, stdin=p.stdout)
        make_zip(archive, archive_dir)
        shutil.rmtree(archive_dir)
        os.chdir(cwd)

    def git_update(self, version, fallback_branch=None):
        """Git update using checkout, stash (if needed) and rebase.

        'version': the version to checkout.
        'fallback_branch': the branch to fallback on when 'version' is not
        found locally or remotely."""
        if version in check_output(["git", "tag"]).split():
            # the version is a tag name
            system("git checkout %s" % version)
        elif version not in check_output(["git", "branch"]).split():
            # create the local branch if missing
            retcode = system("git checkout --track -b %s %s/%s" % (version,
                                                        self.alias, version),
                   fallback_branch is None)
            if retcode != 0 and fallback_branch is not None:
                log("Branch %s not found, fallback on %s" % (version,
                                                             fallback_branch))
                self.git_update(fallback_branch)
        else:
            # reuse local branch
            system("git checkout %s" % version)
            retcode = system("git rebase %s/%s" % (self.alias, version), False)
            if retcode != 0:
                system("git stash")
                system("git rebase %s/%s" % (self.alias, version))
                system("git stash pop -q")
        log("")

    def clone(self, version=None, fallback_branch=None, with_optionals=False):
        """Clone or update whole Nuxeo repository.

        'version': the version to checkout; defaults to current version.
        'fallback_branch': the branch to fallback on when 'version' is not
        found locally or remotely.
        If 'with_optionals', also clone/update "optional" addons."""
        cwd = os.getcwd()
        os.chdir(self.basedir)
        log("[.]")
        system_with_retries("git fetch %s" % (self.alias))
        if version is None:
            version = self.get_current_version()
        self.git_update(version, fallback_branch)

        # Main modules
        self.eval_modules()
        for module in self.modules:
            self.git_pull(module, version, fallback_branch)

        # Addons
        os.chdir(os.path.join(self.basedir, "addons"))
        self.eval_addons(with_optionals)
        if not self.is_online:
            self.url_pattern = self.url_pattern.replace("module",
                                                        "addons/module")
        for addon in self.addons:
            self.git_pull(addon, version, fallback_branch)
        if not self.is_online:
            self.url_pattern = self.url_pattern.replace("addons/module",
                                                        "module")
        os.chdir(cwd)

    def get_current_version(self):
        """Return branch or tag version of current Git workspace."""
        t = check_output(["git", "describe", "--all"]).split("/")
        return t[-1]

    def mvn(self, commands, skip_tests=False, profiles=None):
        """Run Maven commands (install, package, deploy, ...) on the whole
        sources (including addons and all distributions) with the given
        parameters.

        'commands': the commands to run.
        'skip_tests': whether to skip or not the tests.
        'profiles': comma-separated additional Maven profiles to use."""
        skip_tests_param = ""
        if skip_tests:
            skip_tests_param = "-DskipTests=true"
        profiles_param = ""
        if profiles is not None:
            profiles_param = "-P%s" % profiles
        system("mvn %s %s -Paddons,distrib,all-distributions %s" %
               (commands, skip_tests_param, profiles_param),
               delay_stdout=False)


def log(message, out=sys.stdout):
    out.write(message + os.linesep)
    out.flush()


def system(cmd, failonerror=True, delay_stdout=True, run=True,
           stdin=subprocess.PIPE, stdout=subprocess.PIPE):
    """Shell execution.

    'cmd': the command to execute.
    If 'failonerror', command execution failure raises an ExitException.
    If 'delay_stdout', output is flushed at the end of command execution.
    If not 'run', the process is not executed but returned.
    'stdin', 'stdout' are only used if 'delay_stdout' is True."""
    log("$> " + cmd)
    args = shlex.split(cmd)
    if delay_stdout:
        p = subprocess.Popen(args, stdin=stdin, stdout=stdout,
                             stderr=subprocess.STDOUT)
        if run:
            out, err = p.communicate()
            sys.stdout.write(out)
            sys.stdout.flush()
    else:
        p = subprocess.Popen(args)
        if run:
            p.wait()
    if not run:
        return p
    retcode = p.returncode
    if retcode != 0:
        if failonerror:
            raise ExitException(retcode,
                                "Command returned non-zero exit code: %s"
                                % cmd)
    return retcode


def system_with_retries(cmd, failonerror=True):
    """Shell execution with ten retries in case of failures.

    'cmd': the command to execute.
    If 'failonerror', latest command execution failure raises an ExitException.
    """
    retries = 0
    while True:
        retries += 1
        retcode = system(cmd, failonerror=False)
        if retcode == 0:
            return 0
        elif retries > 10:
            return system(cmd, failonerror=failonerror)
        else:
            log("Error executing %s - retrying in 10 seconds..." % cmd,
                sys.stderr)
            time.sleep(10)


def long_path_workaround_init(basedir):
    """Windows only. Try to map the 'basedir' to an unused drive letter
    to shorten path names."""
    newdir = basedir
    driveletter = None
    if platform.system() == "Windows":
        for letter in "GHIJKLMNOPQRSTUVWXYZ":
            if not os.path.isdir("%s:\\" % (letter,)):
                system("SUBST %s: \"%s\"" % (letter, basedir))
                time.sleep(10)
                driveletter = letter
                newdir = driveletter + ":\\"
                break
    return newdir, driveletter, basedir


def long_path_workaround_cleanup(driveletter, basedir):
    """Windows only. Cleanup the directory mapping if any."""
    if driveletter != None:
        os.chdir(basedir)
        system("SUBST %s: /D" % (driveletter,), failonerror=False)


def check_output(cmd):
    """Return Shell command output."""
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = p.communicate()
    if err != None:
        log("[ERROR] Command", str(cmd), " returned an error:", sys.stderr)
        log(err, sys.stderr)
    return out.strip()


def assert_git_config():
    """Check Git configuration."""
    t = check_output(["git", "config", "--get", "color.branch"])
    if "always" in t:
        raise ExitException(1, "The git color mode must not be always, try:" +
                            "\n git config --global color.branch auto" +
                            "\n git config --global color.status auto")


def make_zip(archive, rootdir=None, basedir=None, mode="w"):
    """Create a zip file from all the files under 'rootdir'/'basedir'.

    If 'rootdir' is not specified, it uses the current directory.
    If 'basedir' is not specified, it uses the current directory constant '.'.
    The 'mode' must be 'w' (write) or 'a' (append)."""
    cwd = os.getcwd()
    if rootdir is not None:
        os.chdir(rootdir)
    try:
        if basedir is None:
            basedir = os.curdir
        log("Creating %s with %s ..." % (archive, basedir))
        zip = ZipFile(archive, mode, compression=ZIP_DEFLATED)
        for dirpath, dirnames, filenames in os.walk(basedir):
            for name in filenames:
                path = os.path.normpath(os.path.join(dirpath, name))
                if os.path.isfile(path):
                    zip.write(path, path)
                    log("Adding %s" % path)
        zip.close()
    finally:
        if rootdir is not None:
            os.chdir(cwd)


def extract_zip(archive, outdir=None):
    """Extract a zip file.

    Extracts all the files to the 'outdir' directory (defaults to current dir)
    """
    zip = ZipFile(archive, "r")
    if outdir is None:
        outdir = os.getcwd()
    zip.extractall(outdir)
    zip.close()
