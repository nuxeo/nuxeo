#
#      Licensed to the Apache Software Foundation (ASF) under one
#      or more contributor license agreements.  See the NOTICE file
#      distributed with this work for additional information
#      regarding copyright ownership.  The ASF licenses this file
#      to you under the Apache License, Version 2.0 (the
#      "License"); you may not use this file except in compliance
#      with the License.  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#      Unless required by applicable law or agreed to in writing,
#      software distributed under the License is distributed on an
#      "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#      KIND, either express or implied.  See the License for the
#      specific language governing permissions and limitations
#      under the License.
#
"""
Module containing the domain objects used to work with a CMIS provider.
"""
import logging

moduleLogger = logging.getLogger('cmislib.domain')


class CmisObject(object):

    """
    Common ancestor class for other CMIS domain objects such as
    :class:`Document` and :class:`Folder`.
    """

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

        pass

    def getObjectId(self):

        """
        Returns the object ID for this object.

        >>> doc = resultSet.getResults()[0]
        >>> doc.getObjectId()
        u'workspace://SpacesStore/dc26102b-e312-471b-b2af-91bfb0225339'
        """

        pass

    def getObjectParents(self, **kwargs):
        """
        Gets the parents of this object as a :class:`ResultSet`.

        The following optional arguments are supported:
         - filter
         - includeRelationships
         - renditionFilter
         - includeAllowableActions
         - includeRelativePathSegment
        """

        pass

    def getPaths(self):
        """
        Returns the object's paths as a list of strings.
        """
        # see sub-classes for implementation
        pass

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
        """

        pass

    def getTitle(self):

        """
        Returns the value of the object's cmis:title property.
        """

        pass

    def getProperties(self):

        """
        Returns a dict of the object's properties. If CMIS returns an
        empty element for a property, the property will be in the
        dict with a value of None.

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

        pass

    def getName(self):

        """
        Returns the value of cmis:name from the getProperties() dictionary.
        We don't need a getter for every standard CMIS property, but name
        is a pretty common one so it seems to make sense.

        >>> doc.getName()
        u'system-overview.html'
        """

        pass

    def updateProperties(self, properties):

        """
        Updates the properties of an object with the properties provided.
        Only provide the set of properties that need to be updated.

        >>> folder = repo.getObjectByPath('/someFolder2')
        >>> folder.getName()
        u'someFolder2'
        >>> props = {'cmis:name': 'someFolderFoo'}
        >>> folder.updateProperties(props)
        <cmislib.model.Folder object at 0x103ab1210>
        >>> folder.getName()
        u'someFolderFoo'

        """

        pass

    def move(self, sourceFolder, targetFolder):

        """
        Moves an object from the source folder to the target folder.

        >>> sub1 = repo.getObjectByPath('/cmislib/sub1')
        >>> sub2 = repo.getObjectByPath('/cmislib/sub2')
        >>> doc = repo.getObjectByPath('/cmislib/sub1/testdoc1')
        >>> doc.move(sub1, sub2)
        """

        pass

    def delete(self, **kwargs):

        """
        Deletes this :class:`CmisObject` from the repository. Note that in the
        case of a :class:`Folder` object, some repositories will refuse to
        delete it if it contains children and some will delete it without
        complaint. If what you really want to do is delete the folder and all
        of its descendants, use :meth:`~Folder.deleteTree` instead.

        >>> folder.delete()

        The optional allVersions argument is supported.
        """

        pass

    def applyPolicy(self, policyId):

        """
        This is not yet implemented.
        """

        pass

    def createRelationship(self, targetObj, relTypeId):

        """
        Creates a relationship between this object and a specified target
        object using the relationship type specified. Returns the new
        :class:`Relationship` object.

        >>> rel = tstDoc1.createRelationship(tstDoc2, 'R:cmiscustom:assoc')
        >>> rel.getProperties()
        {u'cmis:objectId': u'workspace://SpacesStore/271c48dd-6548-4771-a8f5-0de69b7cdc25', u'cmis:creationDate': None, u'cmis:objectTypeId': u'R:cmiscustom:assoc', u'cmis:lastModificationDate': None, u'cmis:targetId': u'workspace://SpacesStore/0ca1aa08-cb49-42e2-8881-53aa8496a1c1', u'cmis:lastModifiedBy': None, u'cmis:baseTypeId': u'cmis:relationship', u'cmis:sourceId': u'workspace://SpacesStore/271c48dd-6548-4771-a8f5-0de69b7cdc25', u'cmis:changeToken': None, u'cmis:createdBy': None}

        """

        pass

    def getRelationships(self, **kwargs):

        """
        Returns a :class:`ResultSet` of :class:`Relationship` objects for each
        relationship where the source is this object.

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

        pass

    def removePolicy(self, policyId):

        """
        This is not yet implemented.
        """

        pass

    def getAppliedPolicies(self):

        """
        This is not yet implemented.
        """

        pass

    def getACL(self):

        """
        Repository.getCapabilities['ACL'] must return manage or discover.

        >>> acl = folder.getACL()
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x10071a8d0>, 'jdoe': <cmislib.model.ACE object at 0x10071a590>}

        The optional onlyBasicPermissions argument is currently not supported.
        """

        pass

    def applyACL(self, acl):

        """
        Updates the object with the provided :class:`ACL`.
        Repository.getCapabilities['ACL'] must return manage to invoke this
        call.

        >>> acl = folder.getACL()
        >>> acl.addEntry(ACE('jdoe', 'cmis:write', 'true'))
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x10071a8d0>, 'jdoe': <cmislib.model.ACE object at 0x10071a590>}
        """

        pass

    allowableActions = property(getAllowableActions)
    name = property(getName)
    id = property(getObjectId)
    properties = property(getProperties)
    title = property(getTitle)
    ACL = property(getACL)


class Repository(object):
    """
    Represents a CMIS repository. Will lazily populate itself by
    calling the repository CMIS service URL.

    You must pass in an instance of a CmisClient when creating an
    instance of this class.
    """

    def __init__(self, cmisClient, xmlDoc=None):
        """ Constructor """
        self._cmisClient = cmisClient
        self.xmlDoc = xmlDoc
        self._repositoryId = None
        self._repositoryName = None
        self._repositoryInfo = {}
        self._capabilities = {}
        self._permDefs = {}
        self._permMap = {}
        self._permissions = None
        self._propagation = None
        self.logger = logging.getLogger('cmislib.model.Repository')
        self.logger.info('Creating an instance of Repository')

    def __str__(self):
        """To string"""
        return self.getRepositoryName()

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

    def reload(self):
        """
        This method will re-fetch the repository's XML data from the CMIS
        repository.
        """

        pass

    def getRepositoryId(self):

        """
        Returns this repository's unique identifier

        >>> repo = client.getDefaultRepository()
        >>> repo.getRepositoryId()
        u'83beb297-a6fa-4ac5-844b-98c871c0eea9'
        """

        pass

    def getRepositoryName(self):

        """
        Returns this repository's name

        >>> repo = client.getDefaultRepository()
        >>> repo.getRepositoryName()
        u'Main Repository'
        """

        pass

    def getRepositoryInfo(self):

        """
        Returns a dict of repository information.

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

        pass

    def getObjectByPath(self, path, **kwargs):

        """
        Returns an object given the path to the object.

        >>> doc = repo.getObjectByPath('/jeff test/sample-b.pdf')
        >>> doc.getTitle()
        u'sample-b.pdf'

        The following optional arguments are not currently supported:
         - filter
         - includeAllowableActions
        """

        pass

    def getSupportedPermissions(self):
        """
        Returns the value of the cmis:supportedPermissions element. Valid
        values are:

         - basic: indicates that the CMIS Basic permissions are supported
         - repository: indicates that repository specific permissions are supported
         - both: indicates that both CMIS basic permissions and repository specific permissions are supported

        >>> repo.supportedPermissions
        u'both'
        """

        pass

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

        pass

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

        pass

    def getPropagation(self):

        """
        Returns the value of the cmis:propagation element. Valid values are:
          - objectonly: indicates that the repository is able to apply ACEs
            without changing the ACLs of other objects
          - propagate: indicates that the repository is able to apply ACEs to a
            given object and propagate this change to all inheriting objects

        >>> repo.propagation
        u'propagate'
        """

        pass

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

        pass

    def getRootFolder(self):
        """
        Returns the root folder of the repository

        >>> root = repo.getRootFolder()
        >>> root.getObjectId()
        u'workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348'
        """

        pass

    def getFolder(self, folderId):

        """
        Returns a :class:`Folder` object for a specified folderId

        >>> someFolder = repo.getFolder('workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348')
        >>> someFolder.getObjectId()
        u'workspace://SpacesStore/aa1ecedf-9551-49c5-831a-0502bb43f348'
        """

        pass

    def getTypeChildren(self,
                        typeId=None):

        """
        Returns a list of :class:`ObjectType` objects corresponding to the
        child types of the type specified by the typeId.

        If no typeId is provided, the result will be the same as calling
        `self.getTypeDefinitions`

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

        pass

    def getTypeDescendants(self, typeId=None, **kwargs):

        """
        Returns a list of :class:`ObjectType` objects corresponding to the
        descendant types of the type specified by the typeId.

        If no typeId is provided, the repository's "typesdescendants" URL
        will be called to determine the list of descendant types.

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

        >>> types = repo.getTypeDescendants('cmis:folder')
        >>> len(types)
        17
        >>> types = repo.getTypeDescendants('cmis:folder', depth=1)
        >>> len(types)
        12
        >>> types = repo.getTypeDescendants('cmis:folder', depth=2)
        >>> len(types)
        17
        """

        pass

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

        pass

    def getTypeDefinition(self, typeId):

        """
        Returns an :class:`ObjectType` object for the specified object type id.

        >>> folderType = repo.getTypeDefinition('cmis:folder')
        """

        pass

    def getLink(self, rel):
        """
        Returns the HREF attribute of an Atom link element for the
        specified rel.
        """

        pass

    def getCheckedOutDocs(self, **kwargs):

        """
        Returns a ResultSet of :class:`CmisObject` objects that
        are currently checked out.

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

        pass

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

        pass

    def getObject(self,
                  objectId,
                  **kwargs):

        """
        Returns an object given the specified object ID.

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

        pass

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

        pass

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

        pass

    def createDocumentFromString(self,
                                 name,
                                 properties={},
                                 parentFolder=None,
                                 contentString=None,
                                 contentType=None,
                                 contentEncoding=None):

        """
        Creates a new document setting the content to the string provided. If
        the repository supports unfiled objects, you do not have to pass in
        a parent :class:`Folder` otherwise it is required.

        This method is essentially a convenience method that wraps your string
        with a StringIO and then calls createDocument.

        >>> repo.createDocumentFromString('testdoc5', parentFolder=testFolder, contentString='Hello, World!', contentType='text/plain')
        <cmislib.model.Document object at 0x101352ed0>
        """

        pass

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

        pass

    def createDocumentFromSource(self,
                                 sourceId,
                                 properties={},
                                 parentFolder=None):
        """
        This is not yet implemented.

        The following optional arguments are not yet supported:
         - versioningState
         - policies
         - addACEs
         - removeACEs
        """

        pass

    def createFolder(self,
                     parentFolder,
                     name,
                     properties={}):

        """
        Creates a new :class:`Folder` object in the specified parentFolder.

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

        pass

    def createRelationship(self, sourceObj, targetObj, relType):
        """
        Creates a relationship of the specific type between a source object
        and a target object and returns the new :class:`Relationship` object.

        The following optional arguments are not currently supported:
         - policies
         - addACEs
         - removeACEs
        """

        pass

    def createPolicy(self, properties):
        """
        This has not yet been implemented.

        The following optional arguments are not currently supported:
         - folderId
         - policies
         - addACEs
         - removeACEs
        """

        pass

    def getCollection(self, collectionType, **kwargs):

        """
        Returns a list of objects returned for the specified collection.

        If the query collection is requested, an exception will be raised.
        That collection isn't meant to be retrieved.

        If the types collection is specified, the method returns the result of
        `getTypeDefinitions` and ignores any optional params passed in.

        >>> from cmislib.atompub.atompub_binding import TYPES_COLL
        >>> types = repo.getCollection(TYPES_COLL)
        >>> len(types)
        4
        >>> types[0].getTypeId()
        u'cmis:folder'

        Otherwise, the collection URL is invoked, and a :class:`ResultSet` is
        returned.

        >>> from cmislib.atompub.atompub_binding import CHECKED_OUT_COLL
        >>> resultSet = repo.getCollection(CHECKED_OUT_COLL)
        >>> len(resultSet.getResults())
        1
        """

        pass

    capabilities = property(getCapabilities)
    id = property(getRepositoryId)
    info = property(getRepositoryInfo)
    name = property(getRepositoryName)
    rootFolder = property(getRootFolder)
    permissionDefinitions = property(getPermissionDefinitions)
    permissionMap = property(getPermissionMap)
    propagation = property(getPropagation)
    supportedPermissions = property(getSupportedPermissions)


