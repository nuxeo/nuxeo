#!/usr/bin/env python
"""
(C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Julien Carsique

Utilities for Python scripts."""

import ConfigParser
import errno
import os
import platform
import re
import shlex
import shutil
import subprocess
import sys
import time
import urllib2
from zipfile import ZIP_DEFLATED, ZipFile
from distutils.version import LooseVersion
import warnings


REQUIRED_GIT_VERSION = "1.8.4"
SUPPORTED_GIT_ONLINE_URLS = "http://", "https://", "git://", "git@"
DEFAULT_MP_CONF_URL = "https://raw.github.com/nuxeo/integration-scripts/master/marketplace.ini"


class ExitException(Exception):
    def __init__(self, return_code, message=None):
        super(ExitException, self).__init__(message)
        self.return_code = return_code
        self.message = message

def deprecated(func):
    '''This is a decorator which can be used to mark functions
    as deprecated. It will result in a warning being emitted
    when the function is used.'''
    def new_func(*args, **kwargs):
        warnings.warn("Call to deprecated function {}.".format(func.__name__), category=DeprecationWarning)
        return func(*args, **kwargs)
    new_func.__name__ = func.__name__
    new_func.__doc__ = func.__doc__
    new_func.__dict__.update(func.__dict__)
    return new_func

