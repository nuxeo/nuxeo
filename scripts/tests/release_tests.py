#!/usr/bin/env python
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

    def assertNextSnapshot(self, current, expected):
        self.mocks.etree_parse.return_value.getroot.return_value.find.return_value.text = current
        self.assertEqual(expected,
                         Release(Repository(os.getcwd(), 'nuxeo_scripts'), 'some_branch', 'auto', 'auto',
                                 is_final=True).next_snapshot)

    def testSetNextSnapshot(self):
        self.assertNextSnapshot('1.0.19-SNAPSHOT', '1.0.20-SNAPSHOT')
        self.assertNextSnapshot('8.10-HF01-SNAPSHOT', '8.10-HF02-SNAPSHOT')
        self.assertNextSnapshot('8.10-HF09-SNAPSHOT', '8.10-HF10-SNAPSHOT')
        self.assertNextSnapshot('8.10-HF19-SNAPSHOT', '8.10-HF20-SNAPSHOT')

        self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.0.3-SNAPSHOT')
        self.assertNextSnapshot('1.2-SNAPSHOT', '1.3-SNAPSHOT')
        self.assertNextSnapshot('29-SNAPSHOT', '30-SNAPSHOT')

