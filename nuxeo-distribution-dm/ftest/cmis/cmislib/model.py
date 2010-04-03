#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#   Authors:
#    Jeff Potts, Optaros
#
"""
Module containing the domain objects used to work with a CMIS provider.
"""
from cmislib.net import RESTService as Rest
from cmislib.exceptions import CmisException, RuntimeException, \
    ObjectNotFoundException, InvalidArgumentException, \
    PermissionDeniedException, NotSupportedException, \
    UpdateConflictException
from cmislib import messages
from urllib import quote_plus
from urllib2 import HTTPError
import re
import mimetypes
from xml.parsers.expat import ExpatError
import datetime
import time

# would kind of like to not have any parsing logic in this module,
# but for now I'm going to put the serial/deserialization in methods
# of the CMIS object classes
from xml.dom import minidom

# Namespaces
ATOM_NS = 'http://www.w3.org/2005/Atom'
APP_NS = 'http://www.w3.org/2007/app'
CMISRA_NS = 'http://docs.oasis-open.org/ns/cmis/restatom/200908/'
CMIS_NS = 'http://docs.oasis-open.org/ns/cmis/core/200908/'

# Content types
# Not all of these patterns have variability, but some do. It seemed cleaner
# just to treat them all like patterns to simplify the matching logic
ATOM_XML_TYPE = 'application/atom+xml'
ATOM_XML_ENTRY_TYPE = 'application/atom+xml;type=entry'
ATOM_XML_ENTRY_TYPE_P = re.compile('^application/atom\+xml.*type.*entry')
ATOM_XML_FEED_TYPE = 'application/atom+xml;type=feed'
ATOM_XML_FEED_TYPE_P = re.compile('^application/atom\+xml.*type.*feed')
CMIS_TREE_TYPE = 'application/cmistree+xml'
CMIS_TREE_TYPE_P = re.compile('^application/cmistree\+xml')
CMIS_QUERY_TYPE = 'application/cmisquery+xml'
CMIS_ACL_TYPE = 'application/cmisacl+xml'

# Standard rels
DOWN_REL = 'down'
FIRST_REL = 'first'
LAST_REL = 'last'
NEXT_REL = 'next'
PREV_REL = 'prev'
SELF_REL = 'self'
UP_REL = 'up'
TYPE_DESCENDANTS_REL = 'http://docs.oasis-open.org/ns/cmis/link/200908/typedescendants'
VERSION_HISTORY_REL = 'version-history'
FOLDER_TREE_REL = 'http://docs.oasis-open.org/ns/cmis/link/200908/foldertree'
RELATIONSHIPS_REL = 'http://docs.oasis-open.org/ns/cmis/link/200908/relationships'
ACL_REL = 'http://docs.oasis-open.org/ns/cmis/link/200908/acl'
CHANGE_LOG_REL = 'http://docs.oasis-open.org/ns/cmis/link/200908/changes'

# Collection types
QUERY_COLL = 'query'
TYPES_COLL = 'types'
CHECKED_OUT_COLL = 'checkedout'
UNFILED_COLL = 'unfiled'
ROOT_COLL = 'root'

# This seems to be the common pattern across known CMIS servers
# It is essentially ISO 8601 without the microseconds or time zone offset
timeStampPattern = re.compile('^(\d{4}\-\d{2}\-\d{2}T\d{2}:\d{2}:\d{2})?')


class CmisClient(object):

    """
    Handles all communication with the CMIS provider.
    """

    def __init__(self, repositoryUrl, username, password):

        """
        This is the entry point to the API. You need to know the
        :param repositoryUrl: The service URL of the CMIS provider
        :param username: Username
        :param password: Password

        >>> client = CmisClient('http://localhost:8080/alfresco/s/cmis', 'admin', 'admin')
        """

        self.repositoryUrl = repositoryUrl
        self.username = username
        self.password = password

    def __str__(self):
        """To string"""
        return 'CMIS client connection to %s' % self.repositoryUrl

    def getRepositories(self):

        """
        Returns a dict of high-level info about the repositories available at
        this service. The dict contains entries for 'repositoryId' and
        'repositoryName'.

        See CMIS specification document 2.2.2.1 getRepositories

        >>> client.getRepositories()
        [{'repositoryName': u'Main Repository', 'repositoryId': u'83beb297-a6fa-4ac5-844b-98c871c0eea9'}]
        """

        result = self.get(self.repositoryUrl)
        if (type(result) == HTTPError):
            raise RuntimeException()

        workspaceElements = result.getElementsByTagNameNS(APP_NS, 'workspace')
        # instantiate a Repository object using every workspace element
        # in the service URL then ask the repository object for its ID
        # and name, and return that back

        repositories = []
        for node in [e for e in workspaceElements if e.nodeType == e.ELEMENT_NODE]:
            repository = Repository(self, node)
            repositories.append({'repositoryId': repository.getRepositoryId(),
                                 'repositoryName': repository.getRepositoryInfo()['repositoryName']})
        return repositories

    def getRepository(self, repositoryId):

        """
        Returns the repository identified by the specified repositoryId.

        >>> repo = client.getRepository('83beb297-a6fa-4ac5-844b-98c871c0eea9')
        >>> repo.getRepositoryName()
        u'Main Repository'
        """

        doc = self.get(self.repositoryUrl)
        workspaceElements = doc.getElementsByTagNameNS(APP_NS, 'workspace')

        for workspaceElement in workspaceElements:
            idElement = workspaceElement.getElementsByTagNameNS(CMIS_NS, 'repositoryId')
            if idElement[0].childNodes[0].data == repositoryId:
                return Repository(self, workspaceElement)

        raise ObjectNotFoundException

    def getDefaultRepository(self):

        """
        There does not appear to be anything in the spec that identifies
        a repository as being the default, so we'll define it to be the
        first one in the list.

        >>> repo = client.getDefaultRepository()
        >>> repo.getRepositoryId()
        u'83beb297-a6fa-4ac5-844b-98c871c0eea9'
        """

        doc = self.get(self.repositoryUrl)
        workspaceElements = doc.getElementsByTagNameNS(APP_NS, 'workspace')
        # instantiate a Repository object with the first workspace
        # element we find
        repository = Repository(self, [e for e in workspaceElements if e.nodeType == e.ELEMENT_NODE][0])
        return repository

    def get(self, url, **kwargs):

        """
        Does a get against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects to it for you.

        For example, if you need to get a specific object by object id, try
        :class:`Repository.getObject`. If you have a path instead of an object
        id, use :class:`Repository.getObjectByPath`. Or, you could start with
        the root folder (:class:`Repository.getRootFolder`) and drill down from
        there.
        """

        result = Rest().get(url,
                            username=self.username,
                            password=self.password,
                            **kwargs)
        if type(result) == HTTPError:
            self._processCommonErrors(result)
            return result
        else:
            return minidom.parse(result)

    def delete(self, url, **kwargs):

        """
        Does a delete against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects to it for you.

        For example, to delete a folder you'd call :class:`Folder.delete` and
        to delete a document you'd call :class:`Document.delete`.
        """

        result = Rest().delete(url,
                               username=self.username,
                               password=self.password,
                               **kwargs)
        if type(result) == HTTPError:
            self._processCommonErrors(result)
            return result
        else:
            pass

    def post(self, url, payload, contentType, **kwargs):

        """
        Does a post against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects to it for you.

        For example, to update the properties on an object, you'd call
        :class:`CmisObject.updateProperties`. Or, to check in a document that's
        been checked out, you'd call :class:`Document.checkin` on the PWC.
        """

        result = Rest().post(url,
                             payload,
                             contentType,
                             username=self.username,
                             password=self.password,
                             **kwargs)
        if type(result) != HTTPError:
            return minidom.parse(result)
        elif result.code == 201:
            return minidom.parse(result)
        else:
            self._processCommonErrors(result)
            return result

    def put(self, url, payload, contentType, **kwargs):

        """
        Does a put against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects to it for you.

        For example, to update the properties on an object, you'd call
        :class:`CmisObject.updateProperties`. Or, to check in a document that's
        been checked out, you'd call :class:`Document.checkin` on the PWC.
        """

        result = Rest().put(url,
                            payload,
                            contentType,
                            username=self.username,
                            password=self.password,
                            **kwargs)
        if type(result) == HTTPError:
            self._processCommonErrors(result)
            return result
        else:
            #if result.headers['content-length'] != '0':
            try:
                return minidom.parse(result)
            except ExpatError:
                return None

    def _processCommonErrors(self, error):

        """
        Maps HTTPErrors that are common to all to exceptions. Only errors
        that are truly global, like 401 not authorized, should be handled
        here. Callers should handle the rest.

        See CMIS specification document 3.2.4.1 Common CMIS Exceptions
        """

        if error.status == 401:
            raise PermissionDeniedException(error.status)
        elif error.status == 400:
            raise InvalidArgumentException(error.status)
        elif error.status == 404:
            raise ObjectNotFoundException(error.status)
        elif error.status == 403:
            raise PermissionDeniedException(error.status)
        elif error.status == 405:
            raise NotSupportedException(error.status)
        elif error.status == 409:
            raise UpdateConflictException(error.status)
        elif error.status == 500:
            raise RuntimeException(error.status)

    defaultRepository = property(getDefaultRepository)
    repositories = property(getRepositories)