class ResultSet(object):

    """
    Represents a paged result set. In CMIS, this is most often an Atom feed.
    """

    def __iter__(self):
        """ Iterator for the result set """
        return iter(self.getResults())

    def __getitem__(self, index):
        """ Getter for the result set """
        return self.getResults()[index]

    def __len__(self):
        """ Len method for the result set """
        return len(self.getResults())

    def reload(self):

        """
        Re-invokes the self link for the current set of results.

        >>> resultSet.reload()

        """

        pass

    def getResults(self):

        """
        Returns the results that were fetched and cached by the get*Page call.

        >>> resultSet = repo.getCheckedOutDocs()
        >>> resultSet.hasNext()
        False
        >>> for result in resultSet.getResults():
        ...     result
        ...
        <cmislib.model.Document object at 0x104851810>
        """

        pass

    def hasObject(self, objectId):

        """
        Returns True if the specified objectId is found in the list of results,
        otherwise returns False.
        """

        pass

    def getFirst(self):

        """
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
        """

        pass

    def getPrev(self):

        """
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
        """

        pass

    def getNext(self):

        """
        Returns the next page of results as a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type.
        >>> resultSet.hasNext()
        True
        >>> results = resultSet.getNext()
        >>> for result in results:
        ...     result
        ...
        <cmislib.model.Document object at 0x10480bc90>
        """

        pass

    def getLast(self):

        """
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
        """

        pass

    def hasNext(self):

        """
        Returns True if this page contains a next link.

        >>> resultSet.hasNext()
        True
        """

        pass

    def hasPrev(self):

        """
        Returns True if this page contains a prev link. Not all CMIS providers
        implement prev links consistently.

        >>> resultSet.hasPrev()
        True
        """

        pass

    def hasFirst(self):

        """
        Returns True if this page contains a first link. Not all CMIS providers
        implement first links consistently.

        >>> resultSet.hasFirst()
        True
        """

        pass

    def hasLast(self):

        """
        Returns True if this page contains a last link. Not all CMIS providers
        implement last links consistently.

        >>> resultSet.hasLast()
        True
        """

        pass


