# (C) Copyright 2013 Nuxeo SAS <http://nuxeo.com>
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
"""This test suite contains scnerii to test nuxeo/rest module.

This suite is configured using the Rest.conf file.
"""
import os
from time import sleep
import unittest
import random
from nuxeo.drive import DriveClient
from nuxeo.pages import *
from nuxeo.testcase import NuxeoTestCase


class Drive(NuxeoTestCase):
    ws_title = "FLNXTEST Drive workspace"
    ws_path = "workspaces/FLNXTEST Drive workspace"
    tag = "FLNXTEST"

    def setUp(self):
        NuxeoTestCase.setUp(self)
        self.nb_write = self.conf_getInt('testDrive', 'nb_doc')
        import_path = self.conf_get('testDrive', 'import_path')
        self.files = [os.path.join(import_path, item)
                      for item in os.listdir(import_path)]

    def testInit(self):
        """Create a workspace for drive document."""
        p = LoginPage(self).login(*self.cred_admin)
        ret = p.viewDocumentPath(self.ws_path, raiseOn404=False)
        if ret is None:
            p = (p.getRootWorkspaces()
                 .createWorkspace(self.ws_title, 'A description')
                 .rights().grant('ReadWrite', 'Members group')
                 .view())
        p.logout()

    def createFolder(self, name):
        return (FolderPage(self).viewDocumentPath(self.ws_path)
                .createFolder(name, 'folder for drive test by ' +
                              self.cred_member[0]))

    def createServerFile(self, folder):
        file_path = random.choice(self.files)
        title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
        description = self.tag + ' ' + self._lipsum.getParagraph(1)
        folder.createFile(title, description, file_path)
        return folder.getDocUid()

    def testDrive(self):
        self.setHeader('Accept-Language', 'en-us')
        d = (DriveClient(self)
             .bind_server(*self.cred_member))
        # Use the token auth, create a folder and a file
        folder_name = "Folder " + self._lipsum.getUniqWord()
        folder = self.createFolder(folder_name)
        fuid = folder.getDocUid()
        folder.driveSynchronizeCurrentDocument()
        self.createServerFile(folder)
        # start drive and sync
        d.start_drive()
        parent_id = d.root_ids[0]
        for i in range(self.nb_write):
            path = random.choice(self.files)
            name = self._lipsum.getUniqWord() + '-' + os.path.basename(path)
            d.upload_file(parent_id, path, name)
            folder.viewDocumentUid(fuid)
            uid = self.createServerFile(folder)
            sleep(1)
            d.get_update(" update " + str(i))
            self.assert_(uid in self.getBody(),
                         "expecting to find the new doc uid: " +
                         uid)   # + " in " + self.getBody())
        folder.viewDocumentUid(fuid)
        folder.driveUnsynchronizeCurrentDocument()

if __name__ in ('main', '__main__'):
    unittest.main()
