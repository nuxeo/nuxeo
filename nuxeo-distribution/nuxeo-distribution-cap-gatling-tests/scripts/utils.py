#!/usr/bin/python
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
module = sys.modules['__main__'].__file__

class BaseFilter(object):
    def __ror__(self, other):
        return other  # pass-thru

    def __call__(self, other):
        return other | self


class truncate(BaseFilter):
    """Middle truncate string up to length."""
    def __init__(self, length=20, extra='...'):
        self.length = length
        self.extra = extra

    def __ror__(self, other):
        if len(other) > self.length:
            mid_size = (self.length - 3) / 2
            other = other[:mid_size] + self.extra + other[-mid_size:]
        return other


def download(directory, archive_name, url):
    """Download the archive if not already done"""
    dir = os.path.expanduser(directory)
    if not os.path.exists(dir):
        os.makedirs(dir)
    filename = os.path.join(dir, archive_name)
    if not os.path.exists(filename):
        log = logging.getLogger(module)
        cmd = "curl -L -o %s '%s'" % (filename, url)
        log.warn(cmd)
        os.system(cmd)
    return filename


def nuxeoName(name):
   """Returns a valid Nuxeo name from a string."""
   return name.replace('/', '-').replace("[", "-").replace("]", "-") | truncate(30, '-')