class Document(CmisObject):

    """
    An object typically associated with file content.
    """

    def checkout(self):

        """
        Performs a checkout on the :class:`Document` and returns the
        Private Working Copy (PWC), which is also an instance of
        :class:`Document`

        >>> doc.getObjectId()
        u'workspace://SpacesStore/f0c8b90f-bec0-4405-8b9c-2ab570589808;1.0'
        >>> doc.isCheckedOut()
        False
        >>> pwc = doc.checkout()
        >>> doc.isCheckedOut()
        True
        """

        pass

    def cancelCheckout(self):
        """
        Cancels the checkout of this object by retrieving the Private Working
        Copy (PWC) and then deleting it. After the PWC is deleted, this object
        will be reloaded to update properties related to a checkout.

        >>> doc.isCheckedOut()
        True
        >>> doc.cancelCheckout()
        >>> doc.isCheckedOut()
        False
        """

        pass

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

        pass

    def isCheckedOut(self):

        """
        Returns true if the document is checked out.

        >>> doc.isCheckedOut()
        True
        >>> doc.cancelCheckout()
        >>> doc.isCheckedOut()
        False
        """

        pass

    def getCheckedOutBy(self):

        """
        Returns the ID who currently has the document checked out.
        >>> pwc = doc.checkout()
        >>> pwc.getCheckedOutBy()
        u'admin'
        """

        pass

    def checkin(self, checkinComment=None, **kwargs):

        """
        Checks in this :class:`Document` which must be a private
        working copy (PWC).

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

        pass

    def getLatestVersion(self, **kwargs):

        """
        Returns a :class:`Document` object representing the latest version in
        the version series.

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

        pass

    def getPropertiesOfLatestVersion(self, **kwargs):

        """
        Like :class:`^CmisObject.getProperties`, returns a dict of properties
        from the latest version of this object in the version series.

        The optional major and filter arguments are supported.
        """

        pass

    def getAllVersions(self, **kwargs):

        """
        Returns a :class:`ResultSet` of document objects for the entire
        version history of this object, including any PWC's.

        The optional filter and includeAllowableActions are
        supported.
        """

        pass

    def getContentStream(self):

        """
        Returns the CMIS service response from invoking the 'enclosure' link.

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

        pass

    def setContentStream(self, contentFile, contentType=None):

        """
        Sets the content stream on this object.

        The following optional arguments are not yet supported:
         - overwriteFlag=None
        """

        pass

    def deleteContentStream(self):

        """
        Delete's the content stream associated with this object.
        """

        pass

    def getRenditions(self):

        """
        Returns an array of :class:`Rendition` objects. The repository
        must support the Renditions capability.

        The following optional arguments are not currently supported:
         - renditionFilter
         - maxItems
         - skipCount
        """

        pass

    checkedOut = property(isCheckedOut)

    def getPaths(self):
        """
        Returns the Document's paths by asking for the parents with the
        includeRelativePathSegment flag set to true, then concats the value
        of cmis:path with the relativePathSegment.
        """

        pass


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

        pass

    def createDocumentFromString(self,
                                 name,
                                 properties={},
                                 contentString=None,
                                 contentType=None,
                                 contentEncoding=None):

        """
        Creates a new document setting the content to the string provided. If
        the repository supports unfiled objects, you do not have to pass in
        a parent :class:`Folder` otherwise it is required.

        This method is essentially a convenience method that wraps your string
        with a StringIO and then calls createDocument.

        >>> testFolder.createDocumentFromString('testdoc3', contentString='hello, world', contentType='text/plain')
        """

        pass

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

        pass

    def getChildren(self, **kwargs):

        """
        Returns a paged :class:`ResultSet`. The result set contains a list of
        :class:`CmisObject` objects for each child of the Folder. The actual
        type of the object returned depends on the object's CMIS base type id.
        For example, the method might return a list that contains both
        :class:`Document` objects and :class:`Folder` objects.

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

        pass

    def getDescendants(self, **kwargs):

        """
        Gets the descendants of this folder. The descendants are returned as
        a paged :class:`ResultSet` object. The result set contains a list of
        :class:`CmisObject` objects where the actual type of each object
        returned will vary depending on the object's base type id. For example,
        the method might return a list that contains both :class:`Document`
        objects and :class:`Folder` objects.

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

        pass

    def getTree(self, **kwargs):

        """
        Unlike :class:`Folder.getChildren` or :class:`Folder.getDescendants`,
        this method returns only the descendant objects that are folders. The
        results do not include the current folder.

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

        pass

    def getParent(self):

        """
        The optional filter argument is not yet supported.
        """

        pass

    def deleteTree(self, **kwargs):

        """
        Deletes the folder and all of its descendant objects.

        >>> resultSet = subFolder.getDescendants()
        >>> len(resultSet.getResults())
        2
        >>> subFolder.deleteTree()

        The following optional arguments are supported:
         - allVersions
         - unfileObjects
         - continueOnFailure
        """

        pass

    def addObject(self, cmisObject, **kwargs):

        """
        Adds the specified object as a child of this object. No new object is
        created. The repository must support multifiling for this to work.

        >>> sub1 = repo.getObjectByPath("/cmislib/sub1")
        >>> sub2 = repo.getObjectByPath("/cmislib/sub2")
        >>> doc = sub1.createDocument("testdoc1")
        >>> len(sub1.getChildren())
        1
        >>> len(sub2.getChildren())
        0
        >>> sub2.addObject(doc)
        >>> len(sub2.getChildren())
        1
        >>> sub2.getChildren()[0].name
        u'testdoc1'

        The following optional arguments are supported:
         - allVersions
        """

        pass

    def removeObject(self, cmisObject):

        """
        Removes the specified object from this folder. The repository must
        support unfiling for this to work.
        """

        pass

    def getPaths(self):
        """
        Returns the paths as a list of strings. The spec says folders cannot
        be multi-filed, so this should always be one value. We return a list
        to be symmetric with the same method in :class:`Document`.
        """

        pass


