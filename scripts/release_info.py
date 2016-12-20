import os
import sys

from nxutils import log


class ReleaseInfo(object):
    """Release information. This object is used to construct a Release as well as to (de)serialize it."""

    # pylint: disable-msg=too-many-arguments, too-many-locals
    def __init__(self, module=None, remote_alias=None, branch=None, tag="auto",
                 next_snapshot=None, maintenance_version="discard", is_final=False, skip_tests=False,
                 skip_its=False, profiles='', other_versions=None, files_pattern=None, props_pattern=None,
                 msg_commit='', msg_tag='', auto_increment_policy='auto_patch', deploy = False, dryrun = False,
                 interactive = False):
        self.module = module
        self.remote_alias = remote_alias
        self.branch = branch
        self.tag = tag
        self.next_snapshot = next_snapshot
        """Backward compat for maintenance_version = auto"""
        self.maintenance_version = "discard" if maintenance_version == "auto" else maintenance_version
        self.is_final = is_final
        self.skip_tests = skip_tests
        self.skip_its = skip_its
        self.profiles = profiles
        self.other_versions = other_versions
        self.files_pattern = files_pattern
        self.props_pattern = props_pattern
        self.msg_commit = msg_commit
        self.msg_tag = msg_tag
        self.auto_increment_policy = auto_increment_policy
        self.deploy = deploy
        self.dryrun = dryrun

    def compute_other_versions(self):
        self.other_versions = ':'.join((self.files_pattern, self.props_pattern, self.other_versions))
        if self.other_versions == "::":
            self.other_versions = None

    @staticmethod
    def get_release_log(path=os.getcwd()):
        """Return the path for the file containing the release parameters
given the path parameter.

        'path': root path of the repository being released."""
        return os.path.abspath(os.path.join(path, os.pardir,
                               "release-%s.log" % os.path.basename(path)))

    # pylint: disable=R0914
    def read_release_log(self, release_log):
        """Read release parameters generated for the given path.

        'release_log': path to a release log file."""
        log("Reading parameters from %s ..." % release_log)
        with open(release_log, "rb") as f:
            for line in f:
                (key, value) = line.split("=", 1)
                key = key.strip()
                value = value.strip()
                if key == "MODULE":
                    self.module = value
                elif key == "REMOTE":
                    self.remote_alias = value
                elif key == "BRANCH":
                    self.branch = value
                elif key == "SNAPSHOT":
                    self.snapshot = value
                elif key == "TAG":
                    self.tag = value
                elif key == "NEXT_SNAPSHOT":
                    self.next_snapshot = value
                elif key == "MAINTENANCE":
                    self.maintenance_version = value
                elif key == "FINAL":
                    self.is_final = value == "True"
                elif key == "SKIP_TESTS":
                    self.skip_tests = value == "True"
                elif key == "SKIP_ITS":
                    self.skip_its = value == "True"
                elif key == "PROFILES":
                    self.profiles = value
                elif key == "OTHER_VERSIONS":
                    self.other_versions = value
                elif key == "FILES_PATTERN":
                    self.files_pattern = value
                elif key == "PROPS_PATTERN":
                    self.props_pattern = value
                elif key == "MSG_COMMIT":
                    self.msg_commit = value
                elif key == "MSG_TAG":
                    self.msg_tag = value
                elif key == "AUTO_INCREMENT_POLICY":
                    self.auto_increment_policy = value
                elif key == "DRY_RUN":
                    self.dryrun = value == "True"
                else:
                    log("[WARN] Release info parsing failure for file %s on line: %s" % (release_log, line), sys.stderr)
        self.compute_other_versions()