class Repository(object):

    """
    Represents a CMIS repository. Will lazily populate itself by
    calling the repository CMIS service URL.

    You must pass in an instance of a CmisClient when creating an
    instance of this class.

    See CMIS specification document 2.1.1 Repository
    """

    def __init__(self, cmisClient, xmlDoc=None):
        """ Constructor """
        self._cmisClient = cmisClient
        self.xmlDoc = xmlDoc
        self._repositoryId = None
        self._repositoryName = None
        self._repositoryInfo = {}
        self._capabilities = {}
        self._uriTemplates = {}
        self._permDefs = {}
        self._permMap = {}
        self._permissions = None
        self._propagation = None

    def __str__(self):
        """To string"""
        return self.getRepositoryName()

    def reload(self):
        """
        This method will re-fetch the repository's XML data from the CMIS
        repository.
        """
        self.xmlDoc = self._cmisClient.get(self._cmisClient.repositoryUrl)
        self._initData()

    def _initData(self):
        """
        This method clears out any local variables that would be out of sync
        when data is re-fetched from the server.
        """
        self._repositoryId = None
        self._repositoryName = None
        self._repositoryInfo = {}
        self._capabilities = {}
        self._uriTemplates = {}
        self._permDefs = {}
        self._permMap = {}
        self._permissions = None
        self._propagation = None

    def getSupportedPermissions(self):

        """
        Returns the value of the cmis:supportedPermissions element. Valid
        values are:
          basic: indicates that the CMIS Basic permissions are supported
          repository: indicates that repository specific permissions are supported
          both: indicates that both CMIS basic permissions and repository specific permissions are supported

        >>> repo.supportedPermissions
        u'both'
        """

        if not self.getCapabilities()['ACL']:
            raise NotSupportedException(messages.NO_ACL_SUPPORT)

        if not self._permissions:
            if self.xmlDoc == None:
                self.reload()
            suppEls = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'supportedPermissions')
            assert len(suppEls) == 1, 'Expected the repository service document to have one element named supportedPermissions'
            self._permissions = suppEls[0].childNodes[0].data

        return self._permissions

    def getPermissionDefinitions(self):

        """
        Returns a dictionary of permission definitions for this repository. The
        key is the permission string or technical name of the permission
        and the value is the permission description.

        >>> for permDef in repo.permissionDefinitions:
        ...     print permDef
        ...
        cmis:all
        {http://www.alfresco.org/model/system/1.0}base.LinkChildren
        {http://www.alfresco.org/model/content/1.0}folder.Consumer
        {http://www.alfresco.org/model/security/1.0}All.All
        {http://www.alfresco.org/model/system/1.0}base.CreateAssociations
        {http://www.alfresco.org/model/system/1.0}base.FullControl
        {http://www.alfresco.org/model/system/1.0}base.AddChildren
        {http://www.alfresco.org/model/system/1.0}base.ReadAssociations
        {http://www.alfresco.org/model/content/1.0}folder.Editor
        {http://www.alfresco.org/model/content/1.0}cmobject.Editor
        {http://www.alfresco.org/model/system/1.0}base.DeleteAssociations
        cmis:read
        cmis:write
        """

        if not self.getCapabilities()['ACL']:
            raise NotSupportedException(messages.NO_ACL_SUPPORT)

        if self._permDefs == {}:
            if self.xmlDoc == None:
                self.reload()
            aclEls = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'aclCapability')
            assert len(aclEls) == 1, 'Expected the repository service document to have one element named aclCapability'
            aclEl = aclEls[0]
            perms = {}
            for e in aclEl.childNodes:
                if e.localName == 'permissions':
                    permEls = e.getElementsByTagNameNS(CMIS_NS, 'permission')
                    assert len(permEls) == 1, 'Expected permissions element to have a child named permission'
                    descEls = e.getElementsByTagNameNS(CMIS_NS, 'description')
                    assert len(descEls) == 1, 'Expected permissions element to have a child named description'
                    perm = permEls[0].childNodes[0].data
                    desc = descEls[0].childNodes[0].data
                    perms[perm] = desc
            self._permDefs = perms

        return self._permDefs

    def getPermissionMap(self):

        """
        Returns a dictionary representing the permission mapping table where
        each key is a permission key string and each value is a list of one or
        more permissions the principal must have to perform the operation.

        >>> for (k,v) in repo.permissionMap.items():
        ...     print 'To do this: %s, you must have these perms:' % k
        ...     for perm in v:
        ...             print perm
        ...
        To do this: canCreateFolder.Folder, you must have these perms:
        cmis:all
        {http://www.alfresco.org/model/system/1.0}base.CreateChildren
        To do this: canAddToFolder.Folder, you must have these perms:
        cmis:all
        {http://www.alfresco.org/model/system/1.0}base.CreateChildren
        To do this: canDelete.Object, you must have these perms:
        cmis:all
        {http://www.alfresco.org/model/system/1.0}base.DeleteNode
        To do this: canCheckin.Document, you must have these perms:
        cmis:all
        {http://www.alfresco.org/model/content/1.0}lockable.CheckIn
        """

        if not self.getCapabilities()['ACL']:
            raise NotSupportedException(messages.NO_ACL_SUPPORT)

        if self._permMap == {}:
            if self.xmlDoc == None:
                self.reload()
            aclEls = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'aclCapability')
            assert len(aclEls) == 1, 'Expected the repository service document to have one element named aclCapability'
            aclEl = aclEls[0]
            permMap = {}
            for e in aclEl.childNodes:
                permList = []
                if e.localName == 'mapping':
                    keyEls = e.getElementsByTagNameNS(CMIS_NS, 'key')
                    assert len(keyEls) == 1, 'Expected mapping element to have a child named key'
                    permEls = e.getElementsByTagNameNS(CMIS_NS, 'permission')
                    assert len(permEls) >= 1, 'Expected mapping element to have at least one permission element'
                    key = keyEls[0].childNodes[0].data
                    for permEl in permEls:
                        permList.append(permEl.childNodes[0].data)
                    permMap[key] = permList
            self._permMap = permMap

        return self._permMap

    def getPropagation(self):

        """
        Returns the value of the cmis:propagation element. Valid values are:
          objectonly: indicates that the repository is able to apply ACEs
            without changing the ACLs of other objects
          propagate: indicates that the repository is able to apply ACEs to a
            given object and propagate this change to all inheriting objects

        >>> repo.propagation
        u'propagate'
        """

        if not self.getCapabilities()['ACL']:
            raise NotSupportedException(messages.NO_ACL_SUPPORT)

        if not self._propagation:
            if self.xmlDoc == None:
                self.reload()
            propEls = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'propagation')
            assert len(propEls) == 1, 'Expected the repository service document to have one element named propagation'
            self._propagation = propEls[0].childNodes[0].data

        return self._propagation

    def getRepositoryId(self):

        """
        Returns this repository's unique identifier

        >>> repo = client.getDefaultRepository()
        >>> repo.getRepositoryId()
        u'83beb297-a6fa-4ac5-844b-98c871c0eea9'
        """

        if self._repositoryId == None:
            if self.xmlDoc == None:
                self.reload()
            self._repositoryId = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'repositoryId')[0].firstChild.data
        return self._repositoryId

    def getRepositoryName(self):

        """
        Returns this repository's name

        >>> repo = client.getDefaultRepository()
        >>> repo.getRepositoryName()
        u'Main Repository'
        """

        if self._repositoryName == None:
            if self.xmlDoc == None:
                self.reload()
            self._repositoryName = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'repositoryName')[0].firstChild.data
        return self._repositoryName

    def getRepositoryInfo(self):

        """
        Returns a dict of repository information.

        See CMIS specification document 2.2.2.2 getRepositoryInfo

        >>> repo = client.getDefaultRepository()>>> repo.getRepositoryName()
        u'Main Repository'
        >>> info = repo.getRepositoryInfo()
        >>> for k,v in info.items():
        ...     print "%s:%s" % (k,v)
        ...
        cmisSpecificationTitle:Version 1.0 Committee Draft 04
        cmisVersionSupported:1.0
        repositoryDescription:None
        productVersion:3.2.0 (r2 2440)
        rootFolderId:workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348
        repositoryId:83beb297-a6fa-4ac5-844b-98c871c0eea9
        repositoryName:Main Repository
        vendorName:Alfresco
        productName:Alfresco Repository (Community)
        """

        if not self._repositoryInfo:
            if self.xmlDoc == None:
                self.reload()
            repoInfoElement = self.xmlDoc.getElementsByTagNameNS(CMISRA_NS, 'repositoryInfo')[0]
            for node in repoInfoElement.childNodes:
                if node.nodeType == node.ELEMENT_NODE and node.localName != 'capabilities':
                    try:
                        data = node.childNodes[0].data
                    except:
                        data = None
                    self._repositoryInfo[node.localName] = data
        return self._repositoryInfo

    def getCapabilities(self):

        """
        Returns a dict of repository capabilities.

        >>> caps = repo.getCapabilities()
        >>> for k,v in caps.items():
        ...     print "%s:%s" % (k,v)
        ...
        PWCUpdatable:True
        VersionSpecificFiling:False
        Join:None
        ContentStreamUpdatability:anytime
        AllVersionsSearchable:False
        Renditions:None
        Multifiling:True
        GetFolderTree:True
        GetDescendants:True
        ACL:None
        PWCSearchable:True
        Query:bothcombined
        Unfiling:False
        Changes:None
        """

        if not self._capabilities:
            if self.xmlDoc == None:
                self.reload()
            capabilitiesElement = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'capabilities')[0]
            for node in [e for e in capabilitiesElement.childNodes if e.nodeType == e.ELEMENT_NODE]:
                key = node.localName.replace('capability', '')
                value = parseBoolValue(node.childNodes[0].data)
                self._capabilities[key] = value
        return self._capabilities

    def getRootFolder(self):
        """
        Returns the root folder of the repository

        >>> root = repo.getRootFolder()
        >>> root.getObjectId()
        u'workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348'
        """
        # get the root folder id
        rootFolderId = self.getRepositoryInfo()['rootFolderId']
        # instantiate a Folder object using the ID
        folder = Folder(self._cmisClient, self, rootFolderId)
        # return it
        return folder

    def getFolder(self, folderId):

        """
        Returns a :class:`Folder` object for a specified folderId

        >>> someFolder = repo.getFolder('workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348')
        >>> someFolder.getObjectId()
        u'workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348'
        """

        retObject = self.getObject(folderId)
        return Folder(self._cmisClient, self, xmlDoc=retObject.xmlDoc)

    def getTypeChildren(self,
                        typeId=None):

        """
        Returns a list of :class:`ObjectType` objects corresponding to the
        child types of the type specified by the typeId.

        If no typeId is provided, the result will be the same as calling
        `self.getTypeDefinitions`

        See CMIS specification document 2.2.2.3 getTypeChildren

        These optional arguments are current unsupported:
         - includePropertyDefinitions
         - maxItems
         - skipCount

        >>> baseTypes = repo.getTypeChildren()
        >>> for baseType in baseTypes:
        ...     print baseType.getTypeId()
        ...
        cmis:folder
        cmis:relationship
        cmis:document
        cmis:policy
        """

        # Unfortunately, the spec does not appear to present a way to
        # know how to get the children of a specific type without first
        # retrieving the type, then asking it for one of its navigational
        # links.

        # if a typeId is specified, get it, then get its "down" link
        if typeId:
            targetType = self.getTypeDefinition(typeId)
            childrenUrl = targetType.getLink(DOWN_REL, ATOM_XML_FEED_TYPE_P)
            typesXmlDoc = self._cmisClient.get(childrenUrl)
            entryElements = typesXmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
            types = []
            for entryElement in entryElements:
                objectType = ObjectType(self._cmisClient,
                                        self,
                                        xmlDoc=entryElement)
                types.append(objectType)
        # otherwise, if a typeId is not specified, return
        # the list of base types
        else:
            types = self.getTypeDefinitions()
        return types

    def getTypeDescendants(self, typeId=None, **kwargs):

        """
        Returns a list of :class:`ObjectType` objects corresponding to the
        descendant types of the type specified by the typeId.

        If no typeId is provided, the repository's "typesdescendants" URL
        will be called to determine the list of descendant types.

        See CMIS specification document 2.2.2.4 getTypeDescendants

        >>> allTypes = repo.getTypeDescendants()
        >>> for aType in allTypes:
        ...     print aType.getTypeId()
        ...
        cmis:folder
        F:cm:systemfolder
        F:act:savedactionfolder
        F:app:configurations
        F:fm:forums
        F:wcm:avmfolder
        F:wcm:avmplainfolder
        F:wca:webfolder
        F:wcm:avmlayeredfolder
        F:st:site
        F:app:glossary
        F:fm:topic

        These optional arguments are supported:
         - depth
         - includePropertyDefinitions

        >>> types = alfRepo.getTypeDescendants('cmis:folder')
        >>> len(types)
        17
        >>> types = alfRepo.getTypeDescendants('cmis:folder', depth=1)
        >>> len(types)
        12
        >>> types = alfRepo.getTypeDescendants('cmis:folder', depth=2)
        >>> len(types)
        17
        """

        # Unfortunately, the spec does not appear to present a way to
        # know how to get the children of a specific type without first
        # retrieving the type, then asking it for one of its navigational
        # links.
        if typeId:
            targetType = self.getTypeDefinition(typeId)
            descendUrl = targetType.getLink(DOWN_REL, CMIS_TREE_TYPE_P)

        else:
            descendUrl = self.getLink(TYPE_DESCENDANTS_REL)

        if not descendUrl:
            raise NotSupportedException("Could not determine the type descendants URL")

        typesXmlDoc = self._cmisClient.get(descendUrl, **kwargs)
        entryElements = typesXmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
        types = []
        for entryElement in entryElements:
            objectType = ObjectType(self._cmisClient,
                                    self,
                                    xmlDoc=entryElement)
            types.append(objectType)
        return types

    def getTypeDefinitions(self, **kwargs):

        """
        Returns a list of :class:`ObjectType` objects representing
        the base types in the repository.

        >>> baseTypes = repo.getTypeDefinitions()
        >>> for baseType in baseTypes:
        ...     print baseType.getTypeId()
        ...
        cmis:folder
        cmis:relationship
        cmis:document
        cmis:policy
        """

        typesUrl = self.getCollectionLink(TYPES_COLL)
        typesXmlDoc = self._cmisClient.get(typesUrl, **kwargs)
        entryElements = typesXmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
        types = []
        for entryElement in entryElements:
            objectType = ObjectType(self._cmisClient,
                                    self,
                                    xmlDoc=entryElement)
            types.append(objectType)
        # return the result
        return types

    def getTypeDefinition(self, typeId):

        """
        Returns an :class:`ObjectType` object for the specified object type id.

        See CMIS specification document 2.2.2.5 getTypeDefinition

        >>> folderType = repo.getTypeDefinition('cmis:folder')
        """

        objectType = ObjectType(self._cmisClient, self, typeId)
        objectType.reload()
        return objectType

    def getLink(self, rel):
        """
        Returns the HREF attribute of an Atom link element for the
        specified rel.
        """
        if self.xmlDoc == None:
            self.reload()

        linkElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        for linkElement in linkElements:

            if linkElement.attributes.has_key('rel'):
                relAttr = linkElement.attributes['rel'].value

                if relAttr == rel:
                    return linkElement.attributes['href'].value

    def getCheckedOutDocs(self, **kwargs):

        """
        Returns a ResultSet of :class:`CmisObject` objects that
        are currently checked out.

        See CMIS specification document 2.2.3.6 getCheckedOutDocs

        >>> rs = repo.getCheckedOutDocs()
        >>> len(rs.getResults())
        2
        >>> for doc in repo.getCheckedOutDocs().getResults():
        ...     doc.getTitle()
        ...
        u'sample-a (Working Copy).pdf'
        u'sample-b (Working Copy).pdf'

        These optional arguments are supported:
         - folderId
         - maxItems
         - skipCount
         - orderBy
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
        """

        return self.getCollection(CHECKED_OUT_COLL, **kwargs)

    def getUnfiledDocs(self, **kwargs):

        """
        Returns a ResultSet of :class:`CmisObject` objects that
        are currently unfiled.

        >>> rs = repo.getUnfiledDocs()
        >>> len(rs.getResults())
        2
        >>> for doc in repo.getUnfiledDocs().getResults():
        ...     doc.getTitle()
        ...
        u'sample-a.pdf'
        u'sample-b.pdf'

        These optional arguments are supported:
         - folderId
         - maxItems
         - skipCount
         - orderBy
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
        """

        return self.getCollection(UNFILED_COLL, **kwargs)

    def getObject(self,
                  objectId,
                  **kwargs):

        """
        Returns an object given the specified object ID.

        See CMIS specification document 2.2.4.7 getObject

        >>> doc = repo.getObject('workspace://SpacesStore/f0c8b90f-bec0-4405-8b9c-2ab570589808')
        >>> doc.getTitle()
        u'sample-b.pdf'

        The following optional arguments are supported:
         - returnVersion
         - filter
         - includeRelationships
         - includePolicyIds
         - renditionFilter
         - includeACL
         - includeAllowableActions
        """

        return getSpecializedObject(CmisObject(self._cmisClient, self, objectId, **kwargs), **kwargs)

    def getObjectByPath(self, path, **kwargs):

        """
        Returns an object given the path to the object.

        See CMIS specification document 2.2.4.9 getObjectByPath

        >>> doc = repo.getObjectByPath('/jeff test/sample-b.pdf')
        >>> doc.getTitle()
        u'sample-b.pdf'

        The following optional arguments are not currently supported:
         - filter
         - includeAllowableActions
        """

        # get the uritemplate
        template = self.getUriTemplates()['objectbypath']['template']

        # fill in the template with the path provided
        params = {
              '{path}': quote_plus(path, '/'),
              '{filter}': '',
              '{includeAllowableActions}': 'false',
              '{includePolicyIds}': 'false',
              '{includeRelationships}': 'false',
              '{includeACL}': 'false',
              '{renditionFilter}': ''}

        options = {}
        addOptions = {} # args specified, but not in the template
        for k, v in kwargs.items():
            pKey = "{" + k + "}"
            if template.find(pKey) >= 0:
                options[pKey] = toCMISValue(v)
            else:
                addOptions[k] = toCMISValue(v)

        # merge the templated args with the default params
        params.update(options)

        byObjectPathUrl = multiple_replace(params, template)

        # do a GET against the URL
        result = self._cmisClient.get(byObjectPathUrl, **addOptions)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # instantiate CmisObject objects with the results and return the list
        entryElements = result.getElementsByTagNameNS(ATOM_NS, 'entry')
        assert(len(entryElements) == 1), "Expected entry element in result from calling %s" % byObjectPathUrl
        return getSpecializedObject(CmisObject(self._cmisClient, self, xmlDoc=entryElements[0], **kwargs), **kwargs)

    def query(self, statement, **kwargs):

        """
        Returns a list of :class:`CmisObject` objects based on the CMIS
        Query Language passed in as the statement. The actual objects
        returned will be instances of the appropriate child class based
        on the object's base type ID.

        In order for the results to be properly instantiated as objects,
        make sure you include 'cmis:objectId' as one of the fields in
        your select statement, or just use "SELECT \*".

        If you want the search results to automatically be instantiated with
        the appropriate sub-class of :class:`CmisObject` you must either
        include cmis:baseTypeId as one of the fields in your select statement
        or just use "SELECT \*".

        See CMIS specification document 2.2.6.1 query

        >>> q = "select * from cmis:document where cmis:name like '%test%'"
        >>> resultSet = repo.query(q)
        >>> len(resultSet.getResults())
        1
        >>> resultSet.hasNext()
        False

        The following optional arguments are supported:
         - searchAllVersions
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
         - maxItems
         - skipCount

        >>> q = 'select * from cmis:document'
        >>> rs = repo.query(q)
        >>> len(rs.getResults())
        148
        >>> rs = repo.query(q, maxItems='5')
        >>> len(rs.getResults())
        5
        >>> rs.hasNext()
        True
        """

        if self.xmlDoc == None:
            self.reload()

        # get the URL this repository uses to accept query POSTs
        queryUrl = self.getCollectionLink(QUERY_COLL)

        # build the CMIS query XML that we're going to POST
        xmlDoc = self._getQueryXmlDoc(statement, **kwargs)

        # do the POST
        #print 'posting:%s' % xmlDoc.toxml()
        result = self._cmisClient.post(queryUrl,
                                       xmlDoc.toxml(),
                                       CMIS_QUERY_TYPE)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self, result)

    def getContentChanges(self, **kwargs):

        """
        Returns a :class:`ResultSet` containing :class:`ChangeEntry` objects.

        >>> for changeEntry in rs:
        ...     changeEntry.objectId
        ...     changeEntry.id
        ...     changeEntry.changeType
        ...     changeEntry.changeTime
        ...
        'workspace://SpacesStore/0e2dc775-16b7-4634-9e54-2417a196829b'
        u'urn:uuid:0e2dc775-16b7-4634-9e54-2417a196829b'
        u'created'
        datetime.datetime(2010, 2, 11, 12, 55, 14)
        'workspace://SpacesStore/bd768f9f-99a7-4033-828d-5b13f96c6923'
        u'urn:uuid:bd768f9f-99a7-4033-828d-5b13f96c6923'
        u'updated'
        datetime.datetime(2010, 2, 11, 12, 55, 13)
        'workspace://SpacesStore/572c2cac-6b26-4cd8-91ad-b2931fe5b3fb'
        u'urn:uuid:572c2cac-6b26-4cd8-91ad-b2931fe5b3fb'
        u'updated'

        See CMIS specification document 2.2.6.2 getContentChanges

        The following optional arguments are supported:
         - changeLogToken
         - includeProperties
         - includePolicyIDs
         - includeACL
         - maxItems

        You can get the latest change log token by inspecting the repository
        info via :meth:`Repository.getRepositoryInfo`.

        >>> repo.info['latestChangeLogToken']
        u'2692'
        >>> rs = repo.getContentChanges(changeLogToken='2692')
        >>> len(rs)
        1
        >>> rs[0].id
        u'urn:uuid:8e88f694-93ef-44c5-9f70-f12fff824be9'
        >>> rs[0].changeType
        u'updated'
        >>> rs[0].changeTime
        datetime.datetime(2010, 2, 16, 20, 6, 37)
        """

        if self.getCapabilities()['Changes'] == None:
            raise NotSupportedException(messages.NO_CHANGE_LOG_SUPPORT)

        changesUrl = self.getLink(CHANGE_LOG_REL)
        result = self._cmisClient.get(changesUrl, **kwargs)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ChangeEntryResultSet(self._cmisClient, self, result)

    def createDocument(self,
                       name,
                       properties={},
                       parentFolder=None,
                       contentFile=None,
                       contentType=None,
                       contentEncoding=None):

        """
        Creates a new :class:`Document` object. If the repository
        supports unfiled objects, you do not have to pass in
        a parent :class:`Folder` otherwise it is required.

        To create a document with an associated contentFile, pass in a
        File object. The method will attempt to guess the appropriate content
        type and encoding based on the file. To specify it yourself, pass them
        in via the contentType and contentEncoding arguments.

        See CMIS specification document 2.2.4.1 createDocument

        >>> f = open('sample-a.pdf', 'rb')
        >>> doc = folder.createDocument('sample-a.pdf', contentFile=f)
        <cmislib.model.Document object at 0x105be5e10>
        >>> f.close()
        >>> doc.getTitle()
        u'sample-a.pdf'

        The following optional arguments are not currently supported:
         - versioningState
         - policies
         - addACEs
         - removeACEs
        """

        # if you didn't pass in a parent folder
        if parentFolder == None:
            # if the repository doesn't require fileable objects to be filed
            if self.getCapabilities()['Unfiling']:
                # has not been implemented
                raise NotImplementedError
            else:
                # this repo requires fileable objects to be filed
                raise InvalidArgumentException

        return parentFolder.createDocument(name, properties, contentFile,
            contentType, contentEncoding)

    def createDocumentFromSource(self,
                                 sourceId,
                                 properties={},
                                 parentFolder=None):
        """
        This is not yet implemented.

        See CMIS specification document 2.2.4.2 createDocumentFromSource

        The following optional arguments are not yet supported:
         - versioningState
         - policies
         - addACEs
         - removeACEs
        """
        # TODO: To be implemented
        raise NotImplementedError

    def createFolder(self,
                     parentFolder,
                     name,
                     properties={}):

        """
        Creates a new :class:`Folder` object in the specified parentFolder.

        See CMIS specification document 2.2.4.3 createFolder

        >>> root = repo.getRootFolder()
        >>> folder = repo.createFolder(root, 'someFolder2')
        >>> folder.getTitle()
        u'someFolder2'
        >>> folder.getObjectId()
        u'workspace://SpacesStore/2224a63c-350b-438c-be72-8f425e79ce1f'

        The following optional arguments are not yet supported:
         - policies
         - addACEs
         - removeACEs
        """

        return parentFolder.createFolder(name, properties)

    def createRelationship(self, sourceObj, targetObj, relType):
        """
        Creates a relationship of the specific type between a source object
        and a target object and returns the new :class:`Relationship` object.

        See CMIS specification document 2.2.4.4 createRelationship

        The following optional arguments are not currently supported:
         - policies
         - addACEs
         - removeACEs
        """
        return sourceObj.createRelationship(targetObj, relType)

    def createPolicy(self, properties):
        """
        This has not yet been implemented.

        See CMIS specification document 2.2.4.5 createPolicy

        The following optional arguments are not currently supported:
         - folderId
         - policies
         - addACEs
         - removeACEs
        """
        # TODO: To be implemented
        raise NotImplementedError

    def getUriTemplates(self):

        """
        Returns a list of the URI templates the repository service knows about.

        >>> templates = repo.getUriTemplates()
        >>> templates['typebyid']['mediaType']
        u'application/atom+xml;type=entry'
        >>> templates['typebyid']['template']
        u'http://localhost:8080/alfresco/s/cmis/type/{id}'
        """

        if self._uriTemplates == {}:

            if self.xmlDoc == None:
                self.reload()

            uriTemplateElements = self.xmlDoc.getElementsByTagNameNS(CMISRA_NS, 'uritemplate')

            for uriTemplateElement in uriTemplateElements:
                template = None
                templType = None
                mediatype = None

                for node in [e for e in uriTemplateElement.childNodes if e.nodeType == e.ELEMENT_NODE]:
                    if node.localName == 'template':
                        template = node.childNodes[0].data
                    elif node.localName == 'type':
                        templType = node.childNodes[0].data
                    elif node.localName == 'mediatype':
                        mediatype = node.childNodes[0].data

                self._uriTemplates[templType] = UriTemplate(template,
                                                       templType,
                                                       mediatype)

        return self._uriTemplates

    def getCollection(self, collectionType, **kwargs):

        """
        Returns a list of objects returned for the specified collection.

        If the query collection is requested, an exception will be raised.
        That collection isn't meant to be retrieved.

        If the types collection is specified, the method returns the result of
        `getTypeDefinitions` and ignores any optional params passed in.

        >>> from cmislib.model import TYPES_COLL
        >>> types = repo.getCollection(TYPES_COLL)
        >>> len(types)
        4
        >>> types[0].getTypeId()
        u'cmis:folder'

        Otherwise, the collection URL is invoked, and a :class:`ResultSet` is
        returned.

        >>> from cmislib.model import CHECKED_OUT_COLL
        >>> resultSet = repo.getCollection(CHECKED_OUT_COLL)
        >>> len(resultSet.getResults())
        1
        """

        if collectionType == QUERY_COLL:
            raise NotSupportedException
        elif collectionType == TYPES_COLL:
            return self.getTypeDefinitions()

        result = self._cmisClient.get(self.getCollectionLink(collectionType), **kwargs)
        if (type(result) == HTTPError):
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self, result)

    def getCollectionLink(self, collectionType):

        """
        Returns the link HREF from the specified collectionType
        ('checkedout', for example).

        >>> from cmislib.model import CHECKED_OUT_COLL
        >>> repo.getCollectionLink(CHECKED_OUT_COLL)
        u'http://localhost:8080/alfresco/s/cmis/checkedout'

        """

        collectionElements = self.xmlDoc.getElementsByTagNameNS(APP_NS, 'collection')
        for collectionElement in collectionElements:
            link = collectionElement.attributes['href'].value
            for node in [e for e in collectionElement.childNodes if e.nodeType == e.ELEMENT_NODE]:
                if node.localName == 'collectionType':
                    if node.childNodes[0].data == collectionType:
                        return link

    def _getQueryXmlDoc(self, query, **kwargs):

        """
        Utility method that knows how to build CMIS query xml around the
        specified query statement.
        """

        cmisXmlDoc = minidom.Document()
        queryElement = cmisXmlDoc.createElementNS(CMIS_NS, "query")
        queryElement.setAttribute('xmlns', CMIS_NS)
        cmisXmlDoc.appendChild(queryElement)

        statementElement = cmisXmlDoc.createElementNS(CMIS_NS, "statement")
        cdataSection = cmisXmlDoc.createCDATASection(query)
        statementElement.appendChild(cdataSection)
        queryElement.appendChild(statementElement)

        for (k, v) in kwargs.items():
            optionElement = cmisXmlDoc.createElementNS(CMIS_NS, k)
            optionText = cmisXmlDoc.createTextNode(v)
            optionElement.appendChild(optionText)
            queryElement.appendChild(optionElement)

        return cmisXmlDoc

    capabilities = property(getCapabilities)
    id = property(getRepositoryId)
    info = property(getRepositoryInfo)
    name = property(getRepositoryName)
    rootFolder = property(getRootFolder)
    permissionDefinitions = property(getPermissionDefinitions)
    permissionMap = property(getPermissionMap)
    propagation = property(getPropagation)
    supportedPermissions = property(getSupportedPermissions)


