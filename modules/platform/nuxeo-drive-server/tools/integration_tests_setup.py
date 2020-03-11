"""Launch Nuxeo Drive functional tests against a running Nuxeo instance

Steps executed by the ``test`` command:

    - setup test environment variables

Under Windows:

    - download http://code.google.com/p/lessmsi if not already there
    - extract the MSI archive in a local, non system folder with lessmsi
    - launch the ``ndrive test`` command to run the integration tests

Under POSIX:

    - run the integration tests with nosetests directly from sources

Get the help on running this with::

    python integration_tests_setup.py --help

"""

import os
import sys
import urllib2
import argparse
import re
import zipfile
import shutil
import fnmatch


DEFAULT_BASE_FOLDER = 'nuxeo-drive-client'
DEFAULT_WORK_FOLDER = 'target'
MARKET_PLACE_PREFIX = "nuxeo-drive"
DEFAULT_MARKETPLACE_PATTERN = MARKET_PLACE_PREFIX + r"\-\d\.\d.*?\.zip"
DEFAULT_MARKETPLACE_FILENAME = MARKET_PLACE_PREFIX + '.zip'
LINKS_PATTERN = r'\bhref="([^"]+)"'

DEFAULT_SERVER_URL = "http://localhost:8080/nuxeo"

DEFAULT_MSI_FOLDER = 'dist'
DEFAULT_LESSMSI_URL = "http://lessmsi.googlecode.com/files/lessmsi-v1.0.8.zip"
LESSMSI_FOLDER = 'lessmsi'
EXTRACTED_MSI_FOLDER = 'nxdrive_msi'
NUXEO_DRIVE_HOME_FOLDER = os.path.expanduser('~\.nuxeo-drive')

NOSETESTS_LOGGING_FORMAT = '"%(asctime)s %(process)d %(thread)d %(levelname)-8s %(name)-18s %(message)s"'


def pflush(message):
    """This is required to have messages in the right order in jenkins"""
    print message
    sys.stdout.flush()


def execute(cmd, exit_on_failure=True):
    pflush("> " + cmd)
    code = os.system(cmd)
    if hasattr(os, 'WEXITSTATUS'):
        # Find the exit code in from the POSIX status that also include
        # the kill signal if any (only under POSIX)
        code = os.WEXITSTATUS(code)
    if code != 0 and exit_on_failure:
        pflush("Command %s returned with code %d" % (cmd, code))
        sys.exit(code)


def parse_args(args=None):
    main_parser = argparse.ArgumentParser(
        description="Integration tests coordinator")
    subparsers = main_parser.add_subparsers(title="Commands")

    main_parser.add_argument('--base-folder',
                        default=DEFAULT_BASE_FOLDER,
                        help="Folder to run tests in.")
    main_parser.add_argument('--work-folder', default=DEFAULT_WORK_FOLDER,
                        help="Folder to work in (marketplace package download,"
                        " MSI extract, ...).")

    # Fetch marketplace package dependency from related Jenkins job
    parser = subparsers.add_parser(
        'fetch-mp', help="Fetch nuxeo-drive marketplace package from Jenkins.")
    parser.set_defaults(command='fetch-mp')
    parser.add_argument('--url',
                        help="Marketplace package (Jenkins page) URL.")
    parser.add_argument('--direct', action='store_true',
                        help="Direct download from Nexus.")
    parser.add_argument('--marketplace-pattern',
                        default=DEFAULT_MARKETPLACE_PATTERN,
                        help="If --direct is not specified pattern used to"
                        " download the marketplace package from Jenkins job"
                        " archived artifacts.")
    parser.add_argument('--marketplace-filename',
                        default=DEFAULT_MARKETPLACE_FILENAME,
                        help="Name of the downloaded marketplace package"
                        " file.")

    # Integration test launcher
    parser = subparsers.add_parser(
        'test', help="Launch integration tests with nose.")
    parser.set_defaults(command='test')
    parser.add_argument("--server-url", default=DEFAULT_SERVER_URL)
    parser.add_argument("--msi-folder", default=DEFAULT_MSI_FOLDER)
    parser.add_argument("--lessmsi-url", default=DEFAULT_LESSMSI_URL)

    return main_parser.parse_args(args)


def download(url, filename):
    if not os.path.exists(filename):
        pflush("Downloading %s to %s" % (url, filename))
        headers = {'User-Agent': 'nxdrive test script'}
        req = urllib2.Request(url, None, headers)
        reader = urllib2.urlopen(req)
        with open(filename, 'wb') as f:
            while True:
                b = reader.read(1000 ** 2)
                if b == '':
                    break
                f.write(b)


def unzip(filename, target=None):
    pflush("Unzipping " + filename)
    zf = zipfile.ZipFile(filename, 'r')
    for info in zf.infolist():
        filename = info.filename
        # Skip first directory entry
        if filename.endswith('/'):
            continue
        if target is not None:
            filename = os.path.join(target, filename)
        dirname = os.path.dirname(filename)
        if dirname != '' and not os.path.exists(dirname):
            os.makedirs(dirname)
        with open(filename, 'wb') as f:
            f.write(zf.read(info.filename))


def find_latest(folder, prefix=None, suffix=None):
    files = os.listdir(folder)
    if prefix is not None:
        files = [f for f in files if f.startswith(prefix)]
    if suffix is not None:
        files = [f for f in files if f.endswith(suffix)]
    if not files:
        raise RuntimeError(('Could not find file with prefix "%s"'
                            'and suffix "%s" in "%s"')
                           % (prefix, suffix, folder))
    files.sort()
    return os.path.join(folder, files[-1])


