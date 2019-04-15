# coding: utf-8
"""
(C) Copyright 2016-2019 Nuxeo (http://nuxeo.com/) and contributors.

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
    Mickaël Schoentgen <mschoentgen@nuxeo.com>
"""
import os
import unittest

from mock.mock import Mock, patch

from nxutils import Repository
from release import Release, ReleaseInfo


class Mocks(object):

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

        self.mocks = Mocks()
        patches = [
            patch('release.etree_parse', side_effect=self.mocks.etree_parse)
        ]

        for patcher in patches:
            patcher.start()
            self.addCleanup(patcher.stop)

    def assertNextSnapshot(self, current, expected, policy='auto_last'):
        self.mocks.etree_parse.return_value.getroot.return_value.find.return_value.text = current

        release_info = ReleaseInfo(branch='some_branch', tag='auto', maintenance_version='auto', is_final=True,
                                   auto_increment_policy=policy)
        self.assertEqual(expected,
                         Release(Repository(os.getcwd(), 'nuxeo_scripts'), release_info).next_snapshot)

    def testSetNextSnapshot(self):
        self.assertNextSnapshot('1.0.19-SNAPSHOT', '1.0.20-SNAPSHOT')
        self.assertNextSnapshot('8.10-HF01-SNAPSHOT', '8.10-HF02-SNAPSHOT')
        self.assertNextSnapshot('8.10-HF09-SNAPSHOT', '8.10-HF10-SNAPSHOT')
        self.assertNextSnapshot('8.10-HF19-SNAPSHOT', '8.10-HF20-SNAPSHOT')

        self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.0.3-SNAPSHOT', policy='auto_last')
        self.assertNextSnapshot('1.2-SNAPSHOT', '1.3-SNAPSHOT', policy='auto_last')
        self.assertNextSnapshot('29-SNAPSHOT', '30-SNAPSHOT', policy='auto_last')
        self.assertNextSnapshot('8.10-HF01-SNAPSHOT', '8.10-HF02-SNAPSHOT', policy='auto_last')

        self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.0.3-SNAPSHOT', policy='auto_patch')
        self.assertNextSnapshot('1.2-SNAPSHOT', '1.2.1-SNAPSHOT', policy='auto_patch')
        self.assertNextSnapshot('29-SNAPSHOT', '29.0.1-SNAPSHOT', policy='auto_patch')

        self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.1.0-SNAPSHOT', policy='auto_minor')
        self.assertNextSnapshot('1.2-SNAPSHOT', '1.3-SNAPSHOT', policy='auto_minor')
        self.assertNextSnapshot('29-SNAPSHOT', '29.1-SNAPSHOT', policy='auto_minor')

        self.assertNextSnapshot('1.0.2-SNAPSHOT', '2.0.0-SNAPSHOT', policy='auto_major')
        self.assertNextSnapshot('1.2-SNAPSHOT', '2.0-SNAPSHOT', policy='auto_major')
        self.assertNextSnapshot('29-SNAPSHOT', '30-SNAPSHOT', policy='auto_major')