class ResultSet():

    """
    Represents a paged result set. In CMIS, this is most often an Atom feed.
    """

    def __init__(self, cmisClient, repository, xmlDoc):
        ''' Constructor '''
        self._cmisClient = cmisClient
        self._repository = repository
        self._xmlDoc = xmlDoc
        self._results = []

    def __iter__(self):
        ''' Iterator for the result set '''
        return iter(self.getResults())

    def __getitem__(self, index):
        ''' Getter for the result set '''
        return self.getResults()[index]

    def __len__(self):
        ''' Len method for the result set '''
        return len(self.getResults())

    def _getLink(self, rel):
        '''
        Returns the link found in the feed's XML for the specified rel.
        '''
        linkElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        for linkElement in linkElements:

            if linkElement.attributes.has_key('rel'):
                relAttr = linkElement.attributes['rel'].value

                if relAttr == rel:
                    return linkElement.attributes['href'].value

    def _getPageResults(self, rel):
        '''
        Given a specified rel, does a get using that link (if one exists)
        and then converts the resulting XML into a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type.

        The results are kept around to facilitate repeated calls without moving
        the cursor.
        '''
        link = self._getLink(rel)
        if link:
            result = self._cmisClient.get(link)
            if (type(result) == HTTPError):
                raise CmisException(result.code)

            # return the result
            self._xmlDoc = result
            self._results = []
            return self.getResults()

    def reload(self):

        '''
        Re-invokes the self link for the current set of results.

        >>> resultSet = repo.getCollection(CHECKED_OUT_COLL)
        >>> resultSet.reload()

        '''

        self._getPageResults(SELF_REL)

    def getResults(self):

        '''
        Returns the results that were fetched and cached by the get*Page call.

        >>> resultSet = repo.getCheckedOutDocs()
        >>> resultSet.hasNext()
        False
        >>> for result in resultSet.getResults():
        ...     result
        ...
        <cmislib.model.Document object at 0x104851810>
        '''
        if self._results:
            return self._results

        if self._xmlDoc:
            entryElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
            entries = []
            for entryElement in entryElements:
                cmisObject = getSpecializedObject(CmisObject(self._cmisClient,
                                                             self._repository,
                                                             xmlDoc=entryElement))
                entries.append(cmisObject)

            self._results = entries

        return self._results

    def hasObject(self, objectId):
        for obj in self.getResults():
            if obj.id == objectId:
                return True
        return False

    def getFirst(self):

        '''
        Returns the first page of results as a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type. This only
        works when the server returns a "first" link. Not all of them do.

        >>> resultSet.hasFirst()
        True
        >>> results = resultSet.getFirst()
        >>> for result in results:
        ...     result
        ...
        <cmislib.model.Document object at 0x10480bc90>
        '''

        return self._getPageResults(FIRST_REL)

    def getPrev(self):

        '''
        Returns the prev page of results as a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type. This only
        works when the server returns a "prev" link. Not all of them do.
        >>> resultSet.hasPrev()
        True
        >>> results = resultSet.getPrev()
        >>> for result in results:
        ...     result
        ...
        <cmislib.model.Document object at 0x10480bc90>
        '''

        return self._getPageResults(PREV_REL)

    def getNext(self):

        '''
        Returns the next page of results as a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type.
        >>> resultSet.hasNext()
        True
        >>> results = resultSet.getNext()
        >>> for result in results:
        ...     result
        ...
        <cmislib.model.Document object at 0x10480bc90>
        '''

        return self._getPageResults(NEXT_REL)

    def getLast(self):

        '''
        Returns the last page of results as a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type. This only
        works when the server is returning a "last" link. Not all of them do.

        >>> resultSet.hasLast()
        True
        >>> results = resultSet.getLast()
        >>> for result in results:
        ...     result
        ...
        <cmislib.model.Document object at 0x10480bc90>
        '''

        return self._getPageResults(LAST_REL)

    def hasNext(self):

        '''
        Returns True if this page contains a next link.

        >>> resultSet.hasNext()
        True
        '''

        if self._getLink(NEXT_REL):
            return True
        else:
            return False

    def hasPrev(self):

        '''
        Returns True if this page contains a prev link. Not all CMIS providers
        implement prev links consistently.

        >>> resultSet.hasPrev()
        True
        '''

        if self._getLink(PREV_REL):
            return True
        else:
            return False

    def hasFirst(self):

        '''
        Returns True if this page contains a first link. Not all CMIS providers
        implement first links consistently.

        >>> resultSet.hasFirst()
        True
        '''

        if self._getLink(FIRST_REL):
            return True
        else:
            return False

    def hasLast(self):

        '''
        Returns True if this page contains a last link. Not all CMIS providers
        implement last links consistently.

        >>> resultSet.hasLast()
        True
        '''

        if self._getLink(LAST_REL):
            return True
        else:
            return False


