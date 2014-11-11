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
"""This test suite contains scnerii to test/bench a Nuxeo EP

This suite is configured using the Nuxeo.conf file.
"""
import os
import random
import unittest
from funkload.Lipsum import Lipsum
from funkload.utils import xmlrpc_get_credential
from nuxeo.pages import *
from nuxeo.testcase import NuxeoTestCase


class Nuxeo(NuxeoTestCase):
    ws_title = "FLNXTEST Bench workspace"
    dir_title = "FLNXTEST Bench folder"
    dir_path = "workspaces/FLNXTEST Bench workspace/FLNXTEST Bench folder"
    section_title = "FLNXTEST Bench section"
    section_path = "sections/FLNXTEST Bench section"
    tag = "FLNXTEST"

    def setUp(self):
        NuxeoTestCase.setUp(self)
        self.nb_write = self.conf_getInt('testWriter', 'nb_doc')
        import_path = self.conf_get('testWriter', 'import_path')
        self.files = [os.path.join(import_path, item)
                      for item in os.listdir(import_path)]
        self.nb_read = self.conf_getInt('testReader', 'nb_doc')
        self.search = self.conf_getInt('testReader', 'search', 1)

    def testInit(self):
        p = LoginPage(self).login(self.cred_admin[0], self.cred_admin[1])
        ret = p.viewDocumentPath(self.section_path, raiseOn404=False)
        if ret is None:
            # create a section, grant rights to members
            p = (p.getRootSections()
                 .createSection(self.section_title, 'A description')
                 .rights().grant('ReadWrite', 'Members group'))
        ret = p.viewDocumentPath(self.dir_path, raiseOn404=False)
        if ret is None:
            # create a workspace and a folder, grant rights to members
            p = (p.getRootWorkspaces()
                 .createWorkspace(self.ws_title, 'A description')
                 .rights().grant('ReadWrite', 'Members group')
                 .view()
                 .createFolder(self.dir_title, 'A description'))
        # create users
        login = self.cred_member[0]
        pwd = self.cred_member[1]
        first_login = login
        host = self.credential_host
        port = self.credential_port
        group = 'members'
        p = p.adminCenter().usersAndGroupsPage()
        while True:
            p.createUser(login, login + '@127.0.0.1', pwd, groups='members',
                         firstname="first", lastname=login.capitalize())
            login, pwd = xmlrpc_get_credential(host, port, group)
            if login == first_login:
                break
        p = p.exitAdminCenter()
        p.logout()

    def testWriter(self):
        lipsum = self._lipsum
        tag = self.tag
        # Go to bench folder using redirection after login
        p = (BasePage(self)
             .viewDocumentPath(self.dir_path)
             .login(self.cred_member[0], self.cred_member[1]))
        # create files and publish
        for i in range(self.nb_write):
            file_path = random.choice(self.files)
            extension = os.path.splitext(file_path)[1][1:].upper()
            title = lipsum.getSubject(uniq=True, prefix=tag) + " " + extension
            description = tag + ' ' + self._lipsum.getParagraph(1)
            p.createFile(title, description, file_path)
            p.publish().publishOnFirstSection()
            p = p.viewDocumentPath(self.dir_path)
        p.logout()

    def testDashboard(self):
        p = (LoginPage(self).view()
             .login(self.cred_admin[0], self.cred_admin[1]))
        p = p.dashboardNew()
        p.logout()

    def testReader(self):
        p = (LoginPage(self).view()
             .login(self.cred_member[0], self.cred_member[1]))

        for i in range(self.nb_read):
            p = (FolderPage(self).viewDocumentPath(self.dir_path)
                 .sort(random.choice(['title', 'author', 'date', 'lifecycle']))
                 .sort(random.choice(['title', 'author', 'date', 'lifecycle']))
                 .viewRandomDocument(pattern=self.tag.capitalize()))
            p = (p.edit()
                 .files()
                 .publish()
                 .relations()
                 .mySubscriptions()
                 .comments()
                 .history())

        # p.dashboard()
        # p.getRootWorkspaces()
        # p.dashboardNew()
        p.getRootWorkspaces()
        # p.personalWorkspace()
        if self.search:
            p.getRootWorkspaces()
            p = (p.search('scrum', 'Search with empty results')
                 .search('"' + self.dir_title + '"', 'Search one document')
                 .search('cephalus', 'Search few documents')
                 .search(self.tag, 'Search all documents'))

        p.logout()

if __name__ in ('main', '__main__'):
    unittest.main()