def find_package_url(archive_page_url, pattern):
    pflush("Finding latest package at: " + archive_page_url)
    index_html = urllib2.urlopen(archive_page_url).read()
    candidates = []
    archive_pattern = re.compile(pattern)
    for link in re.compile(LINKS_PATTERN).finditer(index_html):
        link_url = link.group(1)
        if "/" in link_url:
            link_filename = link_url.rsplit("/", 1)[1]
        else:
            link_filename = link_url
        if archive_pattern.match(link_filename):
            candidates.append(link_url)

    if not candidates:
        raise ValueError("Could not find packages with pattern %r on %s"
                         % (pattern, archive_page_url))
    candidates.sort()
    archive = candidates[0]
    if archive.startswith("http"):
        archive_url = archive
    else:
        if not archive_page_url.endswith('/'):
            archive_page_url += '/'
        archive_url = archive_page_url + archive
    return archive_url, archive_url.rsplit('/', 1)[1]


def extract_msi(lessmsi_url, msi_folder, work_folder, extracted_msi_folder):
    if os.path.isdir(msi_folder):
        msi_filename = find_latest(msi_folder, suffix='.msi')
    else:
        msi_filename = msi_folder

    lessmsi_folder = os.path.join(work_folder, LESSMSI_FOLDER)
    if not os.path.exists(lessmsi_folder):
        lessmsi_filename = os.path.basename(lessmsi_url)
        lessmsi_filepath = os.path.join(work_folder, lessmsi_filename)
        download(lessmsi_url, lessmsi_filepath)
        unzip(lessmsi_filepath, target=lessmsi_folder)

    pflush("Extracting the MSI")
    lessmsi = os.path.join(lessmsi_folder, 'lessmsi')
    if os.path.exists(extracted_msi_folder):
        shutil.rmtree(extracted_msi_folder)
    execute("%s /x %s %s" % (lessmsi, msi_filename, extracted_msi_folder))


def set_environment(server_url):
    os.environ['NXDRIVE_TEST_NUXEO_URL'] = server_url
    os.environ['NXDRIVE_TEST_USER'] = "Administrator"
    os.environ['NXDRIVE_TEST_PASSWORD'] = "Administrator"


def clean_pyc(dir_):
    for root, dirnames, filenames in os.walk(dir_):
        if '.git' in dirnames:
            dirnames.remove('.git')
        for filename in fnmatch.filter(filenames, '*.pyc'):
            file_path = os.path.join(root, filename)
            print('Removing .pyc file: %s' % file_path)
            os.unlink(file_path)


def run_tests_from_msi(extracted_msi_folder, base_folder):
    clean_home_folder()
    ndrive = os.path.join(extracted_msi_folder, 'SourceDir', 'ndrive.exe')
    execute(ndrive + " test -w " + base_folder + " --logging-format=" + NOSETESTS_LOGGING_FORMAT)


def clean_home_folder(dir_=None):
    dir_ = dir_ if dir_ is not None else NUXEO_DRIVE_HOME_FOLDER
    if os.path.exists(dir_):
        print('Removing home folder before running tests: %s' % dir_)
        shutil.rmtree(dir_)


def run_tests_from_source(base_folder):
    pflush("PATH detected by Python = " + os.environ.get('PATH'))
    cmd = "cd " + base_folder + " && nosetests -v -x --logging-format=" + NOSETESTS_LOGGING_FORMAT
    execute(cmd)


def download_package(url, pattern, target_folder, filename):
    # First resolve possible URL redirect
    req = urllib2.Request(url)
    res = urllib2.urlopen(req)
    final_url = res.geturl()
    if pattern is None:
        if filename is None:
            filename = final_url.rsplit("/", 1)[1]
    else:
        final_url, url_filename = find_package_url(final_url, pattern)
        if filename is None:
            filename = url_filename
    filepath = os.path.join(target_folder, urllib2.unquote(filename))
    download(final_url, filepath)


def clean_download_dir(dir_, pattern):
    if os.path.exists(dir_):
        for f in os.listdir(dir_):
            if re.search(pattern, f):
                os.remove(os.path.join(dir_, f))
    else:
        os.makedirs(dir_)


if __name__ == "__main__":
    options = parse_args()

    if options.command == 'test':
        if not os.path.exists(options.base_folder):
            pflush("Base folder '%s' doesn't exist, please provide the"
                   " --base-folder option."
                   % options.base_folder)
        else:
            set_environment(options.server_url)
            clean_pyc(options.base_folder)
            if sys.platform == 'win32':
                extracted_msi_folder = os.path.join(options.work_folder,
                                                    EXTRACTED_MSI_FOLDER)
                extract_msi(options.lessmsi_url, options.msi_folder,
                            options.work_folder, extracted_msi_folder)
                run_tests_from_msi(extracted_msi_folder, options.base_folder)
            else:
                run_tests_from_source(options.base_folder)
    elif options.command == 'fetch-mp':
        if options.url is None:
            pflush("Please provide the --url option.")
        else:
            clean_download_dir(options.work_folder,
                               options.marketplace_filename)
            # Download Nuxeo Drive marketplace package
            if options.direct:
                # Direct download from Nexus
                pattern = None
            else:
                # Download from Jenkins job archived artifacts
                pattern = options.marketplace_pattern
            download_package(options.url,
                             pattern,
                             options.work_folder,
                             options.marketplace_filename)
