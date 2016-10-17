"""
(C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
    Pierre-Gildas MILLON <pgmillon@nuxeo.com>
"""
import os
import unittest

from mock.mock import Mock, patch

from nxutils import Repository
from release import Release


class TestMocks(object):

    def __init__(self):
        self.__dict__["_items"] = {}

    def __getattr__(self, item):
        if item not in self._items:
            self._items[item] = Mock()
        return self._items[item]

    def clear(self):
        self.__dict__["_items"] = {}


class ReleaseTestCase(unittest.TestCase):

    def setUp(self):
        super(ReleaseTestCase, self).setUp()

        self.mocks = TestMocks()
        patches = [
            patch('release.etree_parse', side_effect=self.mocks.etree_parse)
        ]

        for patcher in patches:
            patcher.start()
            self.addCleanup(patcher.stop)

    def testSetNextSnapshot(self):
        self.mocks.etree_parse.return_value.getroot.return_value.find.return_value.text = '1.0.0-SNAPSHOT'

        release = Release(Repository(os.getcwd(), 'nuxeo_scripts'), 'some_branch', 'auto', 'auto', is_final=True)
        self.assertEqual('1.0.1-SNAPSHOT', release.next_snapshot)

        release.snapshot = '1.2-SNAPSHOT'
        release.set_next_snapshot('auto')
        self.assertEqual('1.3-SNAPSHOT', release.next_snapshot)

        release.snapshot = '1.0.19-SNAPSHOT'
        release.set_next_snapshot('auto')
        self.assertEqual('1.0.20-SNAPSHOT', release.next_snapshot)