# pylint: disable=R0902
class Repository(object):
    """Nuxeo repository manager.

    Provides recursive Git and Shell functions."""

    def __init__(self, basedir, alias, dirmapping=True, is_nuxeoecm=True):
        assert_git_config()
        (self.basedir, self.driveletter, self.oldbasedir) = long_path_workaround_init(basedir, dirmapping)
        self.mp_dir = os.path.join(self.basedir, "marketplace")
        if not os.path.isdir(self.mp_dir):
            os.mkdir(self.mp_dir)
        self.alias = alias
        # find the remote URL
        os.chdir(self.basedir)
        remote_lines = check_output("git remote -v").split("\n")
        for remote_line in remote_lines:
            remote_alias, remote_url, _ = remote_line.split()
            if alias == remote_alias:
                break

        self.is_online = remote_url.startswith(SUPPORTED_GIT_ONLINE_URLS)
        if self.is_online:
            self.url_pattern = re.sub("(.*)nuxeo", r"\1module", remote_url)
        else:
            self.url_pattern = remote_url + "/module"
        self.modules = []
        self.sub_modules = {}
        self.addons = []
        self.optional_addons = []
        self.is_nuxeoecm = is_nuxeoecm

    def cleanup(self):
        long_path_workaround_cleanup(self.driveletter, self.oldbasedir)

    # pylint: disable=C0103
    @deprecated
    def eval_modules(self):
        """Set the list of Nuxeo addons in 'self.modules'."""
        self.modules = self.retrieve_modules(self.basedir)

    @deprecated
    def eval_addons(self):
        """Set the list of Nuxeo addons in 'self.addons' and 'self.optional_addons'."""
        addons_dir = os.path.join(self.basedir, "addons")
        self.addons = self.retrieve_modules(addons_dir)
        self.optional_addons = self.retrieve_modules(addons_dir, "pom-optionals.xml")

    def execute_on_modules(self, function, with_optionals=False):
        """Executes the given function on each first and second level modules of Nuxeo repository.

        'function' the function to execute on module.
        'with_optionals' weither or not we execute function on optionals (modules with pom-optionals.xml file)
        """
        cwd = os.getcwd()
        os.chdir(self.basedir)
        if not self.modules and self.is_nuxeoecm:
            self.modules = self.retrieve_modules(self.basedir)
        # First level
        addons = ["addons", "addons-core"]
        for module in self.modules:
            function(module)
            # Second level - addons
            if module in addons:
                if not self.is_online:
                    self.url_pattern = self.url_pattern.replace("module", "%s/module" % module)
                os.chdir(module)
                if module not in self.sub_modules and self.is_nuxeoecm:
                    module_dir = os.path.join(self.basedir, module)
                    self.sub_modules[module] = self.retrieve_modules(module_dir)
                    # Handle optionals
                    if with_optionals:
                        self.sub_modules[module] = self.sub_modules[module] + self.retrieve_modules(module_dir,
                                                                                                    "pom-optionals.xml")
                for sub_module in self.sub_modules[module]:
                    function(sub_module)
                os.chdir(self.basedir)
                if not self.is_online:
                    self.url_pattern = self.url_pattern.replace("%s/module" % module, "module")
        os.chdir(cwd)

    @staticmethod
    def retrieve_modules(project_dir, pom_name = "pom.xml"):
        """Retrieve all modules of input Maven project and return it."""
        modules = []
        if os.path.exists(os.path.join(project_dir, pom_name)):
            log("Modules list calculated from the POM file %s/%s" % (project_dir, pom_name))
            cwd = os.getcwd()
            os.chdir(project_dir)
            f = open(pom_name, "r")
            pom_content = f.read()
            modules = re.findall("<module>(.*?)</module>", pom_content)
            f.close()
            os.chdir(cwd)
            modules = sorted(set(modules))
        return modules

    def git_pull(self, module, version, fallback_branch=None, rebase=False):
        """Git clone or fetch, then update and rebase.

        'module': the Git module to run on.
        'version': the version to checkout.
        'fallback_branch': the branch to fallback on when 'version' is not
        found locally or remotely.
        'rebase': rebase onto fallback_branch"""
        repo_url = self.url_pattern.replace("module", module)
        cwd = os.getcwd()
        log("[%s]" % module)
        if os.path.isdir(module):
            os.chdir(module)
            system_with_retries("git fetch %s" % (self.alias))
        else:
            system_with_retries("git clone %s --origin %s" % (repo_url, self.alias))
            os.chdir(module)
        self.git_update(version, fallback_branch, rebase)
        os.chdir(cwd)

    def system_recurse(self, command, with_optionals=False):
        """Execute the given command on current and sub-modules.

        'command': the command to execute.
        If 'with_optionals', also recurse on "optional" addons."""
        cwd = os.getcwd()
        os.chdir(self.basedir)
        log("[.]")
        system(command)
        self.execute_on_modules(lambda module: self.system_module(command, module), with_optionals)
        os.chdir(cwd)

    @staticmethod
    def system_module(command, module):
        """Execute the given command on given module.

        'command': the command to execute.
        'module': the module into execute command."""
        cwd = os.getcwd()
        os.chdir(module)
        log("[%s]" % module)
        system(command)
        os.chdir(cwd)

    def git_recurse(self, command, with_optionals=False):
        """Execute the given Git command on current and sub-modules. It ignores non Git repositories.

        'command': the command to execute.
        If 'with_optionals', also recurse on "optional" addons."""
        command = "git " + command
        cwd = os.getcwd()
        os.chdir(self.basedir)
        log("[.]")
        system(command)
        self.execute_on_modules(lambda module: self.git_module(command, module), with_optionals)
        os.chdir(cwd)

    @staticmethod
    def git_module(command, module):
        """Execute the given Shell command on the given module. It ignores non Git repositories.

        'command': the command to execute.
        'module': the Git sub-directory where to execute the command."""
        cwd = os.getcwd()
        os.chdir(module)
        if os.path.isdir(".git"):
            log("[%s]" % module)
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
        p = system("git archive %s" % version, wait=False)
        # pylint: disable=E1103
        system("tar -C %s -xf -" % archive_dir, stdin=p.stdout)
        self.execute_on_modules(lambda module: self.archive_module(archive_dir, version, module), with_optionals)
        make_zip(archive, archive_dir)
        shutil.rmtree(archive_dir)
        os.chdir(cwd)

    @staticmethod
    def archive_module(archive_dir, version, module):
        """Archive the sources of a the given Git sub-directory.

        'archive_dir': full path of archive to generate.
        'version': version to archive, defaults to current version.
        'module': the Git sub-directory to archive."""
        cwd = os.getcwd()
        os.chdir(module)
        log("[%s]" % module)
        p = system("git archive --prefix=%s/ %s" % (module, version), wait=False)
        system("tar -C %s -xf -" % archive_dir, stdin=p.stdout)
        os.chdir(cwd)

    def git_update(self, version, fallback_branch=None, rebase=False):
        """Git update using checkout, stash (if needed) and rebase.

        'version': the version to checkout.
        'fallback_branch': the branch to fallback on when 'version' is not
        found locally or remotely.
        'rebase': rebase onto fallback branch"""
        is_tag = version in check_output("git tag --list %s" % version).split()
        is_local_branch = version in check_output("git branch --list %s" % version).split()
        is_remote_branch = "%s/%s" % (self.alias, version) in check_output(
                           "git branch -r --list %s/%s" % (self.alias, version)).split()
        if is_tag:
            system("git checkout %s -q" % version)
        elif is_local_branch:
            system("git checkout %s -q" % version)
            if is_remote_branch:
                system("git rebase -q --autostash %s/%s" % (self.alias, version))
        elif is_remote_branch:
            system("git checkout --track -b %s %s/%s -q" % (version, self.alias, version), fallback_branch is None)
        elif fallback_branch:
            log("Branch %s not found, fallback on %s" % (version, fallback_branch))
            self.git_update(fallback_branch)
            return
        else:
            log("Branch %s not found" % version)
        if fallback_branch != None and rebase:
            system("git rebase -q --autostash %s/%s" % (self.alias, fallback_branch))
        log("")

    def get_mp_config(self, marketplace_conf, user_defaults = {}):
        """Return the Marketplace packages configuration."""
        defaults = {'other_versions': None, 'prepared': 'False', 'performed': 'False', 'branched': 'False',
                    "profiles": '', "auto_increment_policy": "auto_patch"}
        defaults.update(user_defaults)
        mp_config = ConfigParser.SafeConfigParser(defaults=defaults)
        if marketplace_conf is None:
            no_remote = True
        else:
            try:
                mp_config.readfp(urllib2.urlopen(marketplace_conf))
                no_remote = False
            except urllib2.URLError:
                no_remote = True
            except ValueError:
                no_remote = True
        mp_config = self.save_mp_config(mp_config, True, no_remote)
        return mp_config

    def save_mp_config(self, mp_config, read_first=False,
                       fail_if_no_file=False):
        """Save the Marketplace packages configuration."""
        configfile_path = os.path.join(self.mp_dir, "release.ini")
        if read_first and os.path.isfile(configfile_path):
            mp_config.read(configfile_path)
        if fail_if_no_file and not os.path.isfile(configfile_path):
            raise ExitException(1, "Missing configuration: '%s'" %
                                configfile_path)
        mp_config.write(open(configfile_path, 'w'))
        log("Configuration saved: " + configfile_path)
        return mp_config

    def clone_mp(self, marketplace_conf, fallback_branch=None):
        """Clone or update Nuxeo Package repositories.

        Returns the Nuxeo Packages configuration."""
        if marketplace_conf == '':
            marketplace_conf = DEFAULT_MP_CONF_URL
        if not marketplace_conf:
            return
        os.chdir(self.mp_dir)
        user_defaults={}
        if self.is_nuxeoecm:
            user_defaults["nuxeo-branch"] = self.get_current_version()
        mp_config = self.get_mp_config(marketplace_conf, user_defaults)
        for marketplace in mp_config.sections():
            self.git_pull(marketplace, mp_config.get(marketplace, "branch"), fallback_branch=fallback_branch)
        return mp_config

    def clone(self, version=None, fallback_branch=None, rebase=False, with_optionals=False,
              marketplace_conf=None):
        """Clone or update whole Nuxeo repository.

        'version': the version to checkout; defaults to current version.
        'rebase': rebase onto the fallback; defaults to false
        'fallback_branch': the branch to fallback on when 'version' is not
        found locally or remotely.
        If 'with_optionals', also clone/update "optional" addons.
        'marketplace_conf': URL of configuration file listing the Marketplace
        repositories to clone or update."""
        cwd = os.getcwd()
        os.chdir(self.basedir)
        log("[.]")
        system_with_retries("git fetch %s" % (self.alias))
        if version is None:
            version = self.get_current_version()
        self.git_update(version, fallback_branch, rebase)
        if self.is_nuxeoecm:
            self.execute_on_modules(lambda module: self.clone_module(module, version, fallback_branch, rebase), with_optionals)
            self.clone_mp(marketplace_conf, fallback_branch)
        os.chdir(cwd)

    def clone_module(self, module, version, fallback_branch, rebase):
        # Ignore modules which are not Git sub-repositories
        if not os.path.isdir(module) or os.path.isdir(os.path.join(module, ".git")):
            self.git_pull(module, version, fallback_branch, rebase)

    @staticmethod
    def get_current_version():
        """Return branch or tag version of current Git workspace."""
        t = check_output("git describe --all").split("/")
        return t[-1]

    def mvn(self, commands, skip_tests=False, skip_ITs=False,
            profiles=None, dryrun=False):
        """Run Maven commands (install, package, deploy, ...) on the whole
        sources (including addons and all distributions) with the given
        parameters.

        'commands': the commands to run.
        'skip_tests': whether to skip or not the tests.
        'skip_ITs': whether to skip or not the Integration Tests.
        'profiles': comma-separated additional Maven profiles to use.
        If 'dryrun', then print command without executing them."""
        skip_tests_param = "-fae"
        if skip_tests:
            skip_tests_param += " -DskipTests=true"
        if skip_ITs:
            skip_tests_param += " -DskipITs=true"
        profiles_param = []
        if self.is_nuxeoecm:
            profiles_param += ["addons", "distrib"]
        if profiles:
            profiles_param += profiles.split(',')
        if profiles_param:
            profiles_param = " -P" + ','.join(profiles_param)
        else:
            profiles_param = ""
        system("mvn %s %s%s -Dnuxeo.tests.random.mode=BYPASS" % (
               commands, skip_tests_param, profiles_param), delay_stdout=False,
               run=not dryrun)


