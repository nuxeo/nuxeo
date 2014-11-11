# -*- coding: iso-8859-15 -*-
"""dam FunkLoad test

$Id: $
"""
import os
import random
import unittest
from datetime import datetime
from funkload.FunkLoadTestCase import FunkLoadTestCase
from webunit.utility import Upload
from funkload.utils import Data
from utils import extractJsfState
from utils import getRandomLines
from funkload.Lipsum import Lipsum

africa_countries = (
    'Algeria', 'Angola', 'Benin', 'Botswana', 'Burundi', 'Cameroon',
    'Cape_Verde', 'Central_African_Republic', 'Chad', 'Comoros',
    'Congo_Republic', 'Congo_Democratic_Republic', 'Cote_D_Ivoire',
    'Djibouti', 'Egypt', 'Equatorial_Guinea', 'Eritrea', 'Ethiopia', 'Gabon',
    'Gambia', 'Ghana', 'Guinea', 'Guinea_Bissau', 'Kenya', 'Lesotho',
    'Liberia', 'Libyan_Arab_Jamahiriya', 'Madagascar', 'Malawi', 'Mali',
    'Mauritania', 'Mauritius', 'Mayotte', 'Morocco', 'Mozambique', 'Namibia',
    'Niger', 'Nigeria', 'Reunion', 'Rwanda', 'St_Helena',
    'Sao_Tome_and_Principe', 'Senegal', 'Seychelles', 'Sierra_Leone',
    'Somalia', 'South_Africa', 'Sudan', 'Swaziland', 'Tanzania', 'Togo',
    'Tunisia', 'Uganda', 'Western_Sahara', 'Zambia', 'Zimbabwe')


class Dam(FunkLoadTestCase):
    """
    This test use a configuration file Dam.conf.
    """
    _lipsum = Lipsum()

    def getLastJsfState(self):
        return extractJsfState(self.getBody())

    def setUp(self):
        """Setting up test."""
        self.logd("setUp")
        self.server_url = self.conf_get('main', 'url')
        self.input_files = self.conf_get('test_import', 'input_files')
        self.input_count = self.conf_getInt('test_import', 'input_count')
        self.user = self.conf_get('test_import', 'user')
        self.pwd = self.conf_get('test_import', 'password')

    def test_available(self):
        server_url = self.server_url
        self.get(server_url,
                 description="Check if the server is alive")

    def test_import(self):
        # The description should be set in the configuration file
        server_url = self.server_url
        lipsum = self._lipsum

        # begin of test ---------------------------------------------
        self.post(server_url + "/nxstartup.faces", params=[
            ['user_name', self.user],
            ['user_password', self.pwd],
            ['language', 'en'],
            ['form_submitted_marker', ''],
            ['Submit', 'Connexion']],
            description="Login " + self.user)
        self.assert_('Log out' in self.getBody(), "Access denied")

        for filepath in getRandomLines(self.input_files, self.input_count):
            # import file loop
            if not os.path.exists(filepath):
                self.logi('File not found skipping: ' + filepath)
                continue
            tag = 'FLNX' + lipsum.getUniqWord()
            self.logd("### Import file: " +  filepath + " with tag " + tag)
            filename = os.path.basename(filepath)
            title = tag
            descr = title + ' ' +  filename + '. ' + lipsum.getParagraph(1)
            author = lipsum.getWord().capitalize()
            country = random.choice(africa_countries)
            today = datetime.today().strftime('%m/%d/%y')
            # rich face random uid
            upload_uid = str(random.random())
            #upload_uid = '0.2156772209657476'

            self.post(server_url + "/view_documents.faces", params=[
                ['AJAXREQUEST', 'j_id84:j_id85'],
                ['j_id84', 'j_id84'],
                ['javax.faces.ViewState', self.getLastJsfState()],
                ['j_id84:importset_creation_button',
                 'j_id84:importset_creation_button']],
                      description="Import form")
            self.assert_('importset_form' in self.getBody())

            self.post(server_url + "/view_documents.faces?_richfaces_upload_uid=" + upload_uid + "&fileUploadForm=fileUploadForm&_richfaces_upload_file_indicator=true&AJAXREQUEST=j_id23", params=[
                ['fileUploadForm:file', Upload(filepath)],
                ['javax.faces.ViewState', self.getLastJsfState()]],
                      description="Upload file" + filename)

            state = self.getLastJsfState()
            self.post(server_url + "/view_documents.faces", params=[
                ['AJAXREQUEST', 'j_id23'],
                ['fileUploadForm:file', title],
                ['fileUploadForm:file', ''],
                ['javax.faces.ViewState', state],
                ['fileUploadForm:_form_link_hidden_', 'fileUploadForm'],
                ['ajaxSingle', 'fileUploadForm'],
                ['_richfaces_upload_uid', upload_uid],
                ['fileUploadForm', 'fileUploadForm'],
                ['_richfaces_file_upload_action', 'progress']],
                      description="Update import form")

            self.post(server_url + "/view_documents.faces", params=[
                ['AJAXREQUEST', 'importset_form:nxl_dublincore:nxw_coverage:nxw_coverage_region'],
                ['importset_form:nxl_heading:nxw_title', title],
                ['importset_form:nxl_heading:nxw_description', descr],
                ['importset_form:nxl_damc:nxw_author', 'auth'],
                ['importset_form:nxl_damc:nxw_authoringDate', today],
                ['importset_form_link_hidden_', 'importset_form:nxl_damc:nxw_authoringDate:trigger'],
                ['importset_form:nxl_dublincore:nxw_coverage:nxw_coverage_continent', 'africa'],
                ['importset_form:nxl_dublincore:nxw_coverage:nxw_coverage_country', ''],
                ['importset_form:nxl_dublincore:nxw_topic:nxw_topic_topic', 'art'],
                ['importset_form:nxl_dublincore:nxw_topic:nxw_topic_subtopic', ''],
                ['importset_form', 'importset_form'],
                ['autoScroll', ''],
                ['javax.faces.ViewState',  state],
                ['importset_form:nxl_dublincore:nxw_coverage:j_id67',
                 'importset_form:nxl_dublincore:nxw_coverage:j_id67']],
                      description="Get list of country")

            self.post(server_url + "/view_documents.faces", params=[
                ['AJAXREQUEST', 'j_id23'],
                ['importset_form:nxl_heading:nxw_title', title],
                ['importset_form:nxl_heading:nxw_description', descr],
                ['importset_form:nxl_damc:nxw_author', author],
                ['importset_form:nxl_damc:nxw_authoringDate', today],
                ['importset_form_link_hidden_', 'importset_form:nxl_damc:nxw_authoringDate:trigger'],
                ['importset_form:nxl_dublincore:nxw_topic:nxw_topic_topic', 'art'],
                ['importset_form:nxl_dublincore:nxw_topic:nxw_topic_subtopic', 'cinema'],
                ['importset_form:nxl_dublincore:nxw_coverage:nxw_coverage_continent', 'africa'],
                ['importset_form:nxl_dublincore:nxw_coverage:nxw_coverage_country', country],
                ['importset_form', 'importset_form'],
                ['autoScroll', ''],
                ['javax.faces.ViewState',  state],
                ['importset_form:importSetFormOk', 'importset_form:importSetFormOk']],
                      description="Submit metadata")
            # XXX no way to check success except by searching


        self.get(server_url + "/logout",
                 description="Logout")

        # end of test -----------------------------------------------

    def tearDown(self):
        """Setting up test."""
        self.logd("tearDown.\n")



if __name__ in ('main', '__main__'):
    unittest.main()
