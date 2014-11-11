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
import unittest
from funkload.Lipsum import Lipsum
from nuxeo.pages import *
from nuxeo.testcase import NuxeoTestCase


class Pages(NuxeoTestCase):
    ws_title = "FLNXTEST Page workspace"
    dir_title = "FLNXTEST Page folder"
    tag = "FLNXTEST"
    _lipsum = Lipsum()

    def testAvailable(self):
        BasePage(self).available()

    def testLoginPage(self):
        (LoginPage(self).view()
             .login(*self.cred_admin)
             .logout())
        LoginPage(self).loginInvalid('foo', 'bar')

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
        p = (p.getRootWorkspaces()
             .personalWorkspace()
             .getRootWorkspaces()
             .search('workspaces')
             .logout())

    def testSections(self):
        title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
        description = self.tag + ' ' + self._lipsum.getParagraph(1)
        p = (LoginPage(self).login(*self.cred_admin)
             .getRootSections()
             .createSection(title, description)
             )
        p = (p.getRootSections()
             .deleteItem(title, "Section")
             .logout())

    def dbgtestPublish(self):
        p = LoginPage(self).login(*self.cred_admin)
        p.viewDocumentPath('workspaces/flnxtest-page-workspace/flnxtest-page-folder/flnxtest-tsoc1g7-tris')
        p.publish().publishOnFirstSection()
        p.logout()

    def testPublish(self):
        title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
        description = self.tag + ' ' + self._lipsum.getParagraph(1)
        title_section = self._lipsum.getSubject(uniq=True, prefix=self.tag)

        p = (LoginPage(self).login(*self.cred_admin)
             .getRootSections()
             .createSection(title_section, description)
             .rights().grant('ReadWrite', 'Members group')
             .view()
             )
        p = (LoginPage(self).login(*self.cred_admin)
             .getRootWorkspaces()
             .createWorkspace(self.ws_title, 'A description')
             .view()
             .createFolder(self.dir_title, 'A description')
             .createFile(title, description))
        p = (p.publish().publishOnFirstSection())
        p = (p.getRootWorkspaces()
             .deleteItem(self.ws_title)
             .getRootSections()
             .deleteItem(title_section, "Section"))
        p.logout()

    def testFolderPage(self):
        title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
        description = self.tag + ' ' + self._lipsum.getParagraph(1)
        p = (LoginPage(self).login(*self.cred_admin)
             .getRootWorkspaces()
             .createWorkspace(self.ws_title, 'A description')
             .rights().grant('ReadWrite', 'Members group')
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
        description = self.tag + ' ' + self._lipsum.getParagraph(1)
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
             .mySubscriptions()
             .comments()
             .history())
        p = (p.getRootWorkspaces()
             .deleteItem(self.ws_title)
             .logout())

    def testUsersGroupsPage(self):
        p = LoginPage(self).login(*self.cred_admin)
        login = self.tag.lower()
        pwd = 'secret'
        p = (p.adminCenter().usersAndGroupsPage()
             .createUser(login, 'bob@foo.com', pwd, groups='members',
                         firstname="first", lastname=login.capitalize())
             .createUser(login, 'bobtwice@foo.com', pwd, groups='members',
                         firstname='first', lastname='last'))
        p.exitAdminCenter().logout()
        p.login(login, pwd).getRootWorkspaces().logout()


if __name__ in ('main', '__main__'):
    unittest.main()
