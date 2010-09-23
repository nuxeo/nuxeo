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
import time
from urllib import quote_plus
from webunit.utility import Upload
from utils import extractToken, extractJsfState, extractIframes, extractJsessionId
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
        fl.setHeader('Accept-Language', 'en')
        fl.post(fl.server_url + "/nxstartup.faces", params=[
            ['language', 'en_US'],
            ['user_name', user],
            ['user_password', password],
            ['form_submitted_marker', ''],
            ['requestedUrl', ''],
            ['Submit', 'Connexion']],
            description="Login " + user)
        fl.assert_('LoginFailed=true' not in fl.getLastUrl(),
                   'Login failed for %s:%s' % (user, password))
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

    def getRootSections(self):
        return self.viewDocumentPath("sections")

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

        fl.post(fl.server_url + '/view_users.faces', params=[
            ['createUserActionsForm:createUserButton', 'createUserActionsForm:createUserButton'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['createUserActionsForm_SUBMIT', '1']],
            description="View user creation form")

        jsfState = fl.getLastJsfState()

        fl.post(fl.server_url + '/create_user.faces', params=[
            ['AJAXREQUEST', 'createUser:nxl_user:nxw_groups_ajax_region'],
            ['createUser:nxl_user:nxw_username', username],
            ['createUser:nxl_user:nxw_firstname', firstname],
            ['createUser:nxl_user:nxw_lastname', lastname],
            ['createUser:nxl_user:nxw_company', company],
            ['createUser:nxl_user:nxw_email', email],
            ['createUser:nxl_user:nxw_firstPassword', password],
            ['createUser:nxl_user:nxw_secondPassword', password],
            ['createUser:nxl_user:nxw_passwordMatcher', 'needed'],
            ['createUser:nxl_user:nxw_groups_suggest', groups],
            ['createUser:nxl_user:nxw_groups_suggestionBox_selection', ''],
            ['createUser_SUBMIT', '1'],
            ['javax.faces.ViewState', jsfState],
            ['userSuggestionSearchType', 'GROUP_TYPE'],
            ['userSuggestionMaxSearchResults', '0'],
            ['ajaxSingle', 'createUser:nxl_user:nxw_groups_suggestionBox'],
            ['createUser:nxl_user:nxw_groups_suggestionBox', 'createUser:nxl_user:nxw_groups_suggestionBox'],
            ['inputvalue', groups],
            ['AJAX:EVENTS_COUNT', '1']],
            description="Create user search group")

        fl.post(fl.server_url + '/create_user.faces', params=[
            ['AJAXREQUEST', 'createUser:nxl_user:nxw_groups_ajax_region'],
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
            ['createUser_SUBMIT', '1'],
            ['javax.faces.ViewState', jsfState],
            ['createUser:nxl_user:nxw_groups_suggestionBox:nxw_groups_listRegion_select', 'createUser:nxl_user:nxw_groups_suggestionBox:nxw_groups_listRegion_select'],
            ['suggestionSelectionListId', 'nxw_groups_list'],
            ['suggestionInputSelectorId', 'nxw_groups_suggest']],
            description="Create user select group")

        fl.post(fl.server_url + "/create_user.faces", params=[
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
            ['createUser_SUBMIT', '1'],
            ['javax.faces.ViewState', jsfState]],
                description="Create user submit form")
        return self

    def dashboard(self):
        """jsf dashboard"""
        fl = self.fl
        self.viewDocumentUid(self.getDocUid(), outcome='user_dashboard',
                             description="View JSF dashboard")
        fl.assert_('My workspaces' in fl.getBody(),
                   'Not a dashboard page')
        return self

    def dashboardNew(self):
        """open social dashboard"""
        fl = self.fl
        server_url = fl.server_url
        fl.post(server_url + "/view_documents.faces", params=[
            ['userServicesForm:userServicesActionsTable:0:userServicesActionCommandLink', 'userServicesForm:userServicesActionsTable:0:userServicesActionCommandLink'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['userServicesForm_SUBMIT', '1']],
                description="Dashboard opensocial")

        ts = str(time.time())
        jid = extractJsessionId(fl)
        uid = extractToken(fl.getBody(), """return "{docRef:'""", "'")
        fl.assert_(len(uid) == 36)

        fl.get(server_url + "/org.nuxeo.opensocial.container.ContainerEntryPoint/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js",
                 description="Get container entry point")
        data =  Data('text/x-gwt-rpc; charset=utf-8', '''5|0|17|''' + server_url + '''/org.nuxeo.opensocial.container.ContainerEntryPoint/|9CCFB53A0997F1E4596C8EE4765CCBAA|org.nuxeo.opensocial.container.client.service.api.ContainerService|getContainer|java.util.Map|java.util.HashMap/962170901|java.lang.String/2004016611|docRef|''' + uid + '''|clientUrl|''' + server_url + '''/|windowWidth|10|nxBaseUrl|userLanguage|fr|locale|1|2|3|4|1|5|6|6|7|8|7|9|7|10|7|11|7|12|7|13|7|14|-5|7|15|7|16|7|17|-10|''')
        fl.post(server_url + "/gwtcontainer", data,
                description="dashboard gwt container")
        fl.assert_('//OK' in fl.getBody())

        # Extract iframes from the gwtcontainer response
        iframes = extractIframes(fl.getBody())
        fl.assert_(len(iframes))

        i = 0
        for iframe in iframes:
            i += 1
            # print "iframe: " + iframe
            fl.get(server_url + iframe,
                     description="dashboard iframe %d" % i)
            fl.assert_(fl.getBody().startswith('<html>'))

        fl.get(server_url + "/opensocial/gadgets/makeRequest?refresh=3600&url=" + quote_plus(server_url) + "%2FrestAPI%2Fdashboard%2FUSER_SITES%3Fformat%3DJSON%26page%3D0%26domain%3Ddefault-domain%26lang%3Den%26ts%3D12766046361930.9475744903817575&httpMethod=GET&headers=Cache-control%3Dno-cache%252C%2520must-revalidate%26X-NUXEO-INTEGRATED-AUTH%3D" + jid + "&postData=&authz=&st=&contentType=JSON&numEntries=3&getSummaries=false&signOwner=true&signViewer=true&gadget=" + quote_plus(server_url) + "%2Fsite%2Fgadgets%2Fuserdocuments%2Fusersites.xml&container=default&bypassSpecCache=1&nocache=0",
            description="dashboard req1: user sites")
        fl.assert_('USER_SITES' in fl.getBody())

        fl.post(server_url + "/opensocial/gadgets/makeRequest", params=[
            ['authz', ''],
            ['signOwner', 'true'],
            ['contentType', 'JSON'],
            ['nocache', '0'],
            ['postData', ''],
            ['headers', 'Cache-control=no-cache%2C%20must-revalidate&X-NUXEO-INTEGRATED-AUTH=' + jid],
            ['url', server_url + '/restAPI/workflowTasks/default?mytasks=false&format=JSON&ts=' + ts + '&lang=en&labels=workflowDirectiveValidation,workflowDirectiveOpinion,workflowDirectiveVerification,workflowDirectiveCheck,workflowDirectiveDiffusion,label.workflow.task.name,label.workflow.task.duedate,label.workflow.task.directive'],
            ['numEntries', '3'],
            ['bypassSpecCache', '1'],
            ['st', ''],
            ['httpMethod', 'GET'],
            ['signViewer', 'true'],
            ['container', 'default'],
            ['getSummaries', 'false'],
            ['gadget', server_url + '/site/gadgets/waitingfor/waitingfor.xml']],
            description="dashboard req2: other tasks")
        fl.assert_('Tasks for' in fl.getBody())

        fl.get(server_url + "/opensocial/gadgets/makeRequest?refresh=3600&url=" + quote_plus(server_url) + "%2FrestAPI%2Fdashboard%2FUSER_WORKSPACES%3Fformat%3DJSON%26page%3D0%26domain%3Ddefault-domain%26lang%3Den%26ts%3D12766046364186.08350334148753&httpMethod=GET&headers=Cache-control%3Dno-cache%252C%2520must-revalidate%26X-NUXEO-INTEGRATED-AUTH%3DEB4D8F264629C549917996193637A4F4&postData=&authz=&st=&contentType=JSON&numEntries=3&getSummaries=false&signOwner=true&signViewer=true&gadget=" + quote_plus(server_url) + "%2Fsite%2Fgadgets%2Fuserworkspaces%2Fuserworkspaces.xml&container=default&bypassSpecCache=1&nocache=0",
            description="dashboard req3: user workspaces")
        fl.assert_('USER_WORKSPACES' in fl.getBody())

        fl.post(server_url + "/opensocial/gadgets/makeRequest", params=[
            ['authz', ''],
            ['signOwner', 'true'],
            ['contentType', 'JSON'],
            ['nocache', '0'],
            ['postData', ''],
            ['headers', 'Cache-control=no-cache%2C%20must-revalidate&X-NUXEO-INTEGRATED-AUTH=EB4D8F264629C549917996193637A4F4'],
            ['url', server_url + '/restAPI/workflowTasks/default?mytasks=true&format=JSON&ts=' + ts + '&lang=en&labels=workflowDirectiveValidation,workflowDirectiveOpinion,workflowDirectiveVerification,workflowDirectiveCheck,workflowDirectiveDiffusion,label.workflow.task.name,label.workflow.task.duedate,label.workflow.task.directive'],
            ['numEntries', '3'],
            ['bypassSpecCache', '1'],
            ['st', ''],
            ['httpMethod', 'GET'],
            ['signViewer', 'true'],
            ['container', 'default'],
            ['getSummaries', 'false'],
            ['gadget', server_url + '/site/gadgets/tasks/tasks.xml']],
            description="dashboard req4: my tasks")
        fl.assert_('Tasks for' in fl.getBody())

        fl.get(server_url + "/opensocial/gadgets/makeRequest?refresh=3600&url=" + quote_plus(server_url) + "%2FrestAPI%2Fdashboard%2FRELEVANT_DOCUMENTS%3Fformat%3DJSON%26page%3D0%26domain%3Ddefault-domain%26lang%3Den%26ts%3D12766046370131.186666326174645&httpMethod=GET&headers=Cache-control%3Dno-cache%252C%2520must-revalidate%26X-NUXEO-INTEGRATED-AUTH%3DEB4D8F264629C549917996193637A4F4&postData=&authz=&st=&contentType=JSON&numEntries=3&getSummaries=false&signOwner=true&signViewer=true&gadget=" + quote_plus(server_url) + "%2Fsite%2Fgadgets%2Fuserdocuments%2Fuserdocuments.xml&container=default&bypassSpecCache=1&nocache=0",
            description="dashboard req5: relevant docs")
        fl.assert_('RELEVANT_DOCUMENTS' in fl.getBody())
        return self

    def personalWorkspace(self):
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['userServicesForm_SUBMIT', '1'],
            ['userServicesForm:userServicesActionsTable:1:userServicesActionCommandLink', 'userServicesForm:userServicesActionsTable:1:userServicesActionCommandLink']],
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
            ['userServicesSearchForm:simpleSearchKeywordsInput', query],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['userServicesSearchForm:simpleSearchSubmitButton', 'Search'],
            ['userServicesSearchForm_SUBMIT', '1']],
            description=description)
        fl.assert_('simple_search' in fl.getBody(),
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

    def publishOnFirstSection(self):
        """Publish in the first section"""
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['publishTreeForm', 'publishTreeForm'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['publishTreeForm:publishTree:publishRecursiveAdaptor:0::publishTreeNodeAjaxExpanded', 'true'],
            ['publishTreeForm:publishTree:publishRecursiveAdaptor:0::publishTreeNodeNodeExpanded', 'true'],
            ['publishTreeForm:publishSelectTreeName', 'DefaultSectionsTree-default-domain'],
            ['AJAXREQUEST', '_viewRoot'],
            ['publishTreeForm:publishTree:input', ''],
            ['autoScroll', '']],
            description="Publish view section root")

        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['publishTreeForm', 'publishTreeForm'],
            ['publishTreeForm:publishSelectTreeName', 'DefaultSectionsTree-default-domain'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['AJAXREQUEST', '_viewRoot'],
            ['publishTreeForm:publishTree:input', 'publishTreeForm:publishTree:publishRecursiveAdaptor:0:publishRecursiveAdaptor:1::publishTreeNode'],
            ['autoScroll', ''],
            ['publishTreeForm:publishTree:publishRecursiveAdaptor:0:publishRecursiveAdaptor:0::publishCommandLink', 'publishTreeForm:publishTree:publishRecursiveAdaptor:0:publishRecursiveAdaptor:0::publishCommandLink']],
            description="Publish document")
        fl.assert_("Unpublish" in fl.getBody())
        return self

    def relations(self):
        ret = self.viewDocumentUid(self.getDocUid(), tab='TAB_RELATIONS',
                                   description="View relations tab")
        self.fl.assert_('Add a new relation' in self.fl.getBody()
                        or 'No incoming or outgoing relation' in self.fl.getBody())
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
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['documentActionSubviewUpperListForm_SUBMIT', '1'],
            ['documentActionSubviewUpperListForm:documentActionSubviewUpperListTable:0:documentActionSubviewUpperListLink', 'documentActionSubviewUpperListForm:documentActionSubviewUpperListTable:0:documentActionSubviewUpperListLink']],
            description="Create workspace form")
        fl.assert_('nxw_title' in fl.getBody(),
                   "Workspace creation form not found.")

        fl.post(fl.server_url + "/create_workspace.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', title],
            ['document_create:nxl_heading:nxw_description', description],
            ['document_create:button_create', 'Create'],
            ['document_create_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
                description="Create workspace submit")
        fl.assert_('Workspace saved' in fl.getBody())
        return self

    def createSection(self, title, description):
        fl = self.fl
        server_url = fl.server_url
        fl.post(server_url + "/view_documents.faces", params=[
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['documentActionSubviewUpperListForm_SUBMIT', '1'],
            ['documentActionSubviewUpperListForm:documentActionSubviewUpperListTable:0:documentActionSubviewUpperListLink', 'documentActionSubviewUpperListForm:documentActionSubviewUpperListTable:0:documentActionSubviewUpperListLink']],
            description="Create a section form")
        fl.assert_('nxw_title' in fl.getBody(),
                   "Workspace creation form not found.")
        
        fl.post(server_url + "/create_document.faces", params=[
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['document_create:nxl_heading:nxw_description', description],
            ['document_create:nxl_heading:nxw_title', title],
            ['document_create:button_create', 'Create'],
            ['document_create_SUBMIT', '1']],
            description="Create a section submit")
        fl.assert_('Section saved' in fl.getBody())
        return self
        

    def createFolder(self, title, description):
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['selectDocumentTypeForCreationForm_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['selectDocumentTypeForCreationForm:selectDocumentTypeForCreationTable:1:selectDocumentTypeForCreationCategory:0:selectDocumentTypeForCreationCategoryTable:0:selectDocumentTypeForCreationCategoryTitleLink', 'selectDocumentTypeForCreationForm:selectDocumentTypeForCreationTable:1:selectDocumentTypeForCreationCategory:0:selectDocumentTypeForCreationCategoryTable:0:selectDocumentTypeForCreationCategoryTitleLink'],
            ['selectDocumentTypeForCreationForm:selectDocTypePanelOpenedState', '']],
            description="Create folder: New Folder")

        fl.assert_('document_create' in fl.getBody(),
                   "Folder form not found")
        fl.post(fl.server_url + "/create_document.faces", params=[
            ['document_create', 'document_create'],
            ['document_create:nxl_heading:nxw_title', title],
            ['document_create:nxl_heading:nxw_description', description],
            #['parentDocumentPath', '/default-domain/workspaces/flnxtest-page-workspace.1237992970017'],
            ['document_create:button_create', 'Create'],
            ['document_create_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
            description="Create folder: Submit")
        fl.assert_('Folder saved' in fl.getBody())
        return self

    def createFile(self, title, description, file_path=None):
        fl = self.fl
        fl.post(fl.server_url + "/view_documents.faces", params=[
            ['selectDocumentTypeForCreationForm_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['selectDocumentTypeForCreationForm:selectDocumentTypeForCreationTable:0:selectDocumentTypeForCreationCategory:0:selectDocumentTypeForCreationCategoryTable:1:selectDocumentTypeForCreationCategoryTitleLink', 'selectDocumentTypeForCreationForm:selectDocumentTypeForCreationTable:0:selectDocumentTypeForCreationCategory:0:selectDocumentTypeForCreationCategoryTable:1:selectDocumentTypeForCreationCategoryTitleLink'],
            ['selectDocumentTypeForCreationForm:selectDocTypePanelOpenedState', '']],
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
            ['document_create_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()]],
            description="Create file: Sumbit")
        fl.assert_('File saved' in fl.getBody())
        return self

    def selectItem(self, title, item_type="Workspace"):
        fl = self.fl
        conversation_id = self.getConversationId()
        folder_uid = self.getDocUid()
        html = fl.getBody()
        if item_type in ['Section', 'SectionRoot']:
            start = html.find('form id="section_content"')
        else:
            start = html.find('form id="document_content"')
        end = html.find(title, start)
        fl.assert_(end>0, 'Item with title "%s" not found.' % title)
        start = html.rfind('<tr class', start, end)

        # seam remoting selection is now done in ajax
#        doc_uid = extractToken(html[start:end], 'docRef:', '"')
#        fl.assert_(doc_uid, 'item "%s" not found.' % title)
#        sel = 'CURRENT_SELECTION'
#        if item_type == "Section":
#            sel = 'CURRENT_SELECTION_SECTIONS'
#        xml = '''<envelope><header><context><conversationId>%s</conversationId></context></header><body><call component="documentActions" method="checkCurrentDocAndProcessSelectRow" id="0">
#<params><param><str>%s</str></param><param><str>CURRENT_DOC_CHILDREN</str></param><param><str>%s</str></param><param><bool>true</bool></param><param><str>%s</str></param></params><refs></refs></call></body></envelope>''' % (
#            conversation_id, doc_uid, sel, folder_uid)
#        #print "%s" % xml
#        fl.post(fl.server_url + "/seam/resource/remoting/execute",
#                Data('application/xml; charset=UTF-8',
#                     xml),
#                description="Select document")
#        fl.assert_(sel + "_TRASH" in fl.getBody())

        checkbox_id = extractToken(html[start:end], 'input id="', '"')
        fl.assert_(checkbox_id, 'item "%s" not found.' % title)
        checkbox_ajax_onclick_id = checkbox_id + '_ajax_onclick'
        table_name = checkbox_id.split(':', 1)[0]

        params = [
            ['AJAXREQUEST', 'contentViewAjaxRegion_0'],
            [checkbox_id, 'on'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            [checkbox_ajax_onclick_id, checkbox_ajax_onclick_id],
            [table_name + '_SUBMIT', '1']
            ]
        fl.post(fl.server_url + "/view_documents.faces", params,
            description='Select document "%s"' % title)

        return self

    def deleteItem(self, title, item_type="Workspace"):
        fl = self.fl
        folder_uid = self.getDocUid()
        state = fl.getLastJsfState()
        self.selectItem(title, item_type)
        # position of the delete button
        table_name = "document_content"
        pos = '3'
        if item_type in ['Section', 'SectionRoot']:
            table_name = "section_content"
            pos = '0'
        params=[
            [table_name + ':nxl_document_listing_ajax:nxw_listing_ajax_selection_box_with_current_document', 'on'],
            ['javax.faces.ViewState', state],
            [table_name + ':clipboardActionsTable_0_0:' + pos + ':clipboardActionsButton', 'Delete'],
            [table_name + '_SUBMIT', '1']
            ]
        fl.post(fl.server_url + "/view_documents.faces", params,
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
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest', user],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox_selection', ''],
            ['add_rights_form:rights_grant_select', 'Grant'],
            ['add_rights_form:rights_permission_select', 'Everything'],
            ['add_rights_form_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            ['userSuggestionSearchType', ''],
            ['userSuggestionMaxSearchResults', '0'],
            ['ajaxSingle', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox'],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox'],
            ['inputvalue', user],
            ['AJAX:EVENTS_COUNT', '1']]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm search user.")
        fl.assert_(user in fl.getBody(), "User not found")

        state = fl.getLastJsfState()
        params = [
            ['AJAXREQUEST', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_ajax_region'],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest', user],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox_selection', '0'],
            ['add_rights_form:rights_grant_select', 'Grant'],
            ['add_rights_form:rights_permission_select', permission],
            ['add_rights_form_SUBMIT', '1'],
            ['javax.faces.ViewState', state],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:nxw_selection_listRegion_select', 'add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:nxw_selection_listRegion_select'],
            ['suggestionSelectionListId', 'nxw_selection_list'],
            ['suggestionInputSelectorId', 'nxw_selection_suggest']]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm select user " + user)

        params = [
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest', ''],
            ['add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox_selection', ''],
            ['add_rights_form:rights_grant_select', 'Grant'],
            ['add_rights_form:rights_permission_select', permission],
            ['add_rights_form:rights_add_button', 'Add permission'],
            ['add_rights_form_SUBMIT', '1'],
            ['javax.faces.ViewState', state]]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm %s to %s" % (permission, user))
        fl.assert_('Save local rights' in fl.getBody())

        params = [
            ['validate_rights:document_rights_validate_button', 'Save local rights'],
            ['validate_rights_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()]]
        fl.post(server_url + "/view_documents.faces", params,
                  description="Grant perm apply")
        fl.assert_('Rights updated' in fl.getBody())
        return self

    def sort(self, column):
        fl = self.fl
        server_url = fl.server_url
        fl.assert_('document_content' in fl.getBody(),
                   'Not a folder listing page.')

        options = {'date': ['document_content:nxl_document_listing_ajax:listing_modification_date_header_sort',
                            'document_content:nxl_document_listing_ajax:listing_modification_date_header_sort'],
                   'author': ['document_content:nxl_document_listing_ajax:listing_author_header_sort',
                              'document_content:nxl_document_listing_ajax:listing_author_header_sort'],
                   'lifecycle': ['document_content:nxl_document_listing_ajax:listing_lifecycle_header_sort',
                                 'document_content:nxl_document_listing_ajax:listing_lifecycle_header_sort'],
                   'title': ['document_content:nxl_document_listing_ajax:listing_title_link_header_sort',
                             'document_content:nxl_document_listing_ajax:listing_title_link_header_sort'],
                   }
        fl.assert_(column in options.keys(), 'Invalid sort column')
        # date
        fl.post(server_url + "/view_documents.faces", params=[
            ['document_content_SUBMIT', '1'],
            ['javax.faces.ViewState', fl.getLastJsfState()],
            options[column]],
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