class Relationship(CmisObject):

    """
    Defines a relationship object between two :class:`CmisObjects` objects
    """

    def getSourceId(self):

        """
        Returns the :class:`CmisId` on the source side of the relationship.
        """

        pass

    def getTargetId(self):

        """
        Returns the :class:`CmisId` on the target side of the relationship.
        """

        pass

    def getSource(self):

        """
        Returns an instance of the appropriate child-type of :class:`CmisObject`
        for the source side of the relationship.
        """

        pass

    def getTarget(self):

        """
        Returns an instance of the appropriate child-type of :class:`CmisObject`
        for the target side of the relationship.
        """

        pass

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

    def getTypeId(self):

        """
        Returns the type ID for this object.

        >>> docType = repo.getTypeDefinition('cmis:document')
        >>> docType.getTypeId()
        'cmis:document'
        """

        pass

    def getLocalName(self):
        """Getter for cmis:localName"""
        pass

    def getLocalNamespace(self):
        """Getter for cmis:localNamespace"""
        pass

    def getDisplayName(self):
        """Getter for cmis:displayName"""
        pass

    def getQueryName(self):
        """Getter for cmis:queryName"""
        pass

    def getDescription(self):
        """Getter for cmis:description"""
        pass

    def getBaseId(self):
        """Getter for cmis:baseId"""
        pass

    def isCreatable(self):
        """Getter for cmis:creatable"""
        pass

    def isFileable(self):
        """Getter for cmis:fileable"""
        pass

    def isQueryable(self):
        """Getter for cmis:queryable"""
        pass

    def isFulltextIndexed(self):
        """Getter for cmis:fulltextIndexed"""
        pass

    def isIncludedInSupertypeQuery(self):
        """Getter for cmis:includedInSupertypeQuery"""
        pass

    def isControllablePolicy(self):
        """Getter for cmis:controllablePolicy"""
        pass

    def isControllableACL(self):
        """Getter for cmis:controllableACL"""
        pass

    def getLink(self, rel, linkType):

        """
        Gets the HREF for the link element with the specified rel and linkType.

        >>> from cmislib.atompub.atompub_binding import ATOM_XML_FEED_TYPE
        >>> docType.getLink('down', ATOM_XML_FEED_TYPE)
        u'http://localhost:8080/alfresco/s/cmis/type/cmis:document/children'
        """

        pass

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

        pass

    def reload(self, **kwargs):
        """
        This method will reload the object's data from the CMIS service.
        """
        pass

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

    def getId(self):
        """Getter for cmis:id"""
        pass

    def getLocalName(self):
        """Getter for cmis:localName"""
        pass

    def getLocalNamespace(self):
        """Getter for cmis:localNamespace"""
        pass

    def getDisplayName(self):
        """Getter for cmis:displayName"""
        pass

    def getQueryName(self):
        """Getter for cmis:queryName"""
        pass

    def getDescription(self):
        """Getter for cmis:description"""
        pass

    def getPropertyType(self):
        """Getter for cmis:propertyType"""
        pass

    def getCardinality(self):
        """Getter for cmis:cardinality"""
        pass

    def getUpdatability(self):
        """Getter for cmis:updatability"""
        pass

    def isInherited(self):
        """Getter for cmis:inherited"""
        pass

    def isRequired(self):
        """Getter for cmis:required"""
        pass

    def isQueryable(self):
        """Getter for cmis:queryable"""
        pass

    def isOrderable(self):
        """Getter for cmis:orderable"""
        pass

    def isOpenChoice(self):
        """Getter for cmis:openChoice"""
        pass

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

    def addEntry(self, principalId, access, direct):

        """
        Adds an :class:`ACE` entry to the ACL.

        >>> acl = folder.getACL()
        >>> acl.addEntry('jpotts', 'cmis:read', 'true')
        >>> acl.addEntry('jsmith', 'cmis:write', 'true')
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>, 'jsmith': <cmislib.model.ACE object at 0x1005a2210>}
        """

        pass

    def removeEntry(self, principalId):

        """
        Removes the :class:`ACE` entry given a specific principalId.

        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>, 'jsmith': <cmislib.model.ACE object at 0x1005a2210>}
        >>> acl.removeEntry('jsmith')
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>}
        """

        pass

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

        pass

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

        pass

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

        self.logger = logging.getLogger('cmislib.model.ACE')
        self.logger.info('Creating an instance of ACE for %s' % principalId)

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

    def getId(self):
        """
        Returns the unique ID of the change entry.
        """
        pass

    def getObjectId(self):
        """
        Returns the object ID of the object that changed.
        """
        pass

    def getChangeType(self):

        """
        Returns the type of change that occurred. The resulting value must be
        one of:

         - created
         - updated
         - deleted
         - security
        """
        pass

    def getACL(self):

        """
        Gets the :class:`ACL` object that is included with this Change Entry.
        """

        pass

    def getChangeTime(self):

        """
        Returns a datetime object representing the time the change occurred.
        """

        pass

    def getProperties(self):

        """
        Returns the properties of the change entry. Note that depending on the
        capabilities of the repository ("capabilityChanges") the list may not
        include the actual property values that changed.
        """

        pass

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

        return iter(self.getResults())

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

        pass


