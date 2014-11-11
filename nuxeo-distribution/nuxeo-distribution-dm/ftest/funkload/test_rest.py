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
"""This test suite contains scnerii to test nuxeo/rest module.

This suite is configured using the Rest.conf file.
"""
import unittest
from zipfile import ZipFile
from cStringIO import StringIO
from webunit.utility import Upload
from funkload.Lipsum import Lipsum
from funkload.FunkLoadTestCase import FunkLoadTestCase
from funkload.utils import xmlrpc_get_credential
from nuxeo.rest import RestAPI

class Rest(FunkLoadTestCase):
    ws_title = "FLNXTEST Rest workspace"
    dir_title = "FLNXTEST Rest folder"
    tag = "FLNXTEST"
    _lipsum = Lipsum()

    def setUp(self):
        self.server_url = self.conf_get('main', 'url')
        self.nb_doc = self.conf_getInt('testWriter', 'nb_doc')
        self.nb_read = self.conf_getInt('testReader', 'nb_read')
        self.credential_host = self.conf_get('credential', 'host')
        self.credential_port = self.conf_getInt('credential', 'port')
        self.cred_admin = xmlrpc_get_credential(self.credential_host,
                                                self.credential_port,
                                                'admin')

    def FAIL_testUpload(self):
        r = RestAPI(self)
        r.login(*self.cred_admin)
        uid = '44f1af7e-5206-4f39-b935-55bb32ab3112'
        r.uploadFile(uid, 'foo.txt')

        r.logout()

    def testWriter(self):
        # Create a folder and few documents
        r = RestAPI(self)
        r.login(*self.cred_admin)
        root_uid = r.getRootWorkspaceUid()
        ws_uid = r.getChildUid(root_uid, self.ws_title, 'Workspace')
        if not ws_uid:
            ws_uid = r.createDocument(root_uid, 'Workspace', self.ws_title,
                                      'Test workspace description.')
        dir_uid = r.getChildUid(ws_uid, self.dir_title, 'Folder')
        if not dir_uid:
            dir_uid = r.createDocument(ws_uid, 'Folder', self.dir_title,
                                       'Test folder description')
        for i in range(self.nb_doc):
            title = self._lipsum.getSubject(uniq=True, prefix=self.tag)
            description = self.tag + ' ' + self._lipsum.getParagraph(1)
            uid = r.createDocument(dir_uid, 'File',
                                   title, description)
        r.logout()

    def testReader(self):
        # browse the workspace
        r = RestAPI(self)
        r.login(*self.cred_admin)
        uid = r.getRootWorkspaceUid()
        uid = r.getChildUid(uid, self.ws_title, 'Workspace')
        self.assert_(uid, 'Workspace "%s" not found.' % self.ws_title)
        uid = r.getChildUid(uid, self.dir_title, 'Folder')
        self.assert_(uid, 'Folder "%s" not found.' % self.dir_title)
        count = 0
        nb_read = self.nb_read
        for item in r.browse(uid):
            ret = r.exportTree(item.id)
            zc = ZipFile(StringIO(ret))
            self.assert_('.nuxeo-archive' in zc.namelist())
            count += 1
            if count > nb_read:
                break
        r.logout()


if __name__ in ('main', '__main__'):
    unittest.main()