class CmisObject(object):

    """
    Common ancestor class for other CMIS domain objects such as
    :class:`Document` and :class:`Folder`.
    """

    def __init__(self, cmisClient, repository, objectId=None, xmlDoc=None, **kwargs):
        """ Constructor """
        self._cmisClient = cmisClient
        self._repository = repository
        self._objectId = objectId
        self._name = None
        self._properties = {}
        self._allowableActions = {}
        self.xmlDoc = xmlDoc
        self._kwargs = kwargs

    def __str__(self):
        """To string"""
        return self.getObjectId()

    def reload(self, **kwargs):

        """
        Fetches the latest representation of this object from the CMIS service.
        Some methods, like :class:`^Document.checkout` do this for you.

        If you call reload with a properties filter, the filter will be in
        effect on subsequent calls until the filter argument is changed. To
        reset to the full list of properties, call reload with filter set to
        '*'.
        """

        if kwargs:
            if self._kwargs:
                self._kwargs.update(kwargs)
            else:
                self._kwargs = kwargs

        templates = self._repository.getUriTemplates()
        template = templates['objectbyid']['template']

        # Doing some refactoring here. Originally, we snagged the template
        # and then "filled in" the template based on the args passed in.
        # However, some servers don't provide a full template which meant
        # supported optional args wouldn't get passed in using the fill-the-
        # template approach. What's going on now is that the template gets
        # filled in where it can, but if additional, non-templated args are
        # passed in, those will get tacked on to the query string as
        # "additional" options.

        params = {
              '{id}': self.getObjectId(),
              '{filter}': '',
              '{includeAllowableActions}': 'false',
              '{includePolicyIds}': 'false',
              '{includeRelationships}': 'false',
              '{includeACL}': 'false',
              '{renditionFilter}': ''}

        options = {}
        addOptions = {} # args specified, but not in the template
        for k, v in self._kwargs.items():
            pKey = "{" + k + "}"
            if template.find(pKey) >= 0:
                options[pKey] = toCMISValue(v)
            else:
                addOptions[k] = toCMISValue(v)

        # merge the templated args with the default params
        params.update(options)

        # fill in the template
        byObjectIdUrl = multiple_replace(params, template)

        self.xmlDoc = self._cmisClient.get(byObjectIdUrl, **addOptions)
        self._initData()

        # if a returnVersion arg was passed in, it is possible we got back
        # a different object ID than the value we started with, so it needs
        # to be cleared out as well
        if options.has_key('returnVersion') or addOptions.has_key('returnVersion'):
            self._objectId = None

    def _initData(self):

        """
        An internal method used to clear out any member variables that
        might be out of sync if we were to fetch new XML from the
        service.
        """

        self._properties = {}
        self._name = None
        self._allowableActions = {}

    def getObjectId(self):

        """
        Returns the object ID for this object.

        >>> doc = resultSet.getResults()[0]
        >>> doc.getObjectId()
        u'workspace://SpacesStore/dc26102b-e312-471b-b2af-91bfb0225339'
        """

        if self._objectId == None:
            if self.xmlDoc == None:
                self.reload()
            props = self.getProperties()
            self._objectId = CmisId(props['cmis:objectId'])
        return self._objectId

    def getObjectParents(self):
        """
        This has not yet been implemented.

        See CMIS specification document 2.2.3.5 getObjectParents

        The following optional arguments are not supported:
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
         - includeRelativePathSegment
        """

        # TODO To be implemented
        raise NotImplementedError

    def getAllowableActions(self):

        """
        Returns a dictionary of allowable actions, keyed off of the action name.

        >>> actions = doc.getAllowableActions()
        >>> for a in actions:
        ...     print "%s:%s" % (a,actions[a])
        ...
        canDeleteContentStream:True
        canSetContentStream:True
        canCreateRelationship:True
        canCheckIn:False
        canApplyACL:False
        canDeleteObject:True
        canGetAllVersions:True
        canGetObjectParents:True
        canGetProperties:True

        See CMIS specification document 2.2.4.6 getAllowableActions
        """

        if self._allowableActions == {}:
            self.reload(includeAllowableActions=True)
            allowElements = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'allowableActions')
            assert len(allowElements) == 1, "Expected response to have exactly one allowableActions element"
            allowElement = allowElements[0]
            for node in [e for e in allowElement.childNodes if e.nodeType == e.ELEMENT_NODE]:
                actionName = node.localName
                actionValue = parseBoolValue(node.childNodes[0].data)
                self._allowableActions[actionName] = actionValue

        return self._allowableActions

    def getTitle(self):

        """
        Returns the value of the object's cmis:title property.
        """

        if self.xmlDoc == None:
            self.reload()

        titleElement = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'title')[0]

        if titleElement and titleElement.childNodes:
            return titleElement.childNodes[0].data

    def getProperties(self):

        """
        Returns a dict of the object's properties. If CMIS returns an
        empty element for a property, the property will be in the
        dict with a value of None.

        See CMIS specification document 2.2.4.8 getProperties

        >>> props = doc.getProperties()
        >>> for p in props:
        ...     print "%s: %s" % (p, props[p])
        ...
        cmis:contentStreamMimeType: text/html
        cmis:creationDate: 2009-12-15T09:45:35.369-06:00
        cmis:baseTypeId: cmis:document
        cmis:isLatestMajorVersion: false
        cmis:isImmutable: false
        cmis:isMajorVersion: false
        cmis:objectId: workspace://SpacesStore/dc26102b-e312-471b-b2af-91bfb0225339

        The optional filter argument is not yet implemented.
        """

        #TODO implement filter
        if self._properties == {}:
            if self.xmlDoc == None:
                self.reload()
            propertiesElement = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'properties')[0]
            #cpattern = re.compile(r'^property([\w]*)')
            for node in [e for e in propertiesElement.childNodes if e.nodeType == e.ELEMENT_NODE and e.namespaceURI == CMIS_NS]:
                #propertyId, propertyString, propertyDateTime
                #propertyType = cpattern.search(node.localName).groups()[0]
                propertyName = node.attributes['propertyDefinitionId'].value
                if node.childNodes and \
                   node.getElementsByTagNameNS(CMIS_NS, 'value')[0] and \
                   node.getElementsByTagNameNS(CMIS_NS, 'value')[0].childNodes:
                    propertyValue = parsePropValue(
                       node.getElementsByTagNameNS(CMIS_NS, 'value')[0].childNodes[0].data,
                       node.localName)
                else:
                    propertyValue = None
                self._properties[propertyName] = propertyValue
        return self._properties

    def getName(self):

        """
        Returns the value of cmis:name from the getProperties() dictionary.
        We don't need a getter for every standard CMIS property, but name
        is a pretty common one so it seems to make sense.

        >>> doc.getName()
        u'system-overview.html'
        """

        if self._name == None:
            self._name = self.getProperties()['cmis:name']
        return self._name

    def updateProperties(self, properties):

        """
        Updates the properties of an object with the properties provided.
        Only provide the set of properties that need to be updated.

        See CMIS specification document 2.2.4.12 updateProperties

        >>> folder = repo.getObjectByPath('/someFolder2')
        >>> folder.getName()
        u'someFolder2'
        >>> props = {'cmis:name': 'someFolderFoo'}
        >>> folder.updateProperties(props)
        <cmislib.model.Folder object at 0x103ab1210>
        >>> folder.getName()
        u'someFolderFoo'

        The optional changeToken is not yet supported.
        """

        # TODO need to support the changeToken

        # get the self link
        selfUrl = self._getSelfLink()

        # build the entry based on the properties provided
        xmlEntryDoc = self._getEntryXmlDoc(properties)

        # do a PUT of the entry
        updatedXmlDoc = self._cmisClient.put(selfUrl,
                                             xmlEntryDoc.toxml(),
                                             ATOM_XML_TYPE)

        # reset the xmlDoc for this object with what we got back from
        # the PUT, then call initData we dont' want to call
        # self.reload because we've already got the parsed XML--
        # there's no need to fetch it again
        self.xmlDoc = updatedXmlDoc
        self._initData()
        return self

    def move(self, targetFolderId, sourceFolderId):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.4.13 move
        """

        #TODO to be implemented
#        From looking at Alfresco, it seems as if you can post an atom entry
#        with an object ID to a new folder and pass the existing folder in the
#        sourceFolderId argument and that will trigger a move.
#
#        See the notes on Folder.addObject
        raise NotImplementedError

    def delete(self, **kwargs):

        """
        Deletes this :class:`CmisObject` from the repository. Note that in the
        case of a :class:`Folder` object, some repositories will refuse to
        delete it if it contains children and some will delete it without
        complaint. If what you really want to do is delete the folder and all
        of its descendants, use :meth:`~Folder.deleteTree` instead.

        See CMIS specification document 2.2.4.14 delete

        >>> folder.delete()

        The optional allVersions argument is supported.
        """

        url = self._getSelfLink()
        result = self._cmisClient.delete(url, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

    def applyPolicy(self, policyId):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.9.1 applyPolicy
        """

        # depends on this object's canApplyPolicy allowable action
        if self.getAllowableActions()['canApplyPolicy']:
            raise NotImplementedError
        else:
            raise CmisException('This object has canApplyPolicy set to false')

    def createRelationship(self, targetObj, relTypeId):

        """
        Creates a relationship between this object and a specified target
        object using the relationship type specified. Returns the new
        :class:`Relationship` object.

        >>> rel = tstDoc1.createRelationship(tstDoc2, 'R:cmiscustom:assoc')
        >>> rel.getProperties()
        {u'cmis:objectId': u'workspace://SpacesStore/271c48dd-6548-4771-a8f5-0de69b7cdc25', u'cmis:creationDate': None, u'cmis:objectTypeId': u'R:cmiscustom:assoc', u'cmis:lastModificationDate': None, u'cmis:targetId': u'workspace://SpacesStore/0ca1aa08-cb49-42e2-8881-53aa8496a1c1', u'cmis:lastModifiedBy': None, u'cmis:baseTypeId': u'cmis:relationship', u'cmis:sourceId': u'workspace://SpacesStore/271c48dd-6548-4771-a8f5-0de69b7cdc25', u'cmis:changeToken': None, u'cmis:createdBy': None}

        """

        if isinstance(relTypeId, str):
            relTypeId = CmisId(relTypeId)

        props = {}
        props['cmis:sourceId'] = self.getObjectId()
        props['cmis:targetId'] = targetObj.getObjectId()
        props['cmis:objectTypeId'] = relTypeId
        xmlDoc = self._getEntryXmlDoc(props)

        url = self._getLink(RELATIONSHIPS_REL)
        assert url != None, 'Could not determine relationships URL'

        result = self._cmisClient.post(url,
                                       xmlDoc.toxml(),
                                       ATOM_XML_TYPE)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # instantiate CmisObject objects with the results and return the list
        entryElements = result.getElementsByTagNameNS(ATOM_NS, 'entry')
        assert(len(entryElements) == 1), "Expected entry element in result from relationship URL post"
        return getSpecializedObject(CmisObject(self._cmisClient, self, xmlDoc=entryElements[0]))

    def getRelationships(self, **kwargs):

        """
        Returns a :class:`ResultSet` of :class:`Relationship` objects for each
        relationship where the source is this object.

        See CMIS specification document 2.2.8.1 getObjectRelationships

        >>> rels = tstDoc1.getRelationships()
        >>> len(rels.getResults())
        1
        >>> rel = rels.getResults().values()[0]
        >>> rel.getProperties()
        {u'cmis:objectId': u'workspace://SpacesStore/271c48dd-6548-4771-a8f5-0de69b7cdc25', u'cmis:creationDate': None, u'cmis:objectTypeId': u'R:cmiscustom:assoc', u'cmis:lastModificationDate': None, u'cmis:targetId': u'workspace://SpacesStore/0ca1aa08-cb49-42e2-8881-53aa8496a1c1', u'cmis:lastModifiedBy': None, u'cmis:baseTypeId': u'cmis:relationship', u'cmis:sourceId': u'workspace://SpacesStore/271c48dd-6548-4771-a8f5-0de69b7cdc25', u'cmis:changeToken': None, u'cmis:createdBy': None}

        The following optional arguments are supported:
         - includeSubRelationshipTypes
         - relationshipDirection
         - typeId
         - maxItems
         - skipCount
         - filter
         - includeAllowableActions
        """

        url = self._getLink(RELATIONSHIPS_REL)
        assert url != None, 'Could not determine relationships URL'

        result = self._cmisClient.get(url, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self._repository, result)

    def removePolicy(self, policyId):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.9.2 removePolicy
        """

        # depends on this object's canRemovePolicy allowable action
        if self.getAllowableActions()['canRemovePolicy']:
            raise NotImplementedError
        else:
            raise CmisException('This object has canRemovePolicy set to false')

    def getAppliedPolicies(self):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.9.3 getAppliedPolicies
        """

        # depends on this object's canGetAppliedPolicies allowable action
        if self.getAllowableActions()['canGetAppliedPolicies']:
            raise NotImplementedError
        else:
            raise CmisException('This object has canGetAppliedPolicies set to false')

    def getACL(self):

        """
        Repository.getCapabilities['ACL'] must return manage or discover.

        >>> acl = folder.getACL()
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x10071a8d0>, 'jdoe': <cmislib.model.ACE object at 0x10071a590>}

        See CMIS specification document 2.2.10.1 getACL

        The optional onlyBasicPermissions argument is currently not supported.
        """

        if self._repository.getCapabilities()['ACL']:
            # if the ACL capability is discover or manage, this must be
            # supported
            aclUrl = self._getLink(ACL_REL)
            result = self._cmisClient.get(aclUrl)
            if type(result) == HTTPError:
                raise CmisException(result.code)
            return ACL(xmlDoc=result)
        else:
            raise NotSupportedException

    def applyACL(self, acl):

        """
        Updates the object with the provided :class:`ACL`.
        Repository.getCapabilities['ACL'] must return manage to invoke this
        call.

        >>> acl = folder.getACL()
        >>> acl.addEntry(ACE('jdoe', 'cmis:write', 'true'))
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x10071a8d0>, 'jdoe': <cmislib.model.ACE object at 0x10071a590>}

        See CMIS specification document 2.2.10.2 applyACL
        """

        if self._repository.getCapabilities()['ACL'] == 'manage':
            # if the ACL capability is manage, this must be
            # supported
            # but it also depends on the canApplyACL allowable action
            # for this object
            if not isinstance(acl, ACL):
                raise CmisException('The ACL to apply must be an instance of the ACL class.')
            aclUrl = self._getLink(ACL_REL)
            assert aclUrl, "Could not determine the object's ACL URL."
            result = self._cmisClient.put(aclUrl, acl.getXmlDoc().toxml(), CMIS_ACL_TYPE)
            if type(result) == HTTPError:
                raise CmisException(result.code)
            return ACL(xmlDoc=result)
        else:
            raise NotSupportedException

    def _getSelfLink(self):

        """
        Returns the URL used to retrieve this object.
        """

        url = self._getLink(SELF_REL)

        assert len(url) > 0, "Could not determine the self link."

        return url

    def _getLink(self, rel, ltype=None):

        """
        Returns the HREF attribute of an Atom link element for the
        specified rel.
        """

        if self.xmlDoc == None:
            self.reload()
        linkElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        for linkElement in linkElements:

            if ltype:
                if linkElement.attributes.has_key('rel'):
                    relAttr = linkElement.attributes['rel'].value

                    if ltype and linkElement.attributes.has_key('type'):
                        typeAttr = linkElement.attributes['type'].value

                        if relAttr == rel and ltype.match(typeAttr):
                            return linkElement.attributes['href'].value
            else:
                if linkElement.attributes.has_key('rel'):
                    relAttr = linkElement.attributes['rel'].value

                    if relAttr == rel:
                        return linkElement.attributes['href'].value

    def _getEmptyXmlDoc(self):

        """
        Internal helper method that knows how to build an empty Atom entry.
        """

        entryXmlDoc = minidom.Document()
        entryElement = entryXmlDoc.createElementNS(ATOM_NS, "entry")
        entryElement.setAttribute('xmlns', ATOM_NS)
        entryXmlDoc.appendChild(entryElement)
        return entryXmlDoc

    def _getEntryXmlDoc(self, properties=None, contentFile=None,
                        contentType=None, contentEncoding=None):

        """
        Internal helper method that knows how to build an Atom entry based
        on the properties and, optionally, the contentFile provided.
        """

        entryXmlDoc = minidom.Document()
        entryElement = entryXmlDoc.createElementNS(ATOM_NS, "entry")
        entryElement.setAttribute('xmlns', ATOM_NS)
        entryElement.setAttribute('xmlns:app', APP_NS)
        entryElement.setAttribute('xmlns:cmisra', CMISRA_NS)
        entryXmlDoc.appendChild(entryElement)

        # if there is a File, encode it and add it to the XML
        if contentFile:
            mimetype = contentType
            encoding = contentEncoding

            # need to determine the mime type
            if not mimetype and hasattr(contentFile, 'name'):
                mimetype, encoding = mimetypes.guess_type(contentFile.name)

            if not mimetype:
                mimetype = 'application/binary'

            if not encoding:
                encoding = 'utf8'

            # This used to be ATOM_NS content but there is some debate among
            # vendors whether the ATOM_NS content must always be base64
            # encoded. The spec does mandate that CMISRA_NS content be encoded
            # and that element takes precedence over ATOM_NS content if it is
            # present, so it seems reasonable to use CMIS_RA content for now
            # and encode everything.

            fileData = contentFile.read().encode("base64")
            contentElement = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:content')
            mediaElement = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:mediatype')
            mediaElementText = entryXmlDoc.createTextNode(mimetype)
            mediaElement.appendChild(mediaElementText)
            base64Element = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:base64')
            base64ElementText = entryXmlDoc.createTextNode(fileData)
            base64Element.appendChild(base64ElementText)
            contentElement.appendChild(mediaElement)
            contentElement.appendChild(base64Element)

            entryElement.appendChild(contentElement)

        objectElement = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:object')
        objectElement.setAttribute('xmlns:cmis', CMIS_NS)
        entryElement.appendChild(objectElement)

        if properties:
            # a name is required for most things, but not for a checkout
            if properties.has_key('cmis:name'):
                titleElement = entryXmlDoc.createElementNS(ATOM_NS, "title")
                titleText = entryXmlDoc.createTextNode(properties['cmis:name'])
                titleElement.appendChild(titleText)
                entryElement.appendChild(titleElement)

            propsElement = entryXmlDoc.createElementNS(CMIS_NS, 'cmis:properties')
            objectElement.appendChild(propsElement)

            for propName, propValue in properties.items():
                """
                the name of the element here is significant: it includes the
                data type. I should be able to figure out the right type based
                on the actual type of the object passed in.

                I could do a lookup to the type definition, but that doesn't
                seem worth the performance hit
                """
                if isinstance(propValue, CmisId):
                    propElementName = 'cmis:propertyId'
                    propValueStr = propValue
                elif isinstance(propValue, str):
                    propElementName = 'cmis:propertyString'
                    propValueStr = propValue
                elif isinstance(propValue, datetime.datetime):
                    propElementName = 'cmis:propertyDateTime'
                    propValueStr = propValue.isoformat()
                elif isinstance(propValue, bool):
                    propElementName = 'cmis:propertyBoolean'
                    propValueStr = str(propValue).lower()
                elif isinstance(propValue, int):
                    propElementName = 'cmis:propertyInteger'
                    propValueStr = str(propValue)
                elif isinstance(propValue, float):
                    propElementName = 'cmis:propertyDecimal'
                    propValueStr = str(propValue)
                else:
                    propElementName = 'cmis:propertyString'
                    propValueStr = str(propValue)

                propElement = entryXmlDoc.createElementNS(CMIS_NS, propElementName)
                propElement.setAttribute('propertyDefinitionId', propName)
                valElement = entryXmlDoc.createElementNS(CMIS_NS, 'cmis:value')
                val = entryXmlDoc.createTextNode(propValueStr)
                valElement.appendChild(val)
                propElement.appendChild(valElement)
                propsElement.appendChild(propElement)

        return entryXmlDoc

    allowableActions = property(getAllowableActions)
    name = property(getName)
    id = property(getObjectId)
    properties = property(getProperties)
    title = property(getTitle)
    ACL = property(getACL)


