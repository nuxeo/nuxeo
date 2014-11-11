# -*- coding: iso-8859-15 -*-
"""multiConversation FunkLoad test

$Id: $
"""
import unittest
from nuxeo.testcase import NuxeoTestCase
from nuxeo.pages import *

class Multiconversation(NuxeoTestCase):
    """
    This test use a configuration file Multiconversation.conf.
    """

    def setUp(self):
        NuxeoTestCase.setUp(self)

    def test_multiConversation(self):
        # The description should be set in the configuration file
        server_url = self.server_url
        p = LoginPage(self).login(self.cred_admin[0], self.cred_admin[1])

        self.get(server_url + "/nxpath/default/default-domain/workspaces@view_documents?conversationId=0NXMAIN",
            description="Get workspaces with conversation 0NXMAIN")

        self.post(server_url + "/view_documents.faces", params=[
            ['nxw_newWorkspace_form', 'nxw_newWorkspace_form'],
            ['javax.faces.ViewState', self.getLastJsfState()],
            ['nxw_newWorkspace_form:nxw_newWorkspace', 'nxw_newWorkspace_form:nxw_newWorkspace']],
            description="Get workspace creation page")

        self.post(server_url + "/create_workspace.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', 'aaaa'],
            ['document_create:nxl_heading:nxw_description', ''],
            ['document_create:nxw_documentCreateButtons_CREATE_WORKSPACE', 'Create'],
            ['javax.faces.ViewState', self.getLastJsfState()]],
            description="Create workspace")
        self.get(server_url + "/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3ATAB_WORKSPACE_EDIT&conversationId=0NXMAIN",
            description="Navigate to created workspace edit page")
        first_window_jsf_state = self.getLastJsfState()

        self.get(server_url + "/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A",
            description="Navigate using a new conversation (will be created because 0NXMAIN is not referenced)")

        # perform some navigations in the new conversation, at least 4 to test first page view restore
        self.get(server_url + "/nxpath/default/default-domain@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nxpath/defau...main@view_documents")
        self.get(server_url + "/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nxpath/defau...aces@view_documents")
        self.get(server_url + "/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nxpath/defau...6176@view_documents")
        self.get(server_url + "/nxpath/default/default-domain@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nxpath/defau...main@view_documents")
        self.get(server_url + "/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nxpath/defau...aces@view_documents")
        self.get(server_url + "/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nxpath/defau...6176@view_documents")

        # post edit page in first window
        self.post(server_url + "/view_documents.faces", params=[
            ['document_edit', 'document_edit'],
            ['document_edit:nxl_heading:nxw_title', 'aaaa'],
            ['document_edit:nxl_heading:nxw_description', 'bbbb'],
            ['document_edit:nxl_dublincore:nxw_nature', ''],
            ['document_edit:nxl_dublincore:nxw_rights', ''],
            ['document_edit:nxl_dublincore:nxw_source', ''],
            ['document_edit:nxl_dublincore:nxw_coverage:nxw_coverage_continent', ''],
            ['document_edit:nxl_dublincore:nxw_coverage:nxw_coverage_country', ''],
            ['document_edit:nxl_dublincore:nxw_format', ''],
            ['document_edit:nxl_dublincore:nxw_language', ''],
            ['document_edit:nxl_dublincore:nxw_expiredInputDate', ''],
            ['document_edit:nxl_dublincore:nxw_expiredInputCurrentDate', '07/2013'],
            ['document_edit:nxl_document_edit_form_options:nxw_document_edit_comment', ''],
            ['document_edit:edit_doc_EDIT_CURRENT_DOCUMENT', 'Save'],
            ['javax.faces.ViewState', first_window_jsf_state]],
            description="Post edit page for workspace using first window")
        # check that page is restored correctly (non regression test for NXP-11967)
        self.assert_("An error occurred" not in self.getBody())

        # remove created document
        p.getRootWorkspaces()
        p.deleteItem("aaaa")
        p.logout()

        # end of test -----------------------------------------------

    def tearDown(self):
        """Setting up test."""
        self.logd("tearDown.\n")



if __name__ in ('main', '__main__'):
    unittest.main()
