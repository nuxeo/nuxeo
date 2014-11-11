# (C) Copyright 2009 Nuxeo SAS <http://nuxeo.com>
# Author: bdelbosc@nuxeo.com
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 2 as published
# by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
# 02111-1307, USA.
#
"""This test suite test the nuxeo/pages module.

This suite is configured using the Pages.conf file.
"""
import os
import unittest
from funkload.Lipsum import Lipsum
if os.environ.get('JAVA6'):
    from nuxeo.pagesjava6 import *
else:
    from nuxeo.pages import *
from nuxeo.testcase import NuxeoTestCase

class Pages(NuxeoTestCase):
    ws_title = "FLNXTEST Page workspace"
    dir_title = "FLNXTEST Page folder"
    tag = "FLNXTEST"
    _lipsum = Lipsum()

    def testAvailable(self):
        p = BasePage(self).available()

    def testLoginPage(self):
        p = (LoginPage(self).view()
             .login(*self.cred_admin)
             .logout())
        p = LoginPage(self).loginInvalid('foo', 'bar')

    def testBasePageViewDocumentPath(self):
        # fluent test
        p = (LoginPage(self)
             .login(*self.cred_admin)
             .getRootWorkspaces()
             .logout())
        # test redirection after login
        p = BasePage(self).viewDocumentPath("workspaces")
        p = p.login(*self.cred_admin)
        # TODO assert we are on workspaces
        ret = p.viewDocumentPath("workspaces/that/does/not/exists",
                                 raiseOn404=False)
        self.assert_(ret is None, "Expecting None for a 404.")
        p.logout()

    def testNavigation(self):
        p = (LoginPage(self)
             .login(*self.cred_admin)
             .getRootWorkspaces())
        p = (p.rights()
             .manage())
        p = (p.dashboard()
             .getRootWorkspaces()
             .personalWorkspace()
             .getRootWorkspaces()
             .search('workspaces')
             .logout())

    def testFolderPage(self):
        title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
        description = self.tag + ' ' + self._lipsum.getParagraph()
        p = (LoginPage(self).login(*self.cred_admin)
             .getRootWorkspaces()
             .createWorkspace(self.ws_title, 'A description')
             .rights().grant('ReadWrite', 'members')
             .view()
             .createFolder(self.dir_title, 'A description'))
        fuid = p.getDocUid()
        p.createFile(title, description)
        p = (p.viewDocumentUid(fuid)
             .sort('author')
             .sort('title')
             .sort('lifecycle')
             .sort('date'))
        p = (p.getRootWorkspaces()
             .deleteItem(self.ws_title)
             .logout())

    def testFileTabs(self):
        title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
        description = self.tag + ' ' + self._lipsum.getParagraph()
        p = (LoginPage(self).login(*self.cred_admin)
             .getRootWorkspaces()
             .createWorkspace(self.ws_title, 'A description')
             .view()
             .createFolder(self.dir_title, 'A description')
             .createFile(title, description))
        p = (p.edit()
             .files()
             .publish()
             .relations()
             .workflow()
             .mySubscriptions()
             .comments()
             .history())
        p = (p.getRootWorkspaces()
             .deleteItem(self.ws_title)
             .logout())


    def testMemberManagementPage(self):
        p = LoginPage(self).login(*self.cred_admin)
        login = self.tag.lower()
        pwd = 'secret'
        p = (p.memberManagement()
             .createUser(login, 'bob@foo.com', pwd, groups='members',
                         firstname="first", lastname=login.capitalize())
             .createUser(login, 'bobtwice@foo.com', pwd, groups='members',
                         firstname='first', lastname='last'))
        p.logout()
        p.login(login, pwd).getRootWorkspaces().logout()



if __name__ in ('main', '__main__'):
    unittest.main()
