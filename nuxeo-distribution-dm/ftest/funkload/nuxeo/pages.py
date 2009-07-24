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
"""
This modules is tied with the Nuxeo EP application.

TODO:

Folder

* emptyTrash()

* next() previous()

Dashboard

* wsRefresh()

Document

* files().add(filename, path)

* comments().add(comment)

* relation().add(uid)

* publish(section_uid)
"""
import random
from webunit.utility import Upload
from utils import extractToken, extractJsfState
from funkload.utils import Data

class BasePage:
    """Base class for nuxeo ep page."""
    fl = None

    def __init__(self, fl):
        self.fl = fl

    # helpers
    def getDocUid(self):
        fl = self.fl
        uid = extractToken(fl.getBody(), "var currentDocURL='default/", "'")
        fl.assert_(uid, 'Current document uid not found.')
        return uid

    def getConversationId(self):
        fl = self.fl
        cId = extractToken(fl.getBody(), "var currentConversationId='", "'")
        fl.assert_(cId, 'Current conversation id not found')
        return cId

    def available(self):
        """Check if the server is available."""
        fl = self.fl
        fl.get(fl.server_url + '/login.jsp',
               description="Check if the server is alive")


    # pages
    def logout(self):
        """Log out the current user."""
        fl = self.fl
        fl.get(fl.server_url + '/logout',
               description="Log out")
        fl.assert_('login' in fl.getLastUrl(),
                     "Not redirected to login page.")
        fl.current_login = None
        return LoginPage(self.fl)

    def login(self, user, password):
        fl = self.fl
        fl.post(fl.server_url + "/nxstartup.faces", params=[
            ['user_name', user],
            ['user_password', password],
            ['form_submitted_marker', ''],
            ['Submit', 'Connexion']],
            description="Login " + user)
        fl.assert_('LoginFailed=true' not in fl.getLastUrl(),
                   'Login failed for %s:%s' % (user, password))
        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['langSelectForm', 'langSelectForm'],
            ['langSelectForm:langSelectMenu', 'en_GB'],
            ['langSelectForm:langSelectSubmitButton', 'Change'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
            description="Change locale to en_US")
        fl.assert_(fl.listHref(content_pattern="Log out"),
                   "No log out link found on the welcome page")
        fl.current_login = user
        return FolderPage(self.fl)

    def loginInvalid(self, user, password):
        fl = self.fl
        fl.post(fl.server_url + "/nxstartup.faces", params=[
            ['user_name', user],
            ['user_password', password],
            ['form_submitted_marker', ''],
            ['Submit', 'Connexion']],
            description="Login invalid user " + user)
        fl.assert_('loginFailed=true' in fl.getLastUrl(),
                   'Invalid login expected for %s:%s.' %  (user, password))
        return self

    def viewDocumentPath(self, path, description=None, raiseOn404=True,
                         outcome=None):
        """This method return None when raiseOn404 is False and the document
        does not exist"""
        fl = self.fl
        if not description:
            description = "View document path:" + path
        ok_codes = [200, 301, 302, 303, 307]
        if not raiseOn404:
            ok_codes.append(404)
        if not outcome:
            outcome = "view_documents"
        resp = fl.get(fl.server_url + "/nxpath/default/default-domain/" +
               path + "@" + outcome,
               description=description, ok_codes=ok_codes)
        if resp.code == 404:
            fl.logi('Document ' + path + ' does not exists.')
            return None
        return self

    def viewDocumentUid(self, uid, tab='', subtab='', description=None,
                        outcome=None):
        fl = self.fl
        if not description:
            description = "View document uid:" + uid + ' ' + tab + subtab
        if not outcome:
            outcome = "view_documents"

        url = '/nxdoc/default/' + uid + '/' + outcome + '?tabId=' + tab
        if subtab:
            url += "&subTabId=" + subtab
        url += '=&conversationId=0NXMAIN1'
        fl.get(fl.server_url + url,
               description=description)
        return self

    def getRootWorkspaces(self):
        return self.viewDocumentPath("workspaces")

    def memberManagement(self):
        fl = self.fl
        self.viewDocumentUid(self.getDocUid(), outcome='view_users',
                             description="View member management")
        return self

    def createUser(self, username, email, password, firstname='',
                   lastname='', company='', groups=''):
        """This method does not raise exception if user already exists"""
        fl = self.fl
        self.memberManagement()

        # TODO: NXBT-77 navigate to create user form
        fl.post(fl.server_url + '/view_users.faces', params=[
            ['j_id192', 'j_id192'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['j_id192:j_id194', 'j_id192:j_id194']],
                description="View user creation form")

        fl.post(fl.server_url + '/create_user.faces', params=[
            ['AJAXREQUEST', 'createUser:nxl_user:nxw_groups_ajax_region'],
            ['createUser', 'createUser'],
            ['createUser:nxl_user:nxw_username', username],
            ['createUser:nxl_user:nxw_firstname', firstname],
            ['createUser:nxl_user:nxw_lastname', lastname],
            ['createUser:nxl_user:nxw_company', company],
            ['createUser:nxl_user:nxw_email', email],
            ['createUser:nxl_user:nxw_firstPassword', password],
            ['createUser:nxl_user:nxw_secondPassword', password],
            ['createUser:nxl_user:nxw_passwordMatcher', 'needed'],
            ['createUser:nxl_user:nxw_groups_suggest', groups],
            ['createUser:nxl_user:nxw_groups_suggestionBox_selection', '0'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['createUser:nxl_user:nxw_groups_suggestionBox:nxw_groups_listRegion_select', 'createUser:nxl_user:nxw_groups_suggestionBox:nxw_groups_listRegion_select'],
            ['suggestionInputSelectorId', 'nxw_groups_suggest'],
            ['suggestionSelectionListId', 'nxw_groups_list']],
                description="Create user select group")

        fl.post(fl.server_url + "/create_user.faces", params=[
            ['createUser', 'createUser'],
            ['createUser:nxl_user:nxw_username', username],
            ['createUser:nxl_user:nxw_firstname', firstname],
            ['createUser:nxl_user:nxw_lastname', lastname],
            ['createUser:nxl_user:nxw_company', company],
            ['createUser:nxl_user:nxw_email', email],
            ['createUser:nxl_user:nxw_firstPassword', password],
            ['createUser:nxl_user:nxw_secondPassword', password],
            ['createUser:nxl_user:nxw_passwordMatcher', 'needed'],
            ['createUser:nxl_user:nxw_groups_suggest', ''],
            ['createUser:nxl_user:nxw_groups_suggestionBox_selection', ''],
            ['createUser:button_create', 'Save'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
                description="Submit user form")
        return self

    def dashboard(self):
        fl = self.fl
        self.viewDocumentUid(self.getDocUid(), outcome='user_dashboard',
                             description="View dashboard")
        fl.assert_('My workspaces' in fl.getBody(),
                   'Not a dashboard page')
        return self

    def personalWorkspace(self):
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            # TODO: NXBT-77 action view personal workspace
            ['j_id16', 'j_id16'],
            ['j_id16:j_id18', ''],
            ['j_id16:j_id19', 'KEYWORDS'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['j_id16:j_id30:1:j_id32', 'j_id16:j_id30:1:j_id32']],
                description="View personal workspace")
        # XXX: not working: post initializes personal workspace if it does
        # not exist...
        #self.viewDocumentPath("UserWorkspaces/" + fl.current_login,
        #                      description="View personal workspace")
        return self

    def search(self, query, description=None):
        fl = self.fl
        description = description and description or 'Search ' + query

        if '/search_results_simple.faces' in fl.getBody():
            action = '/search/search_results_simple.faces'
        else:
            action = '/view_documents.faces'
        fl.post(fl.server_url + action, params=[
            # TODO: NXBT-77 action search
            ['j_id16', 'j_id16'],
            ['j_id16:j_id18', query],
            ['j_id16:j_id19', 'KEYWORDS'],
            ['j_id16:j_id20', 'Search'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
                    description=description)
        fl.assert_('SEARCH_DOCUMENT_LIST' in fl.getBody(),
                     'Not a search result page')
        return self


    def edit(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='TAB_EDIT',
                                   description="View edit tab")
        self.fl.assert_('document_edit' in self.fl.getBody())
        return ret

    def files(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='TAB_FILES_EDIT',
                                   description="View files tab")
        self.fl.assert_('Upload your file' in self.fl.getBody())
        return ret

    def publish(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='TAB_PUBLISH',
                                   description="View publish tab")
        self.fl.assert_('Sections' in self.fl.getBody())
        return ret

    def relations(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='TAB_RELATIONS',
                                   description="View relations tab")
        self.fl.assert_('Add a new relation' in self.fl.getBody()
                        or 'No incoming nor outgoing relation' in self.fl.getBody())
        return ret

    def workflow(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='TAB_CONTENT_JBPM',
                                   description="View workflow tab")
        self.fl.assert_('startWorkflow' in self.fl.getBody() or
                        'No workflows are currently active' in self.fl.getBody())
        return ret

    def mySubscriptions(self):
        ret = self.viewDocumentUid(self.getDocUid(),
                                   tab='TAB_MY_SUBSCRIPTIONS',
                                   description="View my subscriptions tab")
        self.fl.assert_('notifications' in self.fl.getBody())
        return ret

    def manageSubscriptions(self):
        """Only available for manager."""
        ret = self.viewDocumentUid(self.getDocUid(),
                                   tab='TAB_MANAGE_SUBSCRIPTIONS',
                                   description="View manage subscriptions tab")
        self.fl.assert_('add_subscriptions' in self.fl.getBody())
        return ret

    def comments(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='view_comments',
                                   description="View comments tab")
        self.fl.assert_('Add a comment' in self.fl.getBody())
        return ret

    def history(self):
        ret = self.viewDocumentUid(self.getDocUid(),
                                   tab='TAB_CONTENT_HISTORY',
                                   description="View history tab")
        self.fl.assert_('Event log' in self.fl.getBody())
        return ret

    def manage(self):
        ret = self.viewDocumentUid(self.getDocUid(),
                                   tab='TAB_MANAGE',
                                   description="View manage tab")
        return ret



class LoginPage(BasePage):
    """The Login page."""
    def view(self):
        fl = self.fl
        fl.get(fl.server_url + '/login.jsp',
               description='View Login page')
        fl.assert_('user_password' in fl.getBody())
        return self


class FolderPage(BasePage):
    """Folder page"""

    def createWorkspace(self, title, description):
        fl = self.fl

        fl.post(fl.server_url + "/view_documents.faces", params=[
            # TODO: NXBT-77 navigate to create workspace form
            ['j_id248', 'j_id248'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['j_id248:j_id249:j_id251:0:j_id252', 'j_id248:j_id249:j_id251:0:j_id252']],
            description="Create workspace form")
        fl.assert_('nxw_title' in fl.getBody(),
                   "Workspace creation form not found.")

        fl.post(fl.server_url + "/create_workspace.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', title],
            ['document_create:nxl_heading:nxw_description', description],
            ['document_create:button_create', 'Create'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
                description="Create workspace submit")
        fl.assert_('Workspace saved' in fl.getBody())
        return self

    def createFolder(self, title, description):
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            # TODO: NXBT-77 navigate to create folder form
            ['j_id212', 'j_id212'],
            ['j_id212:selectDocTypePanelOpenedState', ''],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['j_id212:j_id218:0:j_id222:0:j_id224:2:j_id228',
             'j_id212:j_id218:0:j_id222:0:j_id224:2:j_id228']],
            description="Create folder: New Folder")
        fl.assert_('document_create' in fl.getBody(),
                   "Folder form not found")
        fl.post(fl.server_url + "/create_document.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', title],
            ['document_create:nxl_heading:nxw_description', description],
            #['parentDocumentPath', '/default-domain/workspaces/flnxtest-page-workspace.1237992970017'],
            ['document_create:button_create', 'Create'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
            description="Create folder: Submit")
        fl.assert_('Folder saved' in fl.getBody())
        return self

    def createFile(self, title, description, file_path=None):
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            # TODO: NXBT-77 navigate to create file form
            ['j_id212', 'j_id212'],
            ['j_id212:selectDocTypePanelOpenedState', ''],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['j_id212:j_id218:1:j_id222:0:j_id224:2:j_id228',
             'j_id212:j_id218:1:j_id222:0:j_id224:2:j_id228']],
            description="Create file: New document")
        fl.assert_('document_create' in fl.getBody(),
                   "File form not found")
        fl.post(fl.server_url + "/create_document.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', title],
            ['document_create:nxl_heading:nxw_description', description],
            ['document_create:nxl_file:nxw_file:nxw_file_file:choice',
             file_path and 'upload' or 'none'],
            ['document_create:nxl_file:nxw_file:nxw_file_file:upload',
             Upload(file_path or '')],
            ['document_create:button_create', 'Create'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
            description="Create file: Sumbit")
        fl.assert_('File saved' in fl.getBody())
        return self

    def selectItem(self, title):
        fl = self.fl
        conversation_id = self.getConversationId()
        folder_uid = self.getDocUid()
        html = fl.getBody()
        start = html.find('form id="CHILDREN_DOCUMENT_LIST"')
        end = html.find(title, start)
        fl.assert_(end>0, 'Item with title "%s" not found.' % title)
        start = html.rfind('<tr class', start, end)
        doc_uid = extractToken(html[start:end], 'docRef:', '"')
        fl.assert_(doc_uid, 'item "%s" not found.' % title)
        xml = '''<envelope><header><context><conversationId>%s</conversationId></context></header><body><call component="documentActions" method="checkCurrentDocAndProcessSelectRow" id="0">
<params><param><str>%s</str></param><param><str>CURRENT_DOC_CHILDREN</str></param><param><str>CURRENT_SELECTION</str></param><param><bool>true</bool></param><param><str>%s</str></param></params><refs></refs></call></body></envelope>''' % (
            conversation_id, doc_uid, folder_uid)
        #print "%s" % xml
        fl.post(fl.server_url + "/seam/resource/remoting/execute",
                Data('application/xml; charset=UTF-8',
                     xml),
                description="Select document")
        fl.assert_('SELECTION_ADDTOLIST' in fl.getBody())
        return self

    def deleteItem(self, title):
        fl = self.fl
        folder_uid = self.getDocUid()
        state = fl.getLastJsfState()
        self.selectItem(title)
        # TODO: NXBT-77 delete action
        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['CHILDREN_DOCUMENT_LIST', 'CHILDREN_DOCUMENT_LIST'],
            ['CHILDREN_DOCUMENT_LIST:dataTable:0:document_checkbox_select', 'on'],
            ['CHILDREN_DOCUMENT_LIST:j_id362:1:j_id364', 'Delete'],
            ['javax.faces.ViewState', state]],
            description='Delete document "%s"' % title)
        fl.assert_('Document(s) deleted' in fl.getBody())
        return self

    def view(self):
        """Default summary tab."""
        self.viewDocumentUid(self.getDocUid())
        return self

    def rights(self):
        """Go to rights tab."""
        self.viewDocumentUid(self.getDocUid(), "TAB_MANAGE", "TAB_RIGHTS")
        self.fl.assert_('Local rights' in self.fl.getBody())
        return self

    def grant(self, permission, user):
        """Grant perm to user."""
        fl = self.fl
        fl.assert_('Local rights' in fl.getBody(),
                   'Current page is not a rights tab.')
        server_url = fl.server_url
        params = [
            ['AJAXREQUEST', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_ajax_region'],
            ['add_rights_form', 'add_rights_form'],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest', user],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox_selection', '0'],
            ['add_rights_form:rights_grant_select', 'Grant'],
            ['add_rights_form:rights_permission_select', 'Read'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['suggestionInputSelectorId', 'nxw_selection_suggest'],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:nxw_selection_listRegion_select', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:nxw_selection_listRegion_select'],
            ['suggestionSelectionListId', 'nxw_selection_list']]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm search user.")
        fl.assert_(user in fl.getBody(), "User not found")

        state = fl.getLastJsfState()
        params = [
            ['AJAXREQUEST', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_ajax_region'],
            ['add_rights_form', 'add_rights_form'],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest', user],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox_selection', '0'],
            ['add_rights_form:rights_grant_select', 'Grant'],
            ['add_rights_form:rights_permission_select', 'Read'],
            ['javax.faces.ViewState', state],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:nxw_selection_listRegion_select', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:nxw_selection_listRegion_select'],
            ['suggestionInputSelectorId', 'nxw_selection_suggest'],
            ['suggestionSelectionListId', 'nxw_selection_list']]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm select user " + user)

        params = [
            ['add_rights_form', 'add_rights_form'],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest', ''],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox_selection', ''],
            ['add_rights_form:rights_grant_select', 'Grant'],
            ['add_rights_form:rights_permission_select', permission],
            ['add_rights_form:rights_add_button', 'Add permission'],
            ['javax.faces.ViewState', state]]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm %s to %s" % (permission, user))
        fl.assert_('Save local rights' in fl.getBody())

        params = [
            ['validate_rights', 'validate_rights'],
            ['validate_rights:document_rights_validate_button', 'Save local rights'],
            ['javax.faces.ViewState', fl.getLastJsfState()]]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm apply")
        fl.assert_('Rights updated' in fl.getBody())
        return self

    def sort(self, column):
        fl = self.fl
        server_url = fl.server_url
        fl.assert_('CHILDREN_DOCUMENT_LIST' in fl.getBody(),
                   'Not a folder listing page.')
        options = {'date': [['CHILDREN_DOCUMENT_LIST:dataTable:header_modified_sort',
                             'CHILDREN_DOCUMENT_LIST:dataTable:header_modified_sort'],
                            ['sortColumn', 'dc:modified']],
                   'author': [['CHILDREN_DOCUMENT_LIST:dataTable:header_creator_sort',
                               'CHILDREN_DOCUMENT_LIST:dataTable:header_creator_sort'],
                              ['sortColumn', 'dc:creator']],
                   'lifecycle':[['CHILDREN_DOCUMENT_LIST:dataTable:header_currentLifeCycleState',
                                 'CHILDREN_DOCUMENT_LIST:dataTable:header_currentLifeCycleState'],
                                ['sortColumn', 'ecm:currentLifeCycleState']],
                   'title': [['CHILDREN_DOCUMENT_LIST:dataTable:header_title_sort',
                              'CHILDREN_DOCUMENT_LIST:dataTable:header_title_sort'],
                             ['sortColumn', 'dc:title']]
                   }
        fl.assert_(column in options.keys(), 'Invalid sort column')
        # date
        fl.post(server_url + "/view_documents.faces", params=[
            ['CHILDREN_DOCUMENT_LIST', 'CHILDREN_DOCUMENT_LIST'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['providerName', 'CURRENT_DOC_CHILDREN']] + options[column],
                description="Sort by " + column)
        return self

    def viewRandomDocument(self, pattern):
        fl = self.fl
        hrefs = fl.listHref(content_pattern=pattern,
                            url_pattern='@view_documents')
        fl.assert_(len(hrefs), "No doc found with pattern: " + pattern)
        doc_url = random.choice(hrefs)
        fl.get(doc_url, description="View a random document")
        return DocumentPage(self.fl)


class DocumentPage(BasePage):
    """Document page."""