def log(message, out=sys.stdout):
    out.write(message + os.linesep)
    out.flush()


# Can't this method be replaced with system?
# pylint: disable=C0103
def check_output(cmd):
    """Return Shell command output."""
    args = shlex.split(cmd)
    try:
        p = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                             shell=platform.system() == "Windows")
    # pylint: disable=C0103
    except OSError, e:
        log("$> " + cmd)
        if e.errno == errno.ENOENT:
            raise ExitException(1, "Command not found: '%s'" % args[0])
        else:
            # re-raise unexpected exception
            raise
    out, err = p.communicate()
    retcode = p.returncode
    if retcode != 0:
        if err is None or err == "":
            err = out
        raise ExitException(retcode, "Command '%s' returned non-zero exit code (%s)\n%s" % (cmd, retcode, err))
    return out.strip()


# pylint: disable=R0912,R0913
def system(cmd, failonerror=True, delay_stdout=True, logOutput=True, wait=True, run=True, stdin=subprocess.PIPE,
           stdout=subprocess.PIPE, stderr=subprocess.PIPE):
    """Shell execution.

    'cmd': the command to execute.
    If 'failonerror', command execution failure raises an ExitException.
    If 'delay_stdout', output is flushed at the end of command execution.
    If not 'run', the command is only printed.
    If not 'wait', the process is executed in background and returned as 'p'.
    'stdin', 'stdout', 'stderr' are only used if 'delay_stdout' is True.
    If not 'logOutput', output is only logged in case of exception. Only
    available if 'delay_stdout'"""
    log("$> " + cmd)
    if not run:
        return
    args = shlex.split(cmd)
    try:
        if delay_stdout:
            if logOutput:
                # Merge stderr with stdout
                stderr = subprocess.STDOUT
            p = subprocess.Popen(args, stdin=stdin, stdout=stdout, stderr=stderr, shell=platform.system() == "Windows")
            if wait:
                out, err = p.communicate()
                if logOutput:
                    sys.stdout.write(out)
                    sys.stdout.flush()
        else:
            logOutput = True
            p = subprocess.Popen(args, shell=platform.system() == "Windows")
            if wait:
                p.wait()
    except OSError, e:
        if e.errno == errno.ENOENT:
            raise ExitException(1, "Command not found: '%s'" % args[0])
        else:
            # re-raise unexpected exception
            raise
    if not wait:
        return p
    # pylint: disable=E1103
    retcode = p.returncode
    if retcode != 0:
        if failonerror:
            if logOutput:
                raise ExitException(
                    retcode, "Command '%s' returned non-zero exit code (%s)"
                    % (cmd, retcode))
            else:
                if err is None or err == "":
                    err = out
                raise ExitException(
                    retcode,
                    "Command '%s' returned non-zero exit code (%s)\n%s"
                    % (cmd, retcode, err))
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
            log("Error executing %s - retrying in 10 seconds..." % cmd, sys.stderr)
            time.sleep(10)