class Rendition(object):

    """
    This class represents a Rendition.
    """

    def __str__(self):
        """To string"""
        return self.getStreamId()

    def getStreamId(self):
        """Getter for the rendition's stream ID"""
        pass

    def getMimeType(self):
        """Getter for the rendition's mime type"""
        pass

    def getLength(self):
        """Getter for the renditions's length"""
        pass

    def getTitle(self):
        """Getter for the renditions's title"""
        pass

    def getKind(self):
        """Getter for the renditions's kind"""
        pass

    def getHeight(self):
        """Getter for the renditions's height"""
        pass

    def getWidth(self):
        """Getter for the renditions's width"""
        pass

    def getHref(self):
        """Getter for the renditions's href"""
        pass

    def getRenditionDocumentId(self):
        """Getter for the renditions's width"""
        pass

    streamId = property(getStreamId)
    mimeType = property(getMimeType)
    length = property(getLength)
    title = property(getTitle)
    kind = property(getKind)
    height = property(getHeight)
    width = property(getWidth)
    href = property(getHref)
    renditionDocumentId = property(getRenditionDocumentId)


class CmisId(str):

    """
    This is a marker class to be used for Strings that are used as CMIS ID's.
    Making the objects instances of this class makes it easier to create the
    Atom entry XML with the appropriate type, ie, cmis:propertyId, instead of
    cmis:propertyString.
    """

    pass