class Document(CmisObject):

    """
    An object typically associated with file content.
    """

    def checkout(self):

        """
        Performs a checkout on the :class:`Document` and returns the
        Private Working Copy (PWC), which is also an instance of
        :class:`Document`

        See CMIS specification document 2.2.7.1 checkout

        >>> doc.getObjectId()
        u'workspace://SpacesStore/f0c8b90f-bec0-4405-8b9c-2ab570589808;1.0'
        >>> doc.isCheckedOut()
        False
        >>> pwc = doc.checkout()
        >>> doc.isCheckedOut()
        True
        """

        # get the checkedout collection URL
        checkoutUrl = self._repository.getCollectionLink(CHECKED_OUT_COLL)
        assert len(checkoutUrl) > 0, "Could not determine the checkedout collection url."

        # get this document's object ID
        # build entry XML with it
        properties = {'cmis:objectId': self.getObjectId()}
        entryXmlDoc = self._getEntryXmlDoc(properties)

        # post it to to the checkedout collection URL
        result = self._cmisClient.post(checkoutUrl,
                                       entryXmlDoc.toxml(),
                                       ATOM_XML_ENTRY_TYPE)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # now that the doc is checked out, we need to refresh the XML
        # to pick up the prop updates related to a checkout
        self.reload()

        return Document(self._cmisClient, self._repository, xmlDoc=result)

    def cancelCheckout(self):
        """
        Cancels the checkout of this object by retrieving the Private Working
        Copy (PWC) and then deleting it. After the PWC is deleted, this object
        will be reloaded to update properties related to a checkout.

        See CMIS specification document 2.2.7.2 cancelCheckOut

        >>> doc.isCheckedOut()
        True
        >>> doc.cancelCheckout()
        >>> doc.isCheckedOut()
        False
        """

        pwcDoc = self.getPrivateWorkingCopy()
        if pwcDoc:
            pwcDoc.delete()
            self.reload()

    def getPrivateWorkingCopy(self):

        """
        Retrieves the object using the object ID in the property:
        cmis:versionSeriesCheckedOutId then uses getObject to instantiate
        the object.

        >>> doc.isCheckedOut()
        False
        >>> doc.checkout()
        <cmislib.model.Document object at 0x103a25ad0>
        >>> pwc = doc.getPrivateWorkingCopy()
        >>> pwc.getTitle()
        u'sample-b (Working Copy).pdf'
        """

        # reloading the document just to make sure we've got the latest
        # and greatest PWC ID
        self.reload()
        pwcDocId = self.getProperties()['cmis:versionSeriesCheckedOutId']
        if pwcDocId:
            return self._repository.getObject(pwcDocId)

    def isCheckedOut(self):

        """
        Returns true if the document is checked out.

        >>> doc.isCheckedOut()
        True
        >>> doc.cancelCheckout()
        >>> doc.isCheckedOut()
        False
        """

        # reloading the document just to make sure we've got the latest
        # and greatest checked out prop
        self.reload()
        return parseBoolValue(self.getProperties()['cmis:isVersionSeriesCheckedOut'])

    def getCheckedOutBy(self):

        """
        Returns the ID who currently has the document checked out.
        >>> pwc = doc.checkout()
        >>> pwc.getCheckedOutBy()
        u'admin'
        """

        # reloading the document just to make sure we've got the latest
        # and greatest checked out prop
        self.reload()
        return self.getProperties()['cmis:versionSeriesCheckedOutBy']

    def checkin(self, checkinComment=None, **kwargs):

        """
        Checks in this :class:`Document` which must be a private
        working copy (PWC).

        See CMIS specification document 2.2.7.3 checkIn

        >>> doc.isCheckedOut()
        False
        >>> pwc = doc.checkout()
        >>> doc.isCheckedOut()
        True
        >>> pwc.checkin()
        <cmislib.model.Document object at 0x103a8ae90>
        >>> doc.isCheckedOut()
        False

        The following optional arguments are supported:
         - major
         - properties
         - contentStream
         - policies
         - addACEs
         - removeACEs
        """

        # Add checkin to kwargs and checkinComment, if it exists
        kwargs['checkin'] = 'true'
        kwargs['checkinComment'] = checkinComment

        # Build an empty ATOM entry
        entryXmlDoc = self._getEmptyXmlDoc()

        # Get the self link
        # Do a PUT of the empty ATOM to the self link
        url = self._getSelfLink()
        result = self._cmisClient.put(url, entryXmlDoc.toxml(), ATOM_XML_TYPE, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        return Document(self._cmisClient, self._repository, xmlDoc=result)

    def getLatestVersion(self, **kwargs):

        """
        Returns a :class:`Document` object representing the latest version in
        the version series. This is retrieved by
        See CMIS specification document 2.2.7.4 getObjectOfLatestVersion

        The following optional arguments are supported:
         - major
         - filter
         - includeRelationships
         - includePolicyIds
         - renditionFilter
         - includeACL
         - includeAllowableActions

        >>> latestDoc = doc.getLatestVersion()
        >>> latestDoc.getProperties()['cmis:versionLabel']
        u'2.1'
        >>> latestDoc = doc.getLatestVersion(major='false')
        >>> latestDoc.getProperties()['cmis:versionLabel']
        u'2.1'
        >>> latestDoc = doc.getLatestVersion(major='true')
        >>> latestDoc.getProperties()['cmis:versionLabel']
        u'2.0'
        """

        doc = None
        if kwargs.has_key('major') and kwargs['major'] == 'true':
            doc = self._repository.getObject(self.getObjectId(), returnVersion='latestmajor')
        else:
            doc = self._repository.getObject(self.getObjectId(), returnVersion='latest')

        return doc

    def getPropertiesOfLatestVersion(self, **kwargs):

        """
        Like :class:`^CmisObject.getProperties`, returns a dict of properties
        from the latest version of this object in the version series.

        See CMIS specification document 2.2.7.4 getPropertiesOfLatestVersion

        The optional major and filter arguments are supported.
        """

        latestDoc = self.getLatestVersion(**kwargs)

        return latestDoc.getProperties()

    def getAllVersions(self, **kwargs):

        """
        Returns a :class:`ResultSet` of document objects for the entire
        version history of this object, including any PWC's.

        See CMIS specification document 2.2.7.5 getAllVersions

        The optional filter and includeAllowableActions are
        supported.
        """

        # get the version history link
        versionsUrl = self._getLink(VERSION_HISTORY_REL)

        # invoke the URL
        result = self._cmisClient.get(versionsUrl, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self._repository, result)

    def getContentStream(self):

        """
        Returns the CMIS service response from invoking the 'enclosure' link.

        See CMIS specification document 2.2.4.10 getContentStream

        >>> doc.getName()
        u'sample-b.pdf'
        >>> o = open('tmp.pdf', 'wb')
        >>> result = doc.getContentStream()
        >>> o.write(result.read())
        >>> result.close()
        >>> o.close()
        >>> import os.path
        >>> os.path.getsize('tmp.pdf')
        117248

        The optional streamId argument is not yet supported.
        """

        # TODO: Need to implement the streamId

        contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        assert(len(contentElements) == 1), 'Expected to find exactly one atom:content element.'

        # if the src element exists, follow that
        if contentElements[0].attributes.has_key('src'):
            srcUrl = contentElements[0].attributes['src'].value

            # the cmis client class parses non-error responses
            result = Rest().get(srcUrl,
                                username=self._cmisClient.username,
                                password=self._cmisClient.password)
            if result.code != 200:
                raise CmisException(result.code)
            return result
        else:
            # otherwise, try to return the value of the content element
            if contentElements[0].childNodes:
                return contentElements[0].childNodes[0].data

    def setContentStream(self, contentFile):

        """
        See CMIS specification document 2.2.4.16 setContentStream

        The following optional arguments are not yet supported:
         - overwriteFlag=None,
         - changeToken=None
        """

        # get this object's content stream link
        contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        assert(len(contentElements) == 1), 'Expected to find exactly one atom:content element.'

        # if the src element exists, follow that
        if contentElements[0].attributes.has_key('src'):
            srcUrl = contentElements[0].attributes['src'].value

        # there may be times when this URL is absent, but I'm not sure how to
        # set the content stream when that is the case
        assert(srcUrl), 'Unable to determine content stream URL.'

        # build the Atom entry
        #xmlDoc = self._getEntryXmlDoc(contentFile=contentFile)

        # post the Atom entry
        result = self._cmisClient.put(srcUrl, contentFile.read(), ATOM_XML_TYPE)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # what comes back is the XML for the updated document,
        # which is not required by the spec to be the same document
        # we just updated, so use it to instantiate a new document
        # then return it
        return Document(self._cmisClient, self._repository, xmlDoc=result)

    def deleteContentStream(self, changeToken=None):

        """
        See CMIS specification document 2.2.4.17 deleteContentStream
        """

        # get this object's content stream link
        contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        assert(len(contentElements) == 1), 'Expected to find exactly one atom:content element.'

        # if the src element exists, follow that
        if contentElements[0].attributes.has_key('src'):
            srcUrl = contentElements[0].attributes['src'].value

        # there may be times when this URL is absent, but I'm not sure how to
        # delete the content stream when that is the case
        assert(srcUrl), 'Unable to determine content stream URL.'

        # delete the content stream
        result = self._cmisClient.delete(srcUrl)
        if type(result) == HTTPError:
            raise CmisException(result.code)

    def getRenditions(self):

        """
        This is not yet supported.

        See CMIS specification document 2.2.4.11 getRenditions

        The following optional arguments are not currently supported:
         - renditionFilter
         - maxItems
         - skipCount
        """

        # if Renditions capability is None, return notsupported
        if self._repository.getCapabilities()['Renditions']:
            raise NotImplementedError
        else:
            raise NotSupportedException

    checkedOut = property(isCheckedOut)


class Folder(CmisObject):

    """
    A container object that can hold other :class:`CmisObject` objects
    """

    def createFolder(self, name, properties={}):

        """
        Creates a new :class:`Folder` using the properties provided.
        Right now I expect a property called 'cmis:name' but I don't
        complain if it isn't there (although the CMIS provider will). If a
        cmis:name property isn't provided, the value passed in to the name
        argument will be used.

        To specify a custom folder type, pass in a property called
        cmis:objectTypeId set to the :class:`CmisId` representing the type ID
        of the instance you want to create. If you do not pass in an object
        type ID, an instance of 'cmis:folder' will be created.

        >>> subFolder = folder.createFolder('someSubfolder')
        >>> subFolder.getName()
        u'someSubfolder'

        The following optional arguments are not supported:
         - policies
         - addACEs
         - removeACEs
        """

        # get the folder represented by folderId.
        # we'll use his 'children' link post the new child
        postUrl = self.getChildrenLink()

        # make sure the name property gets set
        properties['cmis:name'] = name

        # hardcoding to cmis:folder if it wasn't passed in via props
        if not properties.has_key('cmis:objectTypeId'):
            properties['cmis:objectTypeId'] = CmisId('cmis:folder')
        # and checking to make sure the object type ID is an instance of CmisId
        elif not isinstance(properties['cmis:objectTypeId'], CmisId):
            properties['cmis:objectTypeId'] = CmisId(properties['cmis:objectTypeId'])

        # build the Atom entry
        entryXml = self._getEntryXmlDoc(properties)

        # post the Atom entry
        result = self._cmisClient.post(postUrl,
                                       entryXml.toxml(),
                                       ATOM_XML_ENTRY_TYPE)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # what comes back is the XML for the new folder,
        # so use it to instantiate a new folder then return it
        return Folder(self._cmisClient, self._repository, xmlDoc=result)

    def createDocument(self, name, properties={}, contentFile=None,
            contentType=None, contentEncoding=None):

        """
        Creates a new Document object in the repository using
        the properties provided.

        Right now this is basically the same as createFolder,
        but this deals with contentStreams. The common logic should
        probably be moved to CmisObject.createObject.

        The method will attempt to guess the appropriate content
        type and encoding based on the file. To specify it yourself, pass them
        in via the contentType and contentEncoding arguments.

        >>> f = open('250px-Cmis_logo.png', 'rb')
        >>> subFolder.createDocument('logo.png', contentFile=f)
        <cmislib.model.Document object at 0x10410fa10>
        >>> f.close()

        If you wanted to set one or more properties when creating the doc, pass
        in a dict, like this:

        >>> props = {'cmis:someProp':'someVal'}
        >>> f = open('250px-Cmis_logo.png', 'rb')
        >>> subFolder.createDocument('logo.png', props, contentFile=f)
        <cmislib.model.Document object at 0x10410fa10>
        >>> f.close()

        To specify a custom object type, pass in a property called
        cmis:objectTypeId set to the :class:`CmisId` representing the type ID
        of the instance you want to create. If you do not pass in an object
        type ID, an instance of 'cmis:document' will be created.

        The following optional arguments are not yet supported:
         - versioningState
         - policies
         - addACEs
         - removeACEs
        """

        # get the folder represented by folderId.
        # we'll use his 'children' link post the new child
        postUrl = self.getChildrenLink()

        # make sure a name is set
        properties['cmis:name'] = name

        # hardcoding to cmis:document if it wasn't
        # passed in via props
        if not properties.has_key('cmis:objectTypeId'):
            properties['cmis:objectTypeId'] = CmisId('cmis:document')
        # and if it was passed in, making sure it is a CmisId
        elif not isinstance(properties['cmis:objectTypeId'], CmisId):
            properties['cmis:objectTypeId'] = CmisId(properties['cmis:objectTypeId'])

        # build the Atom entry
        xmlDoc = self._getEntryXmlDoc(properties, contentFile,
                                      contentType, contentEncoding)

        # post the Atom entry
        result = self._cmisClient.post(postUrl, xmlDoc.toxml(), ATOM_XML_ENTRY_TYPE)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # what comes back is the XML for the new document,
        # so use it to instantiate a new document
        # then return it
        return Document(self._cmisClient, self._repository, xmlDoc=result)

    def getChildren(self, **kwargs):

        """
        Returns a paged :class:`ResultSet`. The result set contains a list of
        :class:`CmisObject` objects for each child of the Folder. The actual
        type of the object returned depends on the object's CMIS base type id.
        For example, the method might return a list that contains both
        :class:`Document` objects and :class:`Folder` objects.

        See CMIS specification document 2.2.3.1 getChildren

        >>> childrenRS = subFolder.getChildren()
        >>> children = childrenRS.getResults()

        The following optional arguments are supported:
         - maxItems
         - skipCount
         - orderBy
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
         - includePathSegment
        """

        # get the appropriate 'down' link
        childrenUrl = self.getChildrenLink()
        # invoke the URL
        result = self._cmisClient.get(childrenUrl, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self._repository, result)

    def getChildrenLink(self):

        """
        Gets the Atom link that knows how to return this object's children.
        """

        url = self._getLink(DOWN_REL, ATOM_XML_FEED_TYPE_P)

        assert len(url) > 0, "Could not find the children url"

        return url

    def getDescendantsLink(self):

        """
        Returns the 'down' link of type `CMIS_TREE_TYPE`

        >>> folder.getDescendantsLink()
        u'http://localhost:8080/alfresco/s/cmis/s/workspace:SpacesStore/i/86f6bf54-f0e8-4a72-8cb1-213599ba086c/descendants'
        """

        url = self._getLink(DOWN_REL, CMIS_TREE_TYPE_P)

        assert len(url) > 0, "Could not find the descendants url"

        # some servers return a depth arg as part of this URL
        # so strip it off
        if url.find("?") >= 0:
            url = url[:url.find("?")]

        return url

    def getDescendants(self, **kwargs):

        """
        Gets the descendants of this folder. The descendants are returned as
        a paged :class:`ResultSet` object. The result set contains a list of
        :class:`CmisObject` objects where the actual type of each object
        returned will vary depending on the object's base type id. For example,
        the method might return a list that contains both :class:`Document`
        objects and :class:`Folder` objects.

        See CMIS specification document 2.2.3.2 getDescendants

        The following optional argument is supported:
         - depth. Use depth=-1 for all descendants, which is the default if no
           depth is specified.

        >>> resultSet = folder.getDescendants()
        >>> len(resultSet.getResults())
        105
        >>> resultSet = folder.getDescendants(depth=1)
        >>> len(resultSet.getResults())
        103

        The following optional arguments *may* also work but haven't been
        tested:
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
         - includePathSegment

        """

        if not self._repository.getCapabilities()['GetDescendants']:
            raise NotSupportedException('This repository does not support getDescendants')

        # default the depth to -1, which is all descendants
        if "depth" not in kwargs:
            kwargs['depth'] = -1

        # get the appropriate 'down' link
        descendantsUrl = self.getDescendantsLink()

        # invoke the URL
        result = self._cmisClient.get(descendantsUrl, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self._repository, result)

    def getTree(self, **kwargs):

        """
        Unlike :class:`Folder.getChildren` or :class:`Folder.getDescendants`,
        this method returns only the descendant objects that are folders. The
        results do not include the current folder.

        See CMIS specification document 2.2.3.3 getFolderTree

        The following optional arguments are supported:
         - depth
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
         - includePathSegment

         >>> rs = folder.getTree(depth='2')
         >>> len(rs.getResults())
         3
         >>> for folder in rs.getResults().values():
         ...     folder.getTitle()
         ...
         u'subfolder2'
         u'parent test folder'
         u'subfolder'
        """

        # Get the descendants link and do a GET against it
        url = self._getLink(FOLDER_TREE_REL)
        assert url != None, 'Unable to determine folder tree link'
        result = self._cmisClient.get(url, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return ResultSet(self._cmisClient, self, result)

    def getParent(self):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.3.4 getFolderParent

        The optional filter argument is not yet supported.
        """
        # get the appropriate 'up' link
        parentUrl = self._getLink(UP_REL)
        # invoke the URL
        result = self._cmisClient.get(parentUrl)

        if type(result) == HTTPError:
            raise CmisException(result.code)

        # return the result set
        return Folder(self._cmisClient, self._repository, xmlDoc=result)

    def deleteTree(self, **kwargs):

        """
        Deletes the folder and all of its descendant objects.

        See CMIS specification document 2.2.4.15 deleteTree

        >>> resultSet = subFolder.getDescendants()
        >>> len(resultSet.getResults())
        2
        >>> subFolder.deleteTree()

        The following optional arguments are supported:
         - allVersions
         - unfileObjects
         - continueOnFailure
        """

        # Per the spec, the repo must have the GetDescendants capability
        # to support deleteTree
        if not self._repository.getCapabilities()['GetDescendants']:
            raise NotSupportedException('This repository does not support deleteTree')

        # Get the descendants link and do a DELETE against it
        url = self._getLink(DOWN_REL, CMIS_TREE_TYPE_P)
        result = self._cmisClient.delete(url, **kwargs)

        if type(result) == HTTPError:
            raise CmisException(result.code)

    def addObject(self, cmisObject):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.5.1 addObjectToFolder

        The optional allVersions argument is not yet supported.
        """

        # TODO: To be implemented.
#        It looks as if all you need to do is take the object and post its entry
#        XML to the target folder's children URL as if you were creating a new
#        object.
        raise NotImplementedError

    def removeObject(self, cmisObject):

        """
        This is not yet implemented.

        See CMIS specification document 2.2.5.2 removeObjectFromFolder
        """

        # TODO: To be implemented
        raise NotImplementedError


class Relationship(CmisObject):

    """
    Defines a relationship object between two :class:`CmisObjects` objects
    """

    def getSourceId(self):

        """
        Returns the :class:`CmisId` on the source side of the relationship.
        """

        if self.xmlDoc == None:
            self.reload()
        props = self.getProperties()
        return CmisId(props['cmis:sourceId'])

    def getTargetId(self):

        """
        Returns the :class:`CmisId` on the target side of the relationship.
        """

        if self.xmlDoc == None:
            self.reload()
        props = self.getProperties()
        return CmisId(props['cmis:targetId'])

    def getSource(self):

        """
        Returns an instance of the appropriate child-type of :class:`CmisObject`
        for the source side of the relationship.
        """

        sourceId = self.getSourceId()
        return getSpecializedObject(self._repository.getObject(sourceId))

    def getTarget(self):

        """
        Returns an instance of the appropriate child-type of :class:`CmisObject`
        for the target side of the relationship.
        """

        targetId = self.getTargetId()
        return getSpecializedObject(self._repository.getObject(targetId))

    sourceId = property(getSourceId)
    targetId = property(getTargetId)
    source = property(getSource)
    target = property(getTarget)


class Policy(CmisObject):

    """
    An arbirary object that can 'applied' to objects that the
    repository identifies as being 'controllable'.
    """

    pass


class ObjectType(object):

    """
    Represents the CMIS object type such as 'cmis:document' or 'cmis:folder'.
    Contains metadata about the type.
    """

    def __init__(self, cmisClient, repository, typeId=None, xmlDoc=None):
        """ Constructor """
        self._cmisClient = cmisClient
        self._repository = repository
        self._kwargs = None
        self._typeId = typeId
        self.xmlDoc = xmlDoc

    def __str__(self):
        """To string"""
        return self.getTypeId()

    def getTypeId(self):

        """
        Returns the type ID for this object.

        >>> docType = repo.getTypeDefinition('cmis:document')
        >>> docType.getTypeId()
        'cmis:document'
        """

        if self._typeId == None:
            if self.xmlDoc == None:
                self.reload()
            self._typeId = CmisId(self._getElementValue(CMIS_NS, 'id'))

        return self._typeId

    def _getElementValue(self, namespace, elementName):

        """
        Helper method to retrieve child element values from type XML.
        """

        if self.xmlDoc == None:
            self.reload()
        #typeEls = self.xmlDoc.getElementsByTagNameNS(CMISRA_NS, 'type')
        #assert len(typeEls) == 1, "Expected to find exactly one type element but instead found %d" % len(typeEls)
        #typeEl = typeEls[0]
        typeEl = None
        for e in self.xmlDoc.childNodes:
            if e.nodeType == e.ELEMENT_NODE and e.localName == "type":
                typeEl = e
                break

        assert typeEl, "Expected to find one child element named type"
        els = typeEl.getElementsByTagNameNS(namespace, elementName)
        if len(els) >= 1:
            el = els[0]
            if el and len(el.childNodes) >= 1:
                return el.childNodes[0].data

    def getLocalName(self):
        """Getter for cmis:localName"""
        return self._getElementValue(CMIS_NS, 'localName')

    def getLocalNamespace(self):
        """Getter for cmis:localNamespace"""
        return self._getElementValue(CMIS_NS, 'localNamespace')

    def getDisplayName(self):
        """Getter for cmis:displayName"""
        return self._getElementValue(CMIS_NS, 'displayName')

    def getQueryName(self):
        """Getter for cmis:queryName"""
        return self._getElementValue(CMIS_NS, 'queryName')

    def getDescription(self):
        """Getter for cmis:description"""
        return self._getElementValue(CMIS_NS, 'description')

    def getBaseId(self):
        """Getter for cmis:baseId"""
        return CmisId(self._getElementValue(CMIS_NS, 'baseId'))

    def isCreatable(self):
        """Getter for cmis:creatable"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'creatable'))

    def isFileable(self):
        """Getter for cmis:fileable"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'fileable'))

    def isQueryable(self):
        """Getter for cmis:queryable"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'queryable'))

    def isFulltextIndexed(self):
        """Getter for cmis:fulltextIndexed"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'fulltextIndexed'))

    def isIncludedInSupertypeQuery(self):
        """Getter for cmis:includedInSupertypeQuery"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'includedInSupertypeQuery'))

    def isControllablePolicy(self):
        """Getter for cmis:controllablePolicy"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'controllablePolicy'))

    def isControllableACL(self):
        """Getter for cmis:controllableACL"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'controllableACL'))

    def getLink(self, rel, linkType):

        """
        Gets the HREF for the link element with the specified rel and linkType.

        >>> from cmislib.model import ATOM_XML_FEED_TYPE
        >>> docType.getLink('down', ATOM_XML_FEED_TYPE)
        u'http://localhost:8080/alfresco/s/cmis/type/cmis:document/children'
        """

        linkElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        for linkElement in linkElements:

            if linkElement.attributes.has_key('rel') and linkElement.attributes.has_key('type'):
                relAttr = linkElement.attributes['rel'].value
                typeAttr = linkElement.attributes['type'].value

                if relAttr == rel and linkType.match(typeAttr):
                    return linkElement.attributes['href'].value

    def getProperties(self):

        """
        Returns a list of :class:`Property` objects representing each property
        defined for this type.

        >>> objType = repo.getTypeDefinition('cmis:relationship')
        >>> for prop in objType.properties:
        ...    print 'Id:%s' % prop.id
        ...    print 'Cardinality:%s' % prop.cardinality
        ...    print 'Description:%s' % prop.description
        ...    print 'Display name:%s' % prop.displayName
        ...    print 'Local name:%s' % prop.localName
        ...    print 'Local namespace:%s' % prop.localNamespace
        ...    print 'Property type:%s' % prop.propertyType
        ...    print 'Query name:%s' % prop.queryName
        ...    print 'Updatability:%s' % prop.updatability
        ...    print 'Inherited:%s' % prop.inherited
        ...    print 'Orderable:%s' % prop.orderable
        ...    print 'Queryable:%s' % prop.queryable
        ...    print 'Required:%s' % prop.required
        ...    print 'Open choice:%s' % prop.openChoice
        """

        if self.xmlDoc == None:
            self.reload(includePropertyDefinitions='true')
        # Currently, property defs don't have an enclosing element. And, the
        # element name varies depending on type. Until that changes, I'm going
        # to find all elements unique to a prop, then grab its parent node.
        propTypeElements = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'propertyType')
        if len(propTypeElements) <= 0:
            self.reload(includePropertyDefinitions='true')
            propTypeElements = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'propertyType')
            assert len(propTypeElements) > 0, 'Could not retrieve object type property definitions'
        props = {}
        for typeEl in propTypeElements:
            prop = Property(typeEl.parentNode)
            props[prop.id] = prop
        return props

    def reload(self, **kwargs):
        """
        This method will reload the object's data from the CMIS service.
        """
        if kwargs:
            if self._kwargs:
                self._kwargs.update(kwargs)
            else:
                self._kwargs = kwargs
        templates = self._repository.getUriTemplates()
        template = templates['typebyid']['template']
        params = {'{id}': self._typeId}
        byTypeIdUrl = multiple_replace(params, template)
        result = self._cmisClient.get(byTypeIdUrl, **kwargs)
        if type(result) == HTTPError:
            raise CmisException(result.code)

        # instantiate CmisObject objects with the results and return the list
        entryElements = result.getElementsByTagNameNS(ATOM_NS, 'entry')
        assert(len(entryElements) == 1), "Expected entry element in result from calling %s" % byTypeIdUrl
        self.xmlDoc = entryElements[0]

    id = property(getTypeId)
    localName = property(getLocalName)
    localNamespace = property(getLocalNamespace)
    displayName = property(getDisplayName)
    queryName = property(getQueryName)
    description = property(getDescription)
    baseId = property(getBaseId)
    creatable = property(isCreatable)
    fileable = property(isFileable)
    queryable = property(isQueryable)
    fulltextIndexed = property(isFulltextIndexed)
    includedInSupertypeQuery = property(isIncludedInSupertypeQuery)
    controllablePolicy = property(isControllablePolicy)
    controllableACL = property(isControllableACL)
    properties = property(getProperties)


class Property(object):

    """
    This class represents an attribute or property definition of an object
    type.
    """

    def __init__(self, propNode):
        """Constructor"""
        self.xmlDoc = propNode

    def __str__(self):
        """To string"""
        return self.getId()

    def _getElementValue(self, namespace, elementName):

        """
        Utility method for retrieving element values from the object type XML.
        """

        els = self.xmlDoc.getElementsByTagNameNS(namespace, elementName)
        if len(els) >= 1:
            el = els[0]
            if el and len(el.childNodes) >= 1:
                return el.childNodes[0].data

    def getId(self):
        """Getter for cmis:id"""
        return self._getElementValue(CMIS_NS, 'id')

    def getLocalName(self):
        """Getter for cmis:localName"""
        return self._getElementValue(CMIS_NS, 'localName')

    def getLocalNamespace(self):
        """Getter for cmis:localNamespace"""
        return self._getElementValue(CMIS_NS, 'localNamespace')

    def getDisplayName(self):
        """Getter for cmis:displayName"""
        return self._getElementValue(CMIS_NS, 'displayName')

    def getQueryName(self):
        """Getter for cmis:queryName"""
        return self._getElementValue(CMIS_NS, 'queryName')

    def getDescription(self):
        """Getter for cmis:description"""
        return self._getElementValue(CMIS_NS, 'description')

    def getPropertyType(self):
        """Getter for cmis:propertyType"""
        return self._getElementValue(CMIS_NS, 'propertyType')

    def getCardinality(self):
        """Getter for cmis:cardinality"""
        return self._getElementValue(CMIS_NS, 'cardinality')

    def getUpdatability(self):
        """Getter for cmis:updatability"""
        return self._getElementValue(CMIS_NS, 'updatability')

    def isInherited(self):
        """Getter for cmis:inherited"""
        return self._getElementValue(CMIS_NS, 'inherited')

    def isRequired(self):
        """Getter for cmis:required"""
        return self._getElementValue(CMIS_NS, 'required')

    def isQueryable(self):
        """Getter for cmis:queryable"""
        return self._getElementValue(CMIS_NS, 'queryable')

    def isOrderable(self):
        """Getter for cmis:orderable"""
        return self._getElementValue(CMIS_NS, 'orderable')

    def isOpenChoice(self):
        """Getter for cmis:openChoice"""
        return self._getElementValue(CMIS_NS, 'openChoice')

    id = property(getId)
    localName = property(getLocalName)
    localNamespace = property(getLocalNamespace)
    displayName = property(getDisplayName)
    queryName = property(getQueryName)
    description = property(getDescription)
    propertyType = property(getPropertyType)
    cardinality = property(getCardinality)
    updatability = property(getUpdatability)
    inherited = property(isInherited)
    required = property(isRequired)
    queryable = property(isQueryable)
    orderable = property(isOrderable)
    openChoice = property(isOpenChoice)


class ACL(object):

    """
    Represents the Access Control List for an object.
    """

    def __init__(self, aceList=None, xmlDoc=None):

        """
        Constructor. Pass in either a list of :class:`ACE` objects or the XML
        representation of the ACL. If you have only one ACE, don't worry about
        the list--the constructor will convert it to a list for you.
        """

        if aceList:
            self._entries = aceList
        else:
            self._entries = {}
        if xmlDoc:
            self._xmlDoc = xmlDoc
            self._entries = self._getEntriesFromXml()
        else:
            self._xmlDoc = None

    def addEntry(self, ace):

        """
        Adds an :class:`ACE` entry to the ACL.

        >>> acl = folder.getACL()
        >>> acl.addEntry(ACE('jpotts', 'cmis:read', 'true'))
        >>> acl.addEntry(ACE('jsmith', 'cmis:write', 'true'))
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>, 'jsmith': <cmislib.model.ACE object at 0x1005a2210>}
        """

        self._entries[ace.principalId] = ace

    def removeEntry(self, principalId):

        """
        Removes the :class:`ACE` entry given a specific principalId.

        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>, 'jsmith': <cmislib.model.ACE object at 0x1005a2210>}
        >>> acl.removeEntry('jsmith')
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>}
        """

        if self._entries.has_key(principalId):
            del(self._entries[principalId])

    def clearEntries(self):

        """
        Clears all :class:`ACE` entries from the ACL and removes the internal
        XML representation of the ACL.

        >>> acl = ACL()
        >>> acl.addEntry(ACE('jsmith', 'cmis:write', 'true'))
        >>> acl.addEntry(ACE('jpotts', 'cmis:write', 'true'))
        >>> acl.entries
        {'jpotts': <cmislib.model.ACE object at 0x1012c7310>, 'jsmith': <cmislib.model.ACE object at 0x100528490>}
        >>> acl.getXmlDoc()
        <xml.dom.minidom.Document instance at 0x1012cbb90>
        >>> acl.clearEntries()
        >>> acl.entries
        >>> acl.getXmlDoc()
        """

        self._entries.clear()
        self._xmlDoc = None

    def getEntries(self):

        """
        Returns a dictionary of :class:`ACE` objects for each Access Control
        Entry in the ACL. The key value is the ACE principalid.

        >>> acl = ACL()
        >>> acl.addEntry(ACE('jsmith', 'cmis:write', 'true'))
        >>> acl.addEntry(ACE('jpotts', 'cmis:write', 'true'))
        >>> for ace in acl.entries.values():
        ...     print 'principal:%s has the following permissions...' % ace.principalId
        ...     for perm in ace.permissions:
        ...             print perm
        ...
        principal:jpotts has the following permissions...
        cmis:write
        principal:jsmith has the following permissions...
        cmis:write
        """

        if self._entries:
            return self._entries
        else:
            if self._xmlDoc:
                # parse XML doc and build entry list
                self._entries = self._getEntriesFromXml()
                # then return it
                return self._entries

    def _getEntriesFromXml(self):

        """
        Helper method for getting the :class:`ACE` entries from an XML
        representation of the ACL.
        """

        if not self._xmlDoc:
            return
        result = {}
        # first child is the root node, cmis:acl
        for e in self._xmlDoc.childNodes[0].childNodes:
            if e.localName == 'permission':
                # grab the principal/principalId element value
                prinEl = e.getElementsByTagNameNS(CMIS_NS, 'principal')[0]
                if prinEl and prinEl.childNodes:
                    prinIdEl = prinEl.getElementsByTagNameNS(CMIS_NS, 'principalId')[0]
                    if prinIdEl and prinIdEl.childNodes:
                        principalId = prinIdEl.childNodes[0].data
                # grab the permission values
                permEls = e.getElementsByTagNameNS(CMIS_NS, 'permission')
                perms = []
                for permEl in permEls:
                    if permEl and permEl.childNodes:
                        perms.append(permEl.childNodes[0].data)
                # grab the direct value
                dirEl = e.getElementsByTagNameNS(CMIS_NS, 'direct')[0]
                if dirEl and dirEl.childNodes:
                    direct = dirEl.childNodes[0].data
                # create an ACE
                ace = ACE(principalId, perms, direct)
                # append it to the dictionary
                result[principalId] = ace
        return result

    def getXmlDoc(self):

        """
        This method rebuilds the local XML representation of the ACL based on
        the :class:`ACE` objects in the entries list and returns the resulting
        XML Document.
        """

        if not self.getEntries():
            return

        xmlDoc = minidom.Document()
        aclEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:acl')
        aclEl.setAttribute('xmlns:cmis', CMIS_NS)
        for ace in self.getEntries().values():
            permEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:permission')
            #principalId
            prinEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:principal')
            prinIdEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:principalId')
            prinIdElText = xmlDoc.createTextNode(ace.principalId)
            prinIdEl.appendChild(prinIdElText)
            prinEl.appendChild(prinIdEl)
            permEl.appendChild(prinEl)
            #direct
            directEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:direct')
            directElText = xmlDoc.createTextNode(ace.direct)
            directEl.appendChild(directElText)
            permEl.appendChild(directEl)
            #permissions
            for perm in ace.permissions:
                permItemEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:permission')
                permItemElText = xmlDoc.createTextNode(perm)
                permItemEl.appendChild(permItemElText)
                permEl.appendChild(permItemEl)
            aclEl.appendChild(permEl)
        xmlDoc.appendChild(aclEl)
        self._xmlDoc = xmlDoc
        return self._xmlDoc

    entries = property(getEntries)


class ACE(object):

    """
    Represents an individual Access Control Entry.
    """

    def __init__(self, principalId=None, permissions=None, direct=None):
        """Constructor"""
        self._principalId = principalId
        if permissions:
            if isinstance(permissions, str):
                self._permissions = [permissions]
            else:
                self._permissions = permissions
        self._direct = direct

    @property
    def principalId(self):
        """Getter for principalId"""
        return self._principalId

    @property
    def direct(self):
        """Getter for direct"""
        return self._direct

    @property
    def permissions(self):
        """Getter for permissions"""
        return self._permissions


class ChangeEntry(object):

    """
    Represents a change log entry. Retrieve a list of change entries via
    :meth:`Repository.getContentChanges`.

    >>> for changeEntry in rs:
    ...     changeEntry.objectId
    ...     changeEntry.id
    ...     changeEntry.changeType
    ...     changeEntry.changeTime
    ...
    'workspace://SpacesStore/0e2dc775-16b7-4634-9e54-2417a196829b'
    u'urn:uuid:0e2dc775-16b7-4634-9e54-2417a196829b'
    u'created'
    datetime.datetime(2010, 2, 11, 12, 55, 14)
    'workspace://SpacesStore/bd768f9f-99a7-4033-828d-5b13f96c6923'
    u'urn:uuid:bd768f9f-99a7-4033-828d-5b13f96c6923'
    u'updated'
    datetime.datetime(2010, 2, 11, 12, 55, 13)
    'workspace://SpacesStore/572c2cac-6b26-4cd8-91ad-b2931fe5b3fb'
    u'urn:uuid:572c2cac-6b26-4cd8-91ad-b2931fe5b3fb'
    u'updated'
    """

    def __init__(self, cmisClient, repository, xmlDoc):
        """Constructor"""
        self._cmisClient = cmisClient
        self._repository = repository
        self._xmlDoc = xmlDoc
        self._properties = {}
        self._objectId = None
        self._changeEntryId = None
        self._changeType = None
        self._changeTime = None

    def getId(self):
        """
        Returns the unique ID of the change entry.
        """
        if self._changeEntryId == None:
            self._changeEntryId = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'id')[0].firstChild.data
        return self._changeEntryId

    def getObjectId(self):
        """
        Returns the object ID of the object that changed.
        """
        if self._objectId == None:
            props = self.getProperties()
            self._objectId = CmisId(props['cmis:objectId'])
        return self._objectId

    def getChangeType(self):

        """
        Returns the type of change that occurred. The resulting value must be
        one of:
         - created
         - updated
         - deleted
         - security
         """

        if self._changeType == None:
            self._changeType = self._xmlDoc.getElementsByTagNameNS(CMIS_NS, 'changeType')[0].firstChild.data
        return self._changeType

    def getACL(self):

        """
        Gets the :class:`ACL` object that is included with this Change Entry.
        """

        # if you call getContentChanges with includeACL=true, you will get a
        # cmis:ACL entry. change entries don't appear to have a self URL so
        # instead of doing a reload with includeACL set to true, we'll either
        # see if the XML already has an ACL element and instantiate an ACL with
        # it, or we'll get the ACL_REL link, invoke that, and return the result
        if not self._repository.getCapabilities()['ACL']:
            return
        aclEls = self._xmlDoc.getElementsByTagNameNS(CMIS_NS, 'acl')
        aclUrl = self._getLink(ACL_REL)
        if (len(aclEls) == 1):
            return ACL(self._cmisClient, self._repository, aclEls[0])
        elif aclUrl:
            result = self._cmisClient.get(aclUrl)
            if type(result) == HTTPError:
                raise CmisException(result.code)
            return ACL(xmlDoc=result)

    def getChangeTime(self):

        """
        Returns a datetime object representing the time the change occurred.
        """

        if self._changeTime == None:
            self._changeTime = self._xmlDoc.getElementsByTagNameNS(CMIS_NS, 'changeTime')[0].firstChild.data
        return parseDateTimeValue(self._changeTime)

    def getProperties(self):

        """
        Returns the properties of the change entry. Note that depending on the
        capabilities of the repository ("capabilityChanges") the list may not
        include the actual property values that changed.
        """

        if self._properties == {}:
            propertiesElement = self._xmlDoc.getElementsByTagNameNS(CMIS_NS, 'properties')[0]
            for node in [e for e in propertiesElement.childNodes if e.nodeType == e.ELEMENT_NODE]:
                propertyName = node.attributes['propertyDefinitionId'].value
                if node.childNodes and \
                   node.getElementsByTagNameNS(CMIS_NS, 'value')[0] and \
                   node.getElementsByTagNameNS(CMIS_NS, 'value')[0].childNodes:
                    propertyValue = parsePropValue(
                       node.getElementsByTagNameNS(CMIS_NS, 'value')[0].childNodes[0].data,
                       node.localName)
                else:
                    propertyValue = None
                self._properties[propertyName] = propertyValue
        return self._properties

    def _getLink(self, rel):

        """
        Returns the HREF attribute of an Atom link element for the
        specified rel.
        """

        linkElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        for linkElement in linkElements:
            if linkElement.attributes.has_key('rel'):
                relAttr = linkElement.attributes['rel'].value

                if relAttr == rel:
                    return linkElement.attributes['href'].value

    id = property(getId)
    objectId = property(getObjectId)
    changeTime = property(getChangeTime)
    changeType = property(getChangeType)
    properties = property(getProperties)


class ChangeEntryResultSet(ResultSet):

    """
    A specialized type of :class:`ResultSet` that knows how to instantiate
    :class:`ChangeEntry` objects. The parent class assumes children of
    :class:`CmisObject` which doesn't work for ChangeEntries.
    """

    def __iter__(self):

        """
        Overriding to make it work with a list instead of a dict.
        """

        return self.getResults()

    def __getitem__(self, index):

        """
        Overriding to make it work with a list instead of a dict.
        """

        return self.getResults()[index]

    def __len__(self):

        """
        Overriding to make it work with a list instead of a dict.
        """

        return len(self.getResults())

    def getResults(self):

        """
        Overriding to make it work with a list instead of a dict.
        """

        if self._results:
            return self._results

        if self._xmlDoc:
            entryElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
            entries = []
            for entryElement in entryElements:
                changeEntry = ChangeEntry(self._cmisClient, self._repository, entryElement)
                entries.append(changeEntry)

            self._results = entries

        return self._results


class CmisId(str):

    """
    This is a marker class to be used for Strings that are used as CMIS ID's.
    Making the objects instances of this class makes it easier to create the
    Atom entry XML with the appropriate type, ie, cmis:propertyId, instead of
    cmis:propertyString.
    """

    pass


class UriTemplate(dict):

    """
    Simple dictionary to represent the data stored in
    a URI template entry.
    """

    def __init__(self, template, templateType, mediaType):

        """
        Constructor
        """

        dict.__init__(self)
        self['template'] = template
        self['type'] = templateType
        self['mediaType'] = mediaType


def parsePropValue(value, nodeName):

    """
    Returns a properly-typed object based on the type as specified in the
    node's element name.
    """

    if nodeName == 'propertyId':
        return CmisId(value)
    elif nodeName == 'propertyString':
        return value
    elif nodeName == 'propertyBoolean':
        bDict = {'false': False, 'true': True}
        return bDict[value.lower()]
    elif nodeName == 'propertyInteger':
        return int(value)
    elif nodeName == 'propertyDecimal':
        return float(value)
    elif nodeName == 'propertyDateTime':
        #%z doesn't seem to work, so I'm going to trunc the offset
        #not all servers return microseconds, so those go too
        return parseDateTimeValue(value)
    else:
        return value


def parseDateTimeValue(value):

    """
    Utility function to return a datetime from a string.
    """
    timeFormat = '%Y-%m-%dT%H:%M:%S'
    match = timeStampPattern.match(value)
    if match:
        return datetime.datetime.fromtimestamp(time.mktime(time.strptime(
            match.group(),
            timeFormat)))


def parseBoolValue(value):

    """
    Utility function to parse booleans and none from strings
    """

    if value == 'false':
        return False
    elif value == 'true':
        return True
    elif value == 'none':
        return None
    else:
        return value


def toCMISValue(value):

    """
    Utility function to convert Python values to CMIS string values
    """

    if value == False:
        return 'false'
    elif value == True:
        return 'true'
    elif value == None:
        return 'none'
    else:
        return value


def multiple_replace(aDict, text):

    """
    Replace in 'text' all occurences of any key in the given
    dictionary by its corresponding value.  Returns the new string.

    See http://code.activestate.com/recipes/81330/
    """

    # Create a regular expression  from the dictionary keys
    regex = re.compile("(%s)" % "|".join(map(re.escape, aDict.keys())))

    # For each match, look-up corresponding value in dictionary
    return regex.sub(lambda mo: aDict[mo.string[mo.start():mo.end()]], text)


def getSpecializedObject(obj, **kwargs):

    """
    Returns an instance of the appropriate :class:`CmisObject` class or one
    of its child types depending on the specified baseType.
    """

    if 'cmis:baseTypeId' in obj.getProperties():
        baseType = obj.getProperties()['cmis:baseTypeId']
        if baseType == 'cmis:folder':
            return Folder(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)
        if baseType == 'cmis:document':
            return Document(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)
        if baseType == 'cmis:relationship':
            return Relationship(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)
        if baseType == 'cmis:policy':
            return Policy(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)

    # if the base type ID wasn't found in the props (this can happen when
    # someone runs a query that doesn't select * or doesn't individually
    # specify baseTypeId) or if the type isn't one of the known base
    # types, give the object back
    return obj
