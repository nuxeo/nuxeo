#!/usr/bin/env python
# -*- coding: utf-8 -*-

# (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser General Public License
# (LGPL) version 2.1 which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/lgpl-2.1.html
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# Contributors:
#     Delbosc Benoit
#

import os
import sys
import logging
import argparse
from abc import ABCMeta, abstractmethod
from redis import RedisWriter
from nuxeo import NuxeoWriter
from utils import download

__version__ = "0.1.0"

module = sys.modules['__main__'].__file__

DESC = """By default read input from stdin and output Redis pipe command on stdout.

To download the data file see the -d option.
"""

class Injector(object):
    """Abstract class to create an injector, take care of parsing args, downloading data"""
    __metaclass__ = ABCMeta

    @abstractmethod
    def parse(self, input, writer):
        """Parse an input file, use Nuxeo writer to output document in redis format."""
        pass

    @abstractmethod
    def downloadInfo(self):
        """Return a tupple (download_url, archive_name)"""
        pass

    def run(self):
        """Run the injector"""
        self.log = logging.getLogger(module)
        logging.basicConfig(stream=sys.stderr, level=logging.DEBUG,
                            format='%(name)s (%(levelname)s): %(message)s')
        try:
            args = self.parse_command_line()
            self.set_log_level()
            output = args.output
            writer = NuxeoWriter(RedisWriter(out=output,
                                             prefix=args.redis_ns,
                                             usePipeProtocol=not args.no_pipe))
            self.parse(self.get_input(), writer)
            output.flush()
            output.close()
            return 0
        except KeyboardInterrupt:
            self.log.error('Program interrupted!')
            return -1
        finally:
            logging.shutdown()

    def parse_command_line(self):
        argv = sys.argv
        formatter_class = argparse.ArgumentDefaultsHelpFormatter
        parser = argparse.ArgumentParser(description=DESC,
                                         formatter_class=formatter_class)
        parser.add_argument('--version', action='version',
                            version='%(prog)s {}'.format(__version__))
        parser.add_argument('-v', '--verbose', dest='verbose_count',
                            action='count', default=0,
                            help='Increases log verbosity for each occurence.')
        parser.add_argument('-o', '--output', metavar='output',
                            type=argparse.FileType('w'), default=sys.stdout,
                            help='Redirect output to a file')
        parser.add_argument('-i', '--input', metavar='input',
                            type=argparse.FileType('r'),
                            default=sys.stdin,
                            help='Input file')
        parser.add_argument('--no-pipe', action='store_true',
                            help='Output Redis command in clear not using pipe mode protocol.')
        parser.add_argument('-d', '--download', action='store_true', dest='download',
                            help='Download input if not already done.')
        parser.add_argument('-u', '--download-url', dest='url', default=self.downloadInfo()[1],
                            help='URL used to download the data file.')
        parser.add_argument('-O', '--data-directory', dest='data_dir', default=os.path.join('~', 'data'),
                            help='Data directory to store downloaded file.')
        parser.add_argument('-p', '--redis-namespace', dest='redis_ns', default="imp",
                            help='Redis key prefix.')
        arguments = parser.parse_args(argv[1:])
        self.arguments = arguments
        return arguments

    def set_log_level(self):
        # Sets log level to WARN going more verbose for each new -v.
        self.log.setLevel(max(3 - self.arguments.verbose_count, 0) * 10)

    def get_input(self):
        args = self.arguments
        if args.download:
            self.log.info("downloading")
            archive_name, url = self.downloadInfo()
            return open(download(args.data_dir, archive_name, url), 'r')
        return args.input
