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
import os
import unittest
from nuxeo.rest import RestAPI
from nuxeo.testcase import NuxeoTestCase
from random import randint
from nuxeo.pages import *

def getRandomLines(filename, nb_line):
    """Return a list of lines randomly taken from filename"""
    fd = open(filename, "r")
    filesize = os.stat(filename)[6]
    ret = []
    for i in range(nb_line):
        pos = max(randint(0, filesize - 40), 0)
        fd.seek(pos)
        fd.readline() # skip line
        ret.append(fd.readline().strip())
    return ret

def browse(r, item, f, d):
    """Recursive browsing"""
    doc_type = item.type
    if doc_type == 'File':
        d.write(item.id + '\n')
        return
    if doc_type == 'Folder' or doc_type == 'Workspace':
        if doc_type == 'Folder':
            f.write(item.id + '\n')
        for i in r.browse(item.id):
            browse(r, i, f, d)
    return



class Navigation(NuxeoTestCase):

    def setUp(self):
        NuxeoTestCase.setUp(self)
        self.folder_file = self.conf_get('main', 'folder_file')
        self.doc_file = self.conf_get('main', 'doc_file')
        self.nb_docs = self.conf_getInt('testNavigation', 'nb_docs')
        self.nb_folders = self.conf_getInt('testNavigation', 'nb_folders')

    def testBrowse(self):
        # browse the workspace
        r = RestAPI(self)
        r.login(*self.cred_admin)
        uid = r.getRootWorkspaceUid()
        folders = []
        documents = []
        class Root:
            id = uid
            type = 'Folder'
            title = "Root worksapce"

        f = open(self.folder_file, 'w+')
        d = open(self.doc_file, 'w+')
        browse(r, Root(), f, d)
        f.close()
        d.close()
        r.logout()
        print "Done"
        print "Output on %s and %s" % (self.folder_file, self.doc_file)


    def testNavigation(self):
        p = (LoginPage(self).view()
             .login(self.cred_member[0], self.cred_member[1]))

        folders = getRandomLines(self.folder_file, self.nb_folders)
        for uid in folders:
            p = (FolderPage(self).viewDocumentUid(uid)
                 .sort(random.choice(['title', 'author', 'date', 'lifecycle'])))

        documents = getRandomLines(self.doc_file, self.nb_docs)
        for uid in documents:
            p = (p.viewDocumentUid(uid)
                 .publish()
                 .relations()
                 .mySubscriptions()
                 .comments()
                 .history())
        p.logout()


if __name__ in ('main', '__main__'):
    unittest.main()
