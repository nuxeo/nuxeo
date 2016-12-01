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
import shutil
import unittest
from tempfile import mkdtemp

from mock.mock import Mock, patch

from nxutils import Repository, system, system_with_retries, check_output
from release import Release, ReleaseInfo


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

        self.workdir = mkdtemp()
        self.mocks = TestMocks()

        os.chdir(self.workdir)
        system('git init')
        system('git remote add origin file:///dev/null')

    def tearDown(self):
        super(ReleaseTestCase, self).tearDown()

        shutil.rmtree(self.workdir)

    def assertNextSnapshot(self, current, expected, policy='auto_last'):
        self.mocks.etree_parse.return_value.getroot.return_value.find.return_value.text = current

        release_info = ReleaseInfo(branch='some_branch', tag='auto', maintenance_version='auto', is_final=True,
                                   auto_increment_policy=policy, next_snapshot='auto')
        self.assertEqual(expected, Release(Repository(os.getcwd(), 'nuxeo_scripts'), release_info).next_snapshot)

    def generateResource(self, resource):
        shutil.copy(os.path.join(os.path.dirname(__file__), resource), self.workdir)

    def testSetNextSnapshot(self):
        with(patch('release.etree_parse', side_effect=self.mocks.etree_parse)):
            self.assertNextSnapshot('1.0.19-SNAPSHOT', '1.0.20-SNAPSHOT')

            self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.0.3-SNAPSHOT', policy='auto_last')
            self.assertNextSnapshot('1.2-SNAPSHOT', '1.3-SNAPSHOT', policy='auto_last')
            self.assertNextSnapshot('29-SNAPSHOT', '30-SNAPSHOT', policy='auto_last')

            self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.0.3-SNAPSHOT', policy='auto_patch')
            self.assertNextSnapshot('1.2-SNAPSHOT', '1.2.1-SNAPSHOT', policy='auto_patch')
            self.assertNextSnapshot('29-SNAPSHOT', '29.0.1-SNAPSHOT', policy='auto_patch')

            self.assertNextSnapshot('1.0.2-SNAPSHOT', '1.1.0-SNAPSHOT', policy='auto_minor')
            self.assertNextSnapshot('1.2-SNAPSHOT', '1.3-SNAPSHOT', policy='auto_minor')
            self.assertNextSnapshot('29-SNAPSHOT', '29.1-SNAPSHOT', policy='auto_minor')

            self.assertNextSnapshot('1.0.2-SNAPSHOT', '2.0.0-SNAPSHOT', policy='auto_major')
            self.assertNextSnapshot('1.2-SNAPSHOT', '2.0-SNAPSHOT', policy='auto_major')
            self.assertNextSnapshot('29-SNAPSHOT', '30-SNAPSHOT', policy='auto_major')

    def testPrepare(self):
        self.generateResource('resources/pom.xml')
        self.generateResource('resources/layouts-summary-contrib.xml')

        system('git add pom.xml')
        system('git commit -m "Original import"')

        release_info = ReleaseInfo(branch='master', tag='auto', maintenance_version='8.10-HF01-SNAPSHOT', is_final=True,
                                   auto_increment_policy='auto_major_no_zero', next_snapshot='auto')
        release = Release(Repository(os.getcwd(), 'origin'), release_info)

        orig_system_with_retries = system_with_retries

        def mock_system_with_retries(cmd, failonerror=True):
            if "git fetch origin" != cmd:
                orig_system_with_retries(cmd, failonerror)

        with patch('nxutils.system_with_retries', side_effect=mock_system_with_retries), \
                patch('nxutils.Repository.mvn'), patch('release.Release.package'):
            release.prepare()

        self.assertEqual('release-8.10', check_output('git tag'))
        self.assertIn('8.10', check_output('git branch'))
        self.assertIn('master', check_output('git branch'))

        system('git checkout release-8.10')
        with open('pom.xml') as pom, open('layouts-summary-contrib.xml') as contrib:
            self.assertIn('<version>8.10</version>', pom.read())
            self.assertIn('<sinceVersion>8.10</sinceVersion>', contrib.read())

        system('git checkout 8.10')
        with open('pom.xml') as pom, open('layouts-summary-contrib.xml') as contrib:
            self.assertIn('<version>8.10-HF01-SNAPSHOT</version>', pom.read())
            self.assertIn('<sinceVersion>8.10</sinceVersion>', contrib.read())

        system('git checkout master')
        with open('pom.xml') as pom, open('layouts-summary-contrib.xml') as contrib:
            self.assertIn('<version>9.1-SNAPSHOT</version>', pom.read())
            self.assertIn('<sinceVersion>8.10</sinceVersion>', contrib.read())
