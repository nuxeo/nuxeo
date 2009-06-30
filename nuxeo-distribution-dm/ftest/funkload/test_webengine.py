# -*- coding: iso-8859-15 -*-
"""testWebengine FunkLoad test

$Id: $
"""
import unittest
import re
from funkload.Lipsum import Lipsum
from funkload.FunkLoadTestCase import FunkLoadTestCase
from funkload.utils import xmlrpc_get_credential
from nuxeo.webenginepage import WebenginePage

class Webengine(FunkLoadTestCase):
    """Basic test of nuxeo webengine"""
    server_url = None
    _lipsum = Lipsum()
    ws_title = "FLNXTEST Webengine workspace"
    ws_id = "flnxtest-webengine-workspace"
    dir_title = "FLNXTEST Page folder"
    dir_id = "flnxtest-page-folder"
    tag = "FLNXTEST"

    def setUp(self):
        self.server_url = self.conf_get('main', 'url')
        self.credential_host = self.conf_get('credential', 'host')
        self.credential_port = self.conf_getInt('credential', 'port')
        self.cred_admin = xmlrpc_get_credential(self.credential_host,
                                                self.credential_port,
                                                'admin')
        self.cred_member =  xmlrpc_get_credential(self.credential_host,
                                                  self.credential_port,
                                                  'members')
        self.logd("setUp")

    def test_testWebengine(self):
        # begin of test ---------------------------------------------
        p = WebenginePage(self)

        (p.home()
         .login(*self.cred_admin)
         .admin()
         .user_management())

        ws_path = '/'.join(('workspaces', self.ws_id))
        if p.viewDocumentPath(ws_path, raiseOn404=False) is None:
            p.createDocument('workspaces', self.ws_title,
                             doc_type="Workspace")
        parent = '/'.join((ws_path, self.dir_id))
        if p.viewDocumentPath(parent, raiseOn404=False) is None:
            p.createDocument(ws_path, self.dir_title,
                             doc_type="Folder")
        p.viewDocumentPath(parent)

        p.deleteDocument(ws_path)

        p.createUser('john', 'tiger', groups='members')
        p.logout()

        (p.home()
         .login('john', 'tiger')
         .home()
         .logout())

        (p.home()
         .login(*self.cred_admin)
         .deleteUser('john')
         .logout())

        # end of test -----------------------------------------------

    def tearDown(self):
        """Setting up test."""
        self.logd("tearDown.")



if __name__ in ('main', '__main__'):
    unittest.main()