def long_path_workaround_init(basedir, dirmapping=True):
    """Windows only. Try to map the 'basedir' to an unused drive letter
    to shorten path names."""
    newdir = basedir
    driveletter = None
    if platform.system() == "Windows" and dirmapping:
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
    if driveletter is not None:
        os.chdir(basedir)
        system("SUBST %s: /D" % (driveletter,), failonerror=False)


def assert_git_config():
    """Check Git configuration."""
    t = check_output("git --version").split()[-1]
    if LooseVersion(t) < LooseVersion(REQUIRED_GIT_VERSION):
        raise ExitException(1, "Requires Git version %s+ (detected %s)" % (REQUIRED_GIT_VERSION, t))
    try:
        t = check_output("git config --get-all color.branch")
    except ExitException, e:
        # Error code 1 is fine (default value)
        if e.return_code > 1:
            log("[WARN] %s" % e.message, sys.stderr)
    try:
        t += check_output("git config --get-all color.status")
    except ExitException, e:
        # Error code 1 is fine (default value)
        if e.return_code > 1:
            log("[WARN] %s" % e.message, sys.stderr)
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
        zipfile = ZipFile(archive, mode, compression=ZIP_DEFLATED)
        for dirpath, _, filenames in os.walk(basedir):
            for name in filenames:
                path = os.path.normpath(os.path.join(dirpath, name))
                if os.path.isfile(path):
                    zipfile.write(path, path)
                    log("Adding %s" % path)
        zipfile.close()
    finally:
        if rootdir is not None:
            os.chdir(cwd)


def extract_zip(archive, outdir=None):
    """Extract a zip file.

    Extracts all the files to the 'outdir' directory (defaults to current dir)
    """
    zipfile = ZipFile(archive, "r")
    if outdir is None:
        outdir = os.getcwd()
    zipfile.extractall(outdir)
    zipfile.close()
