# -*- coding: iso-8859-15 -*-
"""multiConversation FunkLoad test

$Id: $
"""
import unittest
from funkload.FunkLoadTestCase import FunkLoadTestCase
from webunit.utility import Upload
from funkload.utils import Data
#from funkload.utils import xmlrpc_get_credential

class Multiconversation(FunkLoadTestCase):
    """XXX

    This test use a configuration file Multiconversation.conf.
    """

    def setUp(self):
        """Setting up test."""
        self.logd("setUp")
        self.server_url = self.conf_get('main', 'url')
        # XXX here you can setup the credential access like this
        # credential_host = self.conf_get('credential', 'host')
        # credential_port = self.conf_getInt('credential', 'port')
        # self.login, self.password = xmlrpc_get_credential(credential_host,
        #                                                   credential_port,
        # XXX replace with a valid group
        #                                                   'members')

    def test_multiConversation(self):
        # The description should be set in the configuration file
        server_url = self.server_url
        # begin of test ---------------------------------------------

        # /tmp/tmpYyvA29_funkload/watch0001.request
        self.post(server_url + "/nuxeo/nxstartup.faces", params=[
            ['user_name', 'Administrator'],
            ['user_password', 'Administrator'],
            ['language', 'en_US'],
            ['requestedUrl', ''],
            ['forceAnonymousLogin', ''],
            ['form_submitted_marker', ''],
            ['Submit', 'Log in']],
            description="Post /nuxeo/nxstartup.faces")
        # /tmp/tmpYyvA29_funkload/watch0003.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN",
            description="Get /nuxeo/nxpath/defau...aces@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0005.request
        self.post(server_url + "/nuxeo/view_documents.faces", params=[
            ['nxw_documentActionSubviewUpperList_1_newWorkspace_form', 'nxw_documentActionSubviewUpperList_1_newWorkspace_form'],
            ['javax.faces.ViewState', 'j_id2'],
            ['nxw_documentActionSubviewUpperList_1_newWorkspace_form:nxw_documentActionSubviewUpperList_1_newWorkspace', 'nxw_documentActionSubviewUpperList_1_newWorkspace_form:nxw_documentActionSubviewUpperList_1_newWorkspace']],
            description="Post /nuxeo/view_documents.faces")
        # /tmp/tmpYyvA29_funkload/watch0007.request
        self.post(server_url + "/nuxeo/create_workspace.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', 'aaaa'],
            ['document_create:nxl_heading:nxw_description', ''],
            ['document_create:create_doc_CREATE_WORKSPACE', 'Create'],
            ['javax.faces.ViewState', 'j_id3']],
            description="Post /nuxeo/create_workspace.faces")
        # /tmp/tmpYyvA29_funkload/watch0010.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN",
            description="Get /nuxeo/nxpath/defau...aces@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0011.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3A",
            description="Get /nuxeo/nxpath/defau...6176@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0012.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3A&conversationId=0NXMAIN",
            description="Get /nuxeo/nxpath/defau...6176@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0013.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3ATAB_WORKSPACE_EDIT&conversationId=0NXMAIN",
            description="Get /nuxeo/nxpath/defau...6176@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0016.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nuxeo/nxpath/defau...main@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0017.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nuxeo/nxpath/defau...aces@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0018.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nuxeo/nxpath/defau...6176@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0019.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nuxeo/nxpath/defau...main@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0020.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nuxeo/nxpath/defau...aces@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0021.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces/aaaa@view_documents?tabIds=%3A&conversationId=0NXMAIN2",
            description="Get /nuxeo/nxpath/defau...6176@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0022.request
        self.post(server_url + "/nuxeo/view_documents.faces", params=[
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
            ['javax.faces.ViewState', 'j_id8']],
            description="Post /nuxeo/view_documents.faces")
        # /tmp/tmpYyvA29_funkload/watch0024.request
        self.get(server_url + "/nuxeo/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN",
            description="Get /nuxeo/nxpath/defau...aces@view_documents")
        # /tmp/tmpYyvA29_funkload/watch0025.request
        self.post(server_url + "/nuxeo/view_documents.faces", params=[
            ['AJAXREQUEST', 'cv_document_content_0_region'],
            ['document_content', 'document_content'],
            ['javax.faces.ViewState', 'j_id16'],
            ['document_content:nxl_document_listing_ajax:nxw_listing_ajax_selection_box_with_current_document', 'on'],
            ['ajaxSingle', 'document_content:nxl_document_listing_ajax:nxw_listing_ajax_selection_box_with_current_document'],
            ['document_content:nxl_document_listing_ajax:nxw_listing_ajax_selection_box_with_current_document_ajax_onclick', 'document_content:nxl_document_listing_ajax:nxw_listing_ajax_selection_box_with_current_document_ajax_onclick'],
            ['AJAX:EVENTS_COUNT', '1']],
            description="Post /nuxeo/view_documents.faces")
        # /tmp/tmpYyvA29_funkload/watch0026.request
        self.post(server_url + "/nuxeo/view_documents.faces", params=[
            ['document_content_buttons:nxw_cvButton_CURRENT_SELECTION_TRASH_form', 'document_content_buttons:nxw_cvButton_CURRENT_SELECTION_TRASH_form'],
            ['document_content_buttons:nxw_cvButton_CURRENT_SELECTION_TRASH_form:nxw_cvButton_CURRENT_SELECTION_TRASH', 'Delete'],
            ['javax.faces.ViewState', 'j_id16']],
            description="Post /nuxeo/view_documents.faces")
        # /tmp/tmpYyvA29_funkload/watch0028.request
        self.get(server_url + "/nuxeo/logout",
            description="Get /nuxeo/logout")

        # end of test -----------------------------------------------

    def tearDown(self):
        """Setting up test."""
        self.logd("tearDown.\n")



if __name__ in ('main', '__main__'):
    unittest.main()
