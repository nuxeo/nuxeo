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
"""This test suite contains scnerii to test nuxeo/dav module.

This suite is configured using the Dav.conf file.
"""
import unittest
from funkload.Lipsum import Lipsum
from funkload.utils import Data
from funkload.FunkLoadTestCase import FunkLoadTestCase
from funkload.utils import xmlrpc_get_credential
from xml.dom.minidom import parse, parseString
from nuxeo.rest import RestAPI


class Dav(FunkLoadTestCase):
    ws_title = "FLNXTest-Dav-workspace"
    dir_title = "FLNXTEST-Dav-folder"
    tag = "FLNXTEST"
    _lipsum = Lipsum()

    def setUp(self):
        self.server_url = self.conf_get('main', 'url')
        self.credential_host = self.conf_get('credential', 'host')
        self.credential_port = self.conf_getInt('credential', 'port')
        self.cred_admin = xmlrpc_get_credential(self.credential_host,
                                                self.credential_port,
                                                'admin')
        self.dav_url = self.server_url + "/site/dav"
        self.nb_docs = self.conf_getInt('testWriter', 'nb_docs')
        self.cred_member = xmlrpc_get_credential(self.credential_host,
                                                 self.credential_port,
                                                 'members')

    def initWorkspace(self):
        """Create an initial workspace using rest,
        because DAV is not allowed to create workspace."""
        r = RestAPI(self)
        r.login(*self.cred_admin)
        root_uid = r.getRootWorkspaceUid()
        ws_uid = r.getChildUid(root_uid, self.ws_title, 'Workspace')
        if not ws_uid:
            ws_uid = r.createDocument(root_uid, 'Workspace', self.ws_title,
                                      'DAV Test workspace description.')

    def testDav(self):
        self.initWorkspace()
        self.setBasicAuth(*self.cred_admin)
        dav_url = self.dav_url

        resp = self.options(dav_url, description="option on root")
        dav = resp.headers.get('DAV')
        self.assert_(dav is not None)
        self.assert_(dav == '1,2')
        allow = resp.headers.get('Allow')
        for method in ['PROPPATCH', 'MKCOL', 'COPY', 'MOVE',
                        'LOCK', 'UNLOCK']:
            self.assert_(method in allow)

        resp = self.propfind(dav_url, depth=0,
                             description="propfind root depth0")
        # dom = parseString(self.getBody())
        #for node in dom.firstChild.childNodes:
        #    print node.toxml()
        resp = self.propfind(dav_url, depth=1,
                             description="propfind root depth1")
        dom = parseString(self.getBody())
        for node in dom.getElementsByTagName('ns2:href'):
            url = node.firstChild.data
            # print url

        url = dav_url + "/" + self.ws_title
        folder_url = url + "/" + self.dir_title
        self.delete(folder_url, ok_codes=[204, 404],
                    description="Remove folder")

        self.method("MKCOL", folder_url, ok_codes=[201, ],
                    description="Create a folder")
        resp = self.propfind(folder_url, depth=0,
                             description="propfind root depth0")

        # create file
        doc_url = folder_url + '/' + self._lipsum.getUniqWord() + '.txt'
        content = self._lipsum.getSentence()
        self.put(doc_url, params=Data(None, content),
                 description="Create a doc", ok_codes=[201, ])
        # self.delete(folder_url, ok_codes=[204, ],
        #            description="Remove folder")

        self.clearBasicAuth()

    def testWriter(self):
        self.initWorkspace()
        self.setBasicAuth(*self.cred_admin)
        dav_url = self.dav_url
        url = dav_url + "/" + self.ws_title

        folder_url = url + "/" + self._lipsum.getUniqWord()
        # create a folder
        self.method("MKCOL", folder_url, ok_codes=[201, ],
                    description="Create a folder")
        #resp = self.propfind(folder_url, depth=0,
        #                     description="propfind root depth0")
        # create files
        for i in range(self.nb_docs):
            doc_url = folder_url + '/' + self._lipsum.getUniqWord() + '.txt'
            content = self._lipsum.getParagraph()
            self.put(doc_url, params=Data(None, content),
                     description="Create a doc " + str(i), ok_codes=[201, ])
        self.clearBasicAuth()

    def testUserAgents(self):
        dav_url = self.dav_url
        # check that nuxeo ask for digest auth for the following ua
        uas = ["MSFrontPage 1", "Microsoft-WebDAV-MiniRedir 1", "DavClnt"
               "litmus 1", "gvfs 1", "davfs 1", "WebDAV 1", "cadaver 1"]
        for ua in uas:
            self.setUserAgent(ua)
            resp = self.propfind(dav_url,
                                  description="test ua auth for " + ua,
                                  ok_codes=[401, ])
            auth = resp.headers.get('WWW-Authenticate')
            # print auth
            self.assert_("Digest realm" in auth, "%s: %s" % (ua, auth))
        # unknown UA also requires authentication
        self.setUserAgent("FunkLoad")
        resp = self.propfind(dav_url,
                             description="test non DAV ua",
                             ok_codes=[401,])

    def testLocks(self):
        self.initWorkspace()
        self.setBasicAuth(*self.cred_admin)
        dav_url = self.dav_url
        url = dav_url + "/" + self.ws_title
        doc_url = url + "/" + "lockme"
        content = self._lipsum.getParagraph()

        self.delete(doc_url, ok_codes=[204, 404],
                    description="Remove doc if exists")

        self.put(doc_url, params=Data(None, content),
                 description="Create a doc", ok_codes=[201, ])

        data = Data('text/xml', """<?xml version="1.0" encoding="utf-8"?>
<?xml version="1.0" encoding="utf-8"?>
<lockinfo xmlns='DAV:'>
 <lockscope><exclusive/></lockscope>
<locktype><write/></locktype><owner>funkload test suite</owner>
</lockinfo>""")
        self.method("LOCK", doc_url, params=data, description="Lock")

        self.propfind(doc_url, ok_codes=[207, ],
                      description="Get info")
        # nothing in the response tell that the doc is locked :/
        # print self.getBody()
        self.method("UNLOCK", doc_url, ok_codes=[204, ],
                    description="Unlock")

        self.delete(doc_url, ok_codes=[204, ],
                    description="Remove doc")


    def testCreateUpdate(self):
        self.initWorkspace()
        self.setBasicAuth(*self.cred_admin)
        dav_url = self.dav_url
        url = dav_url + "/" + self.ws_title

        folder_url = url + "/" + self._lipsum.getUniqWord()
        # create a folder
        self.method("MKCOL", folder_url, ok_codes=[201, ],
                    description="Create a folder")
        # create files
        for i in range(self.nb_docs):
            doc_url = folder_url + '/' + self._lipsum.getUniqWord() + '.txt'
            content = self._lipsum.getParagraph()
            self.put(doc_url, params=Data(None, content),
                     description="Create a doc " + str(i), ok_codes=[201, ])
            content += self._lipsum.getParagraph() + " UPDATE"
            self.put(doc_url, params=Data(None, content),
                     description="Update doc " + str(i), ok_codes=[201, ])
            #self.delete(doc_url, ok_codes=[204, ], description="Delete doc")
        self.clearBasicAuth()


if __name__ in ('main', '__main__'):
    unittest.main()
