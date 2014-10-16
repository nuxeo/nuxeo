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
Module containing the Atom Pub binding-specific objects used to work with a CMIS
provider.
"""
from cmislib.cmis_services import Binding, RepositoryServiceIfc
from cmislib.domain import CmisId, CmisObject, ObjectType, Property, ACL, ACE, ChangeEntry, ResultSet, Rendition
from cmislib import messages
from cmislib.net import RESTService as Rest
from cmislib.exceptions import CmisException, \
    ObjectNotFoundException, InvalidArgumentException, \
    NotSupportedException
from cmislib.util import multiple_replace, parsePropValue, parseBoolValue, toCMISValue, parseDateTimeValue

from urllib import quote
from urlparse import urlparse, urlunparse
import re
import mimetypes
from xml.parsers.expat import ExpatError
import datetime
import StringIO
import logging
from xml.dom import minidom

moduleLogger = logging.getLogger('cmislib.atompub_binding')

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
POLICIES_REL = 'http://docs.oasis-open.org/ns/cmis/link/200908/policies'
RENDITION_REL = 'alternate'

# Collection types
QUERY_COLL = 'query'
TYPES_COLL = 'types'
CHECKED_OUT_COLL = 'checkedout'
UNFILED_COLL = 'unfiled'
ROOT_COLL = 'root'


class AtomPubBinding(Binding):

    """
    The binding responsible for talking to the CMIS server via the AtomPub
    Publishing Protocol.
    """

    def __init__(self, **kwargs):
        self.extArgs = kwargs

    def getRepositoryService(self):
        return RepositoryService()

    def get(self, url, username, password, **kwargs):

        """
        Does a get against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects do it for you.

        For example, if you need to get a specific object by object id, try
        :class:`Repository.getObject`. If you have a path instead of an object
        id, use :class:`Repository.getObjectByPath`. Or, you could start with
        the root folder (:class:`Repository.getRootFolder`) and drill down from
        there.
        """

        # merge the cmis client extended args with the ones that got passed in
        if len(self.extArgs) > 0:
            kwargs.update(self.extArgs)

        resp, content = Rest().get(url,
                                   username=username,
                                   password=password,
                                   **kwargs)
        if resp['status'] != '200':
            self._processCommonErrors(resp, url)
            return content
        else:
            try:
                return minidom.parseString(content)
            except ExpatError:
                raise CmisException('Could not parse server response', url)

    def delete(self, url, username, password, **kwargs):

        """
        Does a delete against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects do it for you.

        For example, to delete a folder you'd call :class:`Folder.delete` and
        to delete a document you'd call :class:`Document.delete`.
        """

        # merge the cmis client extended args with the ones that got passed in
        if len(self.extArgs) > 0:
            kwargs.update(self.extArgs)

        resp, content = Rest().delete(url,
                                      username=username,
                                      password=password,
                                      **kwargs)
        if resp['status'] != '200' and resp['status'] != '204':
            self._processCommonErrors(resp, url)
            return content
        else:
            pass

    def post(self, url, username, password, payload, contentType, **kwargs):

        """
        Does a post against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects do it for you.

        For example, to update the properties on an object, you'd call
        :class:`CmisObject.updateProperties`. Or, to check in a document that's
        been checked out, you'd call :class:`Document.checkin` on the PWC.
        """

        # merge the cmis client extended args with the ones that got passed in
        if len(self.extArgs) > 0:
            kwargs.update(self.extArgs)

        resp, content = Rest().post(url,
                                    payload,
                                    contentType,
                                    username=username,
                                    password=password,
                                    **kwargs)
        if resp['status'] == '200':
            try:
                return minidom.parseString(content)
            except ExpatError:
                raise CmisException('Could not parse server response', url)
        elif resp['status'] == '201':
            try:
                return minidom.parseString(content)
            except ExpatError:
                raise CmisException('Could not parse server response', url)
        else:
            self._processCommonErrors(resp, url)
            return resp

    def put(self, url, username, password, payload, contentType, **kwargs):

        """
        Does a put against the CMIS service. More than likely, you will not
        need to call this method. Instead, let the other objects do it for you.

        For example, to update the properties on an object, you'd call
        :class:`CmisObject.updateProperties`. Or, to check in a document that's
        been checked out, you'd call :class:`Document.checkin` on the PWC.
        """

        # merge the cmis client extended args with the ones that got passed in
        if len(self.extArgs) > 0:
            kwargs.update(self.extArgs)

        resp, content = Rest().put(url,
                                   payload,
                                   contentType,
                                   username=username,
                                   password=password,
                                   **kwargs)
        if resp['status'] != '200' and resp['status'] != '201':
            self._processCommonErrors(resp, url)
            return content
        else:
            try:
                return minidom.parseString(content)
            except ExpatError:
                # This may happen and is normal
                return None


class RepositoryService(RepositoryServiceIfc):

    """
    The repository service for the AtomPub binding.
    """

    def __init__(self):
        self._uriTemplates = {}
        self.logger = logging.getLogger('cmislib.atompub_binding.RepositoryService')

    def reload(self, obj):

        """ Reloads the state of the repository object."""

        self.logger.debug('Reload called on object')
        obj.xmlDoc = obj._cmisClient.binding.get(obj._cmisClient.repositoryUrl.encode('utf-8'),
                                                 obj._cmisClient.username,
                                                 obj._cmisClient.password)
        obj._initData()

    def getRepository(self, client, repositoryId):

        """
        Get the repository for the specified repositoryId.
        """

        doc = client.binding.get(client.repositoryUrl, client.username, client.password, **client.extArgs)
        workspaceElements = doc.getElementsByTagNameNS(APP_NS, 'workspace')

        for workspaceElement in workspaceElements:
            idElement = workspaceElement.getElementsByTagNameNS(CMIS_NS, 'repositoryId')
            if idElement[0].childNodes[0].data == repositoryId:
                return AtomPubRepository(self, workspaceElement)

        raise ObjectNotFoundException(url=client.repositoryUrl)

    def getRepositories(self, client):

        """
        Get all of the repositories provided by the server.
        """

        result = client.binding.get(client.repositoryUrl, client.username, client.password, **client.extArgs)

        workspaceElements = result.getElementsByTagNameNS(APP_NS, 'workspace')
        # instantiate a Repository object using every workspace element
        # in the service URL then ask the repository object for its ID
        # and name, and return that back

        repositories = []
        for node in [e for e in workspaceElements if e.nodeType == e.ELEMENT_NODE]:
            repository = AtomPubRepository(client, node)
            repositories.append({'repositoryId': repository.getRepositoryId(),
                                 'repositoryName': repository.getRepositoryInfo()['repositoryName']})
        return repositories

    def getDefaultRepository(self, client):

        """
        Returns the default repository for the server via the AtomPub binding.
        """

        doc = client.binding.get(client.repositoryUrl, client.username, client.password, **client.extArgs)
        workspaceElements = doc.getElementsByTagNameNS(APP_NS, 'workspace')
        # instantiate a Repository object with the first workspace
        # element we find
        repository = AtomPubRepository(client, [e for e in workspaceElements if e.nodeType == e.ELEMENT_NODE][0])
        return repository


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


class AtomPubCmisObject(CmisObject):

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
        self.logger = logging.getLogger('cmislib.atompub_binding.AtomPubCmisObject')
        self.logger.info('Creating an instance of CmisObject')

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

        self.logger.debug('Reload called on CmisObject')
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

        params = {'{id}': self.getObjectId(),
                  '{filter}': '',
                  '{includeAllowableActions}': 'false',
                  '{includePolicyIds}': 'false',
                  '{includeRelationships}': '',
                  '{includeACL}': 'false',
                  '{renditionFilter}': ''}

        options = {}
        addOptions = {}  # args specified, but not in the template
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

        self.xmlDoc = self._cmisClient.binding.get(byObjectIdUrl.encode('utf-8'),
                                                   self._cmisClient.username,
                                                   self._cmisClient.password,
                                                   **addOptions)
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

        if self._objectId is None:
            if self.xmlDoc is None:
                self.logger.debug('Both objectId and xmlDoc were None, reloading')
                self.reload()
            props = self.getProperties()
            self._objectId = CmisId(props['cmis:objectId'])
        return self._objectId

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
        # get the appropriate 'up' link
        parentUrl = self._getLink(UP_REL)

        if parentUrl is None:
            raise NotSupportedException('Root folder does not support getObjectParents')

        # invoke the URL
        result = self._cmisClient.binding.get(parentUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self._repository, result)

    def getPaths(self):
        """
        Returns the object's paths as a list of strings.
        """
        # see sub-classes for implementation
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

        # if Renditions capability is None, return notsupported
        if self._repository.getCapabilities()['Renditions']:
            pass
        else:
            raise NotSupportedException

        if self.xmlDoc is None:
            self.reload()

        linkElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        renditions = []
        for linkElement in linkElements:

            if linkElement.attributes.has_key('rel'):
                relAttr = linkElement.attributes['rel'].value

                if relAttr == RENDITION_REL:
                    renditions.append(AtomPubRendition(linkElement))
        return renditions

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
        Returns the value of the object's atom:title property.
        """

        if self.xmlDoc is None:
            self.reload()

        titleElement = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'title')[0]

        if titleElement and titleElement.childNodes:
            return titleElement.childNodes[0].data

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

        # TODO implement filter
        if self._properties == {}:
            if self.xmlDoc is None:
                self.reload()
            propertiesElement = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'properties')[0]
            # cpattern = re.compile(r'^property([\w]*)')
            for node in [e for e in propertiesElement.childNodes if e.nodeType == e.ELEMENT_NODE and e.namespaceURI == CMIS_NS]:
                # propertyId, propertyString, propertyDateTime
                # propertyType = cpattern.search(node.localName).groups()[0]
                propertyName = node.attributes['propertyDefinitionId'].value
                if node.childNodes and \
                   node.getElementsByTagNameNS(CMIS_NS, 'value')[0] and \
                   node.getElementsByTagNameNS(CMIS_NS, 'value')[0].childNodes:
                    valNodeList = node.getElementsByTagNameNS(CMIS_NS, 'value')
                    if len(valNodeList) == 1:
                        propertyValue = parsePropValue(valNodeList[0].
                                                       childNodes[0].data,
                                                       node.localName)
                    else:
                        propertyValue = []
                        for valNode in valNodeList:
                            propertyValue.append(parsePropValue(valNode.
                                                                childNodes[0].data,
                                                                node.localName))
                else:
                    propertyValue = None
                self._properties[propertyName] = propertyValue

            for node in [e for e in self.xmlDoc.childNodes if e.nodeType == e.ELEMENT_NODE and e.namespaceURI == CMISRA_NS]:
                propertyName = node.nodeName
                if node.childNodes:
                    propertyValue = node.firstChild.nodeValue
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

        if self._name is None:
            self._name = self.getProperties()['cmis:name']
        return self._name

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

        self.logger.debug('Inside updateProperties')

        # get the self link
        selfUrl = self._getSelfLink()

        # if we have a change token, we must pass it back, per the spec
        args = {}
        if self.properties.has_key('cmis:changeToken') and self.properties['cmis:changeToken'] is not None:
            self.logger.debug('Change token present, adding it to args')
            args = {"changeToken": self.properties['cmis:changeToken']}

        # the getEntryXmlDoc function may need the object type
        objectTypeId = None
        if self.properties.has_key('cmis:objectTypeId') and not properties.has_key('cmis:objectTypeId'):
            objectTypeId = self.properties['cmis:objectTypeId']
            self.logger.debug('This object type is:%s', objectTypeId)

        # build the entry based on the properties provided
        xmlEntryDoc = getEntryXmlDoc(self._repository, objectTypeId, properties)

        self.logger.debug('xmlEntryDoc:' + xmlEntryDoc.toxml())

        # do a PUT of the entry
        updatedXmlDoc = self._cmisClient.binding.put(selfUrl.encode('utf-8'),
                                                     self._cmisClient.username,
                                                     self._cmisClient.password,
                                                     xmlEntryDoc.toxml(encoding='utf-8'),
                                                     ATOM_XML_TYPE,
                                                     **args)

        # reset the xmlDoc for this object with what we got back from
        # the PUT, then call initData we dont' want to call
        # self.reload because we've already got the parsed XML--
        # there's no need to fetch it again
        self.xmlDoc = updatedXmlDoc
        self._initData()
        return self

    def move(self, sourceFolder, targetFolder):

        """
        Moves an object from the source folder to the target folder.

        >>> sub1 = repo.getObjectByPath('/cmislib/sub1')
        >>> sub2 = repo.getObjectByPath('/cmislib/sub2')
        >>> doc = repo.getObjectByPath('/cmislib/sub1/testdoc1')
        >>> doc.move(sub1, sub2)
        """

        postUrl = targetFolder.getChildrenLink()

        args = {"sourceFolderId": sourceFolder.id}

        # post the Atom entry
        self._cmisClient.binding.post(postUrl.encode('utf-8'),
                                      self._cmisClient.username,
                                      self._cmisClient.password,
                                      self.xmlDoc.toxml(encoding='utf-8'),
                                      ATOM_XML_ENTRY_TYPE,
                                      **args)

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

        url = self._getSelfLink()
        self._cmisClient.binding.delete(url.encode('utf-8'),
                                        self._cmisClient.username,
                                        self._cmisClient.password,
                                        **kwargs)

    def applyPolicy(self, policyId):

        """
        This is not yet implemented.
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
        xmlDoc = getEntryXmlDoc(self._repository, properties=props)

        url = self._getLink(RELATIONSHIPS_REL)
        assert url is not None, 'Could not determine relationships URL'

        result = self._cmisClient.binding.post(url.encode('utf-8'),
                                               self._cmisClient.username,
                                               self._cmisClient.password,
                                               xmlDoc.toxml(encoding='utf-8'),
                                               ATOM_XML_TYPE)

        # instantiate CmisObject objects with the results and return the list
        entryElements = result.getElementsByTagNameNS(ATOM_NS, 'entry')
        assert(len(entryElements) == 1), "Expected entry element in result from relationship URL post"
        return getSpecializedObject(AtomPubCmisObject(self._cmisClient, self, xmlDoc=entryElements[0]))

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

        url = self._getLink(RELATIONSHIPS_REL)
        assert url is not None, 'Could not determine relationships URL'

        result = self._cmisClient.binding.get(url.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self._repository, result)

    def removePolicy(self, policyId):

        """
        This is not yet implemented.
        """

        # depends on this object's canRemovePolicy allowable action
        if self.getAllowableActions()['canRemovePolicy']:
            raise NotImplementedError
        else:
            raise CmisException('This object has canRemovePolicy set to false')

    def getAppliedPolicies(self):

        """
        This is not yet implemented.
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

        The optional onlyBasicPermissions argument is currently not supported.
        """

        if self._repository.getCapabilities()['ACL']:
            # if the ACL capability is discover or manage, this must be
            # supported
            aclUrl = self._getLink(ACL_REL)
            result = self._cmisClient.binding.get(aclUrl.encode('utf-8'),
                                                  self._cmisClient.username,
                                                  self._cmisClient.password)
            return AtomPubACL(xmlDoc=result)
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
            result = self._cmisClient.binding.put(aclUrl.encode('utf-8'),
                                                  self._cmisClient.username,
                                                  self._cmisClient.password,
                                                  acl.getXmlDoc().toxml(encoding='utf-8'),
                                                  CMIS_ACL_TYPE)
            return AtomPubACL(xmlDoc=result)
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

        if self.xmlDoc is None:
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

    allowableActions = property(getAllowableActions)
    name = property(getName)
    id = property(getObjectId)
    properties = property(getProperties)
    title = property(getTitle)
    ACL = property(getACL)


class AtomPubRepository(object):

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
        self._uriTemplates = {}
        self._permDefs = {}
        self._permMap = {}
        self._permissions = None
        self._propagation = None
        self.logger = logging.getLogger('cmislib.model.Repository')
        self.logger.info('Creating an instance of Repository')

    def __str__(self):
        """To string"""
        return self.getRepositoryName()

    def reload(self):
        """
        This method will re-fetch the repository's XML data from the CMIS
        repository.
        """
        self.logger.debug('Reload called on object')
        self.xmlDoc = self._cmisClient.binding.get(self._cmisClient.repositoryUrl.encode('utf-8'),
                                                   self._cmisClient.username,
                                                   self._cmisClient.password)
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

         - basic: indicates that the CMIS Basic permissions are supported
         - repository: indicates that repository specific permissions are supported
         - both: indicates that both CMIS basic permissions and repository specific permissions are supported

        >>> repo.supportedPermissions
        u'both'
        """

        if not self.getCapabilities()['ACL']:
            raise NotSupportedException(messages.NO_ACL_SUPPORT)

        if not self._permissions:
            if self.xmlDoc is None:
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
            if self.xmlDoc is None:
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
            if self.xmlDoc is None:
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
          - objectonly: indicates that the repository is able to apply ACEs
            without changing the ACLs of other objects
          - propagate: indicates that the repository is able to apply ACEs to a
            given object and propagate this change to all inheriting objects

        >>> repo.propagation
        u'propagate'
        """

        if not self.getCapabilities()['ACL']:
            raise NotSupportedException(messages.NO_ACL_SUPPORT)

        if not self._propagation:
            if self.xmlDoc is None:
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

        if self._repositoryId is None:
            if self.xmlDoc is None:
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

        if self._repositoryName is None:
            if self.xmlDoc is None:
                self.reload()
            self._repositoryName = self.xmlDoc.getElementsByTagNameNS(CMIS_NS, 'repositoryName')[0].firstChild.data
        return self._repositoryName

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

        if not self._repositoryInfo:
            if self.xmlDoc is None:
                self.reload()
            repoInfoElement = self.xmlDoc.getElementsByTagNameNS(CMISRA_NS, 'repositoryInfo')[0]
            for node in repoInfoElement.childNodes:
                if node.nodeType == node.ELEMENT_NODE and \
                   node.localName != 'capabilities' and \
                   node.localName != 'aclCapability':
                    try:
                        data = node.childNodes[0].data
                    except IndexError:
                        data = None
                    except AttributeError:
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
            if self.xmlDoc is None:
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
        folder = AtomPubFolder(self._cmisClient, self, rootFolderId)
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
        return AtomPubFolder(self._cmisClient, self, xmlDoc=retObject.xmlDoc)

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

        # Unfortunately, the spec does not appear to present a way to
        # know how to get the children of a specific type without first
        # retrieving the type, then asking it for one of its navigational
        # links.

        # if a typeId is specified, get it, then get its "down" link
        if typeId:
            targetType = self.getTypeDefinition(typeId)
            childrenUrl = targetType.getLink(DOWN_REL, ATOM_XML_FEED_TYPE_P)
            typesXmlDoc = self._cmisClient.binding.get(childrenUrl.encode('utf-8'),
                                                       self._cmisClient.username,
                                                       self._cmisClient.password)
            entryElements = typesXmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
            types = []
            for entryElement in entryElements:
                objectType = AtomPubObjectType(self._cmisClient,
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

        typesXmlDoc = self._cmisClient.binding.get(descendUrl.encode('utf-8'),
                                                   self._cmisClient.username,
                                                   self._cmisClient.password,
                                                   **kwargs)
        entryElements = typesXmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
        types = []
        for entryElement in entryElements:
            objectType = AtomPubObjectType(self._cmisClient,
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
        typesXmlDoc = self._cmisClient.binding.get(typesUrl,
                                                   self._cmisClient.username,
                                                   self._cmisClient.password,
                                                   **kwargs)
        entryElements = typesXmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
        types = []
        for entryElement in entryElements:
            objectType = AtomPubObjectType(self._cmisClient,
                                           self,
                                           xmlDoc=entryElement)
            types.append(objectType)
        # return the result
        return types

    def getTypeDefinition(self, typeId):

        """
        Returns an :class:`ObjectType` object for the specified object type id.

        >>> folderType = repo.getTypeDefinition('cmis:folder')
        """

        objectType = AtomPubObjectType(self._cmisClient, self, typeId)
        objectType.reload()
        return objectType

    def getLink(self, rel):
        """
        Returns the HREF attribute of an Atom link element for the
        specified rel.
        """
        if self.xmlDoc is None:
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

        return getSpecializedObject(AtomPubCmisObject(self._cmisClient, self, CmisId(objectId), **kwargs), **kwargs)

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

        # get the uritemplate
        template = self.getUriTemplates()['objectbypath']['template']

        # fill in the template with the path provided
        params = {'{path}': quote(path, '/'),
                  '{filter}': '',
                  '{includeAllowableActions}': 'false',
                  '{includePolicyIds}': 'false',
                  '{includeRelationships}': '',
                  '{includeACL}': 'false',
                  '{renditionFilter}': ''}

        options = {}
        addOptions = {}  # args specified, but not in the template
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
        result = self._cmisClient.binding.get(byObjectPathUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **addOptions)

        # instantiate CmisObject objects with the results and return the list
        entryElements = result.getElementsByTagNameNS(ATOM_NS, 'entry')
        assert(len(entryElements) == 1), "Expected entry element in result from calling %s" % byObjectPathUrl
        return getSpecializedObject(AtomPubCmisObject(self._cmisClient, self, xmlDoc=entryElements[0], **kwargs), **kwargs)

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

        if self.xmlDoc is None:
            self.reload()

        # get the URL this repository uses to accept query POSTs
        queryUrl = self.getCollectionLink(QUERY_COLL)

        # build the CMIS query XML that we're going to POST
        xmlDoc = self._getQueryXmlDoc(statement, **kwargs)

        # do the POST
        # print 'posting:%s' % xmlDoc.toxml(encoding='utf-8')
        result = self._cmisClient.binding.post(queryUrl.encode('utf-8'),
                                               self._cmisClient.username,
                                               self._cmisClient.password,
                                               xmlDoc.toxml(encoding='utf-8'),
                                               CMIS_QUERY_TYPE)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self, result)

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

        if self.getCapabilities()['Changes'] is None:
            raise NotSupportedException(messages.NO_CHANGE_LOG_SUPPORT)

        changesUrl = self.getLink(CHANGE_LOG_REL)
        result = self._cmisClient.binding.get(changesUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubChangeEntryResultSet(self._cmisClient, self, result)

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

        # if you didn't pass in a parent folder
        if parentFolder is None:
            # if the repository doesn't require fileable objects to be filed
            if self.getCapabilities()['Unfiling']:
                # has not been implemented
                # postUrl = self.getCollectionLink(UNFILED_COLL)
                raise NotImplementedError
            else:
                # this repo requires fileable objects to be filed
                raise InvalidArgumentException

        return parentFolder.createDocument(name, properties, StringIO.StringIO(contentString),
                                           contentType, contentEncoding)

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

        postUrl = ''
        # if you didn't pass in a parent folder
        if parentFolder is None:
            # if the repository doesn't require fileable objects to be filed
            if self.getCapabilities()['Unfiling']:
                # has not been implemented
                # postUrl = self.getCollectionLink(UNFILED_COLL)
                raise NotImplementedError
            else:
                # this repo requires fileable objects to be filed
                raise InvalidArgumentException
        else:
            postUrl = parentFolder.getChildrenLink()

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
        xmlDoc = getEntryXmlDoc(self, None, properties, contentFile,
                                contentType, contentEncoding)

        # post the Atom entry
        result = self._cmisClient.binding.post(postUrl.encode('utf-8'),
                                               self._cmisClient.username,
                                               self._cmisClient.password,
                                               xmlDoc.toxml(encoding='utf-8'),
                                               ATOM_XML_ENTRY_TYPE)

        # what comes back is the XML for the new document,
        # so use it to instantiate a new document
        # then return it
        return AtomPubDocument(self._cmisClient, self, xmlDoc=result)

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
        # TODO: To be implemented
        raise NotImplementedError

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

        return parentFolder.createFolder(name, properties)

    def createRelationship(self, sourceObj, targetObj, relType):
        """
        Creates a relationship of the specific type between a source object
        and a target object and returns the new :class:`Relationship` object.

        The following optional arguments are not currently supported:
         - policies
         - addACEs
         - removeACEs
        """
        return sourceObj.createRelationship(targetObj, relType)

    def createPolicy(self, properties):
        """
        This has not yet been implemented.

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

            if self.xmlDoc is None:
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

        if collectionType == QUERY_COLL:
            raise NotSupportedException
        elif collectionType == TYPES_COLL:
            return self.getTypeDefinitions()

        result = self._cmisClient.binding.get(self.getCollectionLink(collectionType).encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self, result)

    def getCollectionLink(self, collectionType):

        """
        Returns the link HREF from the specified collectionType
        ('checkedout', for example).

        >>> from cmislib.atompub.atompub_binding import CHECKED_OUT_COLL
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
        # CMIS-703
        # cdataSection = cmisXmlDoc.createCDATASection(query)
        # statementElement.appendChild(cdataSection)
        textNode = cmisXmlDoc.createTextNode(query)
        statementElement.appendChild(textNode)
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


class AtomPubResultSet(ResultSet):

    """
    Represents a paged result set. In CMIS, this is most often an Atom feed.
    """

    def __init__(self, cmisClient, repository, xmlDoc):
        """ Constructor """
        self._cmisClient = cmisClient
        self._repository = repository
        self._xmlDoc = xmlDoc
        self._results = []
        self.logger = logging.getLogger('cmislib.model.ResultSet')
        self.logger.info('Creating an instance of ResultSet')

    def __iter__(self):
        """ Iterator for the result set """
        return iter(self.getResults())

    def __getitem__(self, index):
        """ Getter for the result set """
        return self.getResults()[index]

    def __len__(self):
        """ Len method for the result set """
        return len(self.getResults())

    def _getLink(self, rel):
        """
        Returns the link found in the feed's XML for the specified rel.
        """
        linkElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'link')

        for linkElement in linkElements:

            if linkElement.attributes.has_key('rel'):
                relAttr = linkElement.attributes['rel'].value

                if relAttr == rel:
                    return linkElement.attributes['href'].value

    def _getPageResults(self, rel):
        """
        Given a specified rel, does a get using that link (if one exists)
        and then converts the resulting XML into a dictionary of
        :class:`CmisObject` objects or its appropriate sub-type.

        The results are kept around to facilitate repeated calls without moving
        the cursor.
        """
        link = self._getLink(rel)
        if link:
            result = self._cmisClient.binding.get(link.encode('utf-8'),
                                                  self._cmisClient.username,
                                                  self._cmisClient.password)

            # return the result
            self._xmlDoc = result
            self._results = []
            return self.getResults()

    def reload(self):

        """
        Re-invokes the self link for the current set of results.

        >>> resultSet = repo.getCollection(CHECKED_OUT_COLL)
        >>> resultSet.reload()

        """

        self.logger.debug('Reload called on result set')
        self._getPageResults(SELF_REL)

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
        if self._results:
            return self._results

        if self._xmlDoc:
            entryElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
            entries = []
            for entryElement in entryElements:
                cmisObject = getSpecializedObject(AtomPubCmisObject(self._cmisClient,
                                                                    self._repository,
                                                                    xmlDoc=entryElement))
                entries.append(cmisObject)

            self._results = entries

        return self._results

    def hasObject(self, objectId):

        """
        Returns True if the specified objectId is found in the list of results,
        otherwise returns False.
        """

        for obj in self.getResults():
            if obj.id == objectId:
                return True
        return False

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

        return self._getPageResults(FIRST_REL)

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

        return self._getPageResults(PREV_REL)

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

        return self._getPageResults(NEXT_REL)

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

        return self._getPageResults(LAST_REL)

    def hasNext(self):

        """
        Returns True if this page contains a next link.

        >>> resultSet.hasNext()
        True
        """

        if self._getLink(NEXT_REL):
            return True
        else:
            return False

    def hasPrev(self):

        """
        Returns True if this page contains a prev link. Not all CMIS providers
        implement prev links consistently.

        >>> resultSet.hasPrev()
        True
        """

        if self._getLink(PREV_REL):
            return True
        else:
            return False

    def hasFirst(self):

        """
        Returns True if this page contains a first link. Not all CMIS providers
        implement first links consistently.

        >>> resultSet.hasFirst()
        True
        """

        if self._getLink(FIRST_REL):
            return True
        else:
            return False

    def hasLast(self):

        """
        Returns True if this page contains a last link. Not all CMIS providers
        implement last links consistently.

        >>> resultSet.hasLast()
        True
        """

        if self._getLink(LAST_REL):
            return True
        else:
            return False


class AtomPubDocument(AtomPubCmisObject):

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

        # get the checkedout collection URL
        checkoutUrl = self._repository.getCollectionLink(CHECKED_OUT_COLL)
        assert len(checkoutUrl) > 0, "Could not determine the checkedout collection url."

        # get this document's object ID
        # build entry XML with it
        properties = {'cmis:objectId': self.getObjectId()}
        entryXmlDoc = getEntryXmlDoc(self._repository, properties=properties)

        # post it to to the checkedout collection URL
        result = self._cmisClient.binding.post(checkoutUrl.encode('utf-8'),
                                               self._cmisClient.username,
                                               self._cmisClient.password,
                                               entryXmlDoc.toxml(encoding='utf-8'),
                                               ATOM_XML_ENTRY_TYPE)

        # now that the doc is checked out, we need to refresh the XML
        # to pick up the prop updates related to a checkout
        self.reload()

        return AtomPubDocument(self._cmisClient, self._repository, xmlDoc=result)

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

        # major = true is supposed to be the default but inmemory 0.9 is throwing an error 500 without it
        if not kwargs.has_key('major'):
            kwargs['major'] = 'true'

        # Add checkin to kwargs and checkinComment, if it exists
        kwargs['checkin'] = 'true'
        kwargs['checkinComment'] = checkinComment

        # Build an empty ATOM entry
        entryXmlDoc = getEmptyXmlDoc()

        # Get the self link
        # Do a PUT of the empty ATOM to the self link
        url = self._getSelfLink()
        result = self._cmisClient.binding.put(url.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              entryXmlDoc.toxml(encoding='utf-8'),
                                              ATOM_XML_TYPE,
                                              **kwargs)

        return AtomPubDocument(self._cmisClient, self._repository, xmlDoc=result)

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

        The optional major and filter arguments are supported.
        """

        latestDoc = self.getLatestVersion(**kwargs)

        return latestDoc.getProperties()

    def getAllVersions(self, **kwargs):

        """
        Returns a :class:`ResultSet` of document objects for the entire
        version history of this object, including any PWC's.

        The optional filter and includeAllowableActions are
        supported.
        """

        # get the version history link
        versionsUrl = self._getLink(VERSION_HISTORY_REL)

        # invoke the URL
        result = self._cmisClient.binding.get(versionsUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self._repository, result)

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

        # TODO: Need to implement the streamId

        contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        # CMIS-701
        if len(contentElements) != 1:
            self.reload()
            contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        assert(len(contentElements) == 1), 'Expected to find exactly one atom:content element.'

        # if the src element exists, follow that
        if contentElements[0].attributes.has_key('src'):
            srcUrl = contentElements[0].attributes['src'].value

            # the cmis client class parses non-error responses
            result, content = Rest().get(srcUrl.encode('utf-8'),
                                         username=self._cmisClient.username,
                                         password=self._cmisClient.password,
                                         **self._cmisClient.extArgs)
            if result['status'] != '200':
                raise CmisException(result['status'])
            return StringIO.StringIO(content)
        else:
            # otherwise, try to return the value of the content element
            if contentElements[0].childNodes:
                return contentElements[0].childNodes[0].data

    def setContentStream(self, contentFile, contentType=None):

        """
        Sets the content stream on this object.

        The following optional arguments are not yet supported:
         - overwriteFlag=None
        """

        # get this object's content stream link
        contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        assert(len(contentElements) == 1), 'Expected to find exactly one atom:content element.'

        # if the src element exists, follow that
        if contentElements[0].attributes.has_key('src'):
            srcUrl = contentElements[0].attributes['src'].value

        # there may be times when this URL is absent, but I'm not sure how to
        # set the content stream when that is the case
        assert srcUrl, 'Unable to determine content stream URL.'

        # need to determine the mime type
        mimetype = contentType
        if not mimetype and hasattr(contentFile, 'name'):
            mimetype, encoding = mimetypes.guess_type(contentFile.name)

        if not mimetype:
            mimetype = 'application/binary'

        # if we have a change token, we must pass it back, per the spec
        args = {}
        if self.properties.has_key('cmis:changeToken') and self.properties['cmis:changeToken'] is not None:
            self.logger.debug('Change token present, adding it to args')
            args = {"changeToken": self.properties['cmis:changeToken']}

        # put the content file
        result = self._cmisClient.binding.put(srcUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              contentFile.read(),
                                              mimetype,
                                              **args)

        # what comes back is the XML for the updated document,
        # which is not required by the spec to be the same document
        # we just updated, so use it to instantiate a new document
        # then return it
        return AtomPubDocument(self._cmisClient, self._repository, xmlDoc=result)

    def deleteContentStream(self):

        """
        Delete's the content stream associated with this object.
        """

        # get this object's content stream link
        contentElements = self.xmlDoc.getElementsByTagNameNS(ATOM_NS, 'content')

        assert(len(contentElements) == 1), 'Expected to find exactly one atom:content element.'

        # if the src element exists, follow that
        if contentElements[0].attributes.has_key('src'):
            srcUrl = contentElements[0].attributes['src'].value

        # there may be times when this URL is absent, but I'm not sure how to
        # delete the content stream when that is the case
        assert srcUrl, 'Unable to determine content stream URL.'

        # if we have a change token, we must pass it back, per the spec
        args = {}
        if self.properties.has_key('cmis:changeToken') and self.properties['cmis:changeToken'] is not None:
            self.logger.debug('Change token present, adding it to args')
            args = {"changeToken": self.properties['cmis:changeToken']}

        # delete the content stream
        self._cmisClient.binding.delete(srcUrl.encode('utf-8'),
                                        self._cmisClient.username,
                                        self._cmisClient.password,
                                        **args)

    checkedOut = property(isCheckedOut)

    def getPaths(self):
        """
        Returns the Document's paths by asking for the parents with the
        includeRelativePathSegment flag set to true, then concats the value
        of cmis:path with the relativePathSegment.
        """
        # get the appropriate 'up' link
        parentUrl = self._getLink(UP_REL)

        if parentUrl is None:
            raise NotSupportedException('Root folder does not support getObjectParents')

        # invoke the URL
        result = self._cmisClient.binding.get(parentUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              filter='cmis:path',
                                              includeRelativePathSegment=True)

        paths = []
        rs = AtomPubResultSet(self._cmisClient, self._repository, result)
        for res in rs:
            path = res.properties['cmis:path']
            relativePathSegment = res.properties['cmisra:relativePathSegment']

            # concat with a slash
            # add it to the list
            paths.append(path + '/' + relativePathSegment)

        return paths


class AtomPubFolder(AtomPubCmisObject):

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
        entryXml = getEntryXmlDoc(self._repository, properties=properties)

        # post the Atom entry
        result = self._cmisClient.binding.post(postUrl.encode('utf-8'),
                                               self._cmisClient.username,
                                               self._cmisClient.password,
                                               entryXml.toxml(encoding='utf-8'),
                                               ATOM_XML_ENTRY_TYPE)

        # what comes back is the XML for the new folder,
        # so use it to instantiate a new folder then return it
        return AtomPubFolder(self._cmisClient, self._repository, xmlDoc=result)

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

        return self._repository.createDocumentFromString(name, properties,
                                                         self, contentString, contentType, contentEncoding)

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

        return self._repository.createDocument(name,
                                               properties,
                                               self,
                                               contentFile,
                                               contentType,
                                               contentEncoding)

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

        # get the appropriate 'down' link
        childrenUrl = self.getChildrenLink()
        # invoke the URL
        result = self._cmisClient.binding.get(childrenUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self._repository, result)

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
        # so strip it off but keep other args
        if url.find("?") >= 0:
            u = list(urlparse(url))
            u[4] = '&'.join([p for p in u[4].split('&') if not p.startswith('depth=')])
            url = urlunparse(u)

        return url

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

        if not self._repository.getCapabilities()['GetDescendants']:
            raise NotSupportedException('This repository does not support getDescendants')

        # default the depth to -1, which is all descendants
        if "depth" not in kwargs:
            kwargs['depth'] = -1

        # get the appropriate 'down' link
        descendantsUrl = self.getDescendantsLink()

        # invoke the URL
        result = self._cmisClient.binding.get(descendantsUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self._repository, result)

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

        # Get the descendants link and do a GET against it
        url = self._getLink(FOLDER_TREE_REL)
        assert url is not None, 'Unable to determine folder tree link'
        result = self._cmisClient.binding.get(url.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

        # return the result set
        return AtomPubResultSet(self._cmisClient, self, result)

    def getParent(self):

        """
        This is not yet implemented.

        The optional filter argument is not yet supported.
        """
        # get the appropriate 'up' link
        parentUrl = self._getLink(UP_REL)
        # invoke the URL
        result = self._cmisClient.binding.get(parentUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password)

        # return the result set
        return AtomPubFolder(self._cmisClient, self._repository, xmlDoc=result)

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

        # Per the spec, the repo must have the GetDescendants capability
        # to support deleteTree
        if not self._repository.getCapabilities()['GetDescendants']:
            raise NotSupportedException('This repository does not support deleteTree')

        # Get the descendants link and do a DELETE against it
        url = self._getLink(DOWN_REL, CMIS_TREE_TYPE_P)
        result = self._cmisClient.binding.delete(url.encode('utf-8'),
                                                 self._cmisClient.username,
                                                 self._cmisClient.password,
                                                 **kwargs)

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

        if not self._repository.getCapabilities()['Multifiling']:
            raise NotSupportedException('This repository does not support multifiling')

        postUrl = self.getChildrenLink()

        # post the Atom entry
        self._cmisClient.binding.post(postUrl.encode('utf-8'),
                                      self._cmisClient.username,
                                      self._cmisClient.password,
                                      cmisObject.xmlDoc.toxml(encoding='utf-8'),
                                      ATOM_XML_ENTRY_TYPE,
                                      **kwargs)

    def removeObject(self, cmisObject):

        """
        Removes the specified object from this folder. The repository must
        support unfiling for this to work.
        """

        if not self._repository.getCapabilities()['Unfiling']:
            raise NotSupportedException('This repository does not support unfiling')

        postUrl = self._repository.getCollectionLink(UNFILED_COLL)

        args = {"removeFrom": self.getObjectId()}

        # post the Atom entry to the unfiled collection
        self._cmisClient.binding.post(postUrl.encode('utf-8'),
                                      self._cmisClient.username,
                                      self._cmisClient.password,
                                      cmisObject.xmlDoc.toxml(encoding='utf-8'),
                                      ATOM_XML_ENTRY_TYPE,
                                      **args)

    def getPaths(self):
        """
        Returns the paths as a list of strings. The spec says folders cannot
        be multi-filed, so this should always be one value. We return a list
        to be symmetric with the same method in :class:`Document`.
        """
        return [self.properties['cmis:path']]


class AtomPubRelationship(AtomPubCmisObject):

    """
    Defines a relationship object between two :class:`CmisObjects` objects
    """

    def getSourceId(self):

        """
        Returns the :class:`CmisId` on the source side of the relationship.
        """

        if self.xmlDoc is None:
            self.reload()
        props = self.getProperties()
        return AtomPubCmisId(props['cmis:sourceId'])

    def getTargetId(self):

        """
        Returns the :class:`CmisId` on the target side of the relationship.
        """

        if self.xmlDoc is None:
            self.reload()
        props = self.getProperties()
        return AtomPubCmisId(props['cmis:targetId'])

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


class AtomPubPolicy(AtomPubCmisObject):

    """
    An arbirary object that can 'applied' to objects that the
    repository identifies as being 'controllable'.
    """

    pass


class AtomPubObjectType(ObjectType):

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
        self.logger = logging.getLogger('cmislib.model.ObjectType')
        self.logger.info('Creating an instance of ObjectType')

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

        if self._typeId is None:
            if self.xmlDoc is None:
                self.reload()
            self._typeId = CmisId(self._getElementValue(CMIS_NS, 'id'))

        return self._typeId

    def _getElementValue(self, namespace, elementName):

        """
        Helper method to retrieve child element values from type XML.
        """

        if self.xmlDoc is None:
            self.reload()
        # typeEls = self.xmlDoc.getElementsByTagNameNS(CMISRA_NS, 'type')
        # assert len(typeEls) == 1, "Expected to find exactly one type element but instead found %d" % len(typeEls)
        # typeEl = typeEls[0]
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
        return AtomPubCmisId(self._getElementValue(CMIS_NS, 'baseId'))

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

        >>> from cmislib.atompub.atompub_binding import ATOM_XML_FEED_TYPE
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

        if self.xmlDoc is None:
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
            prop = AtomPubProperty(typeEl.parentNode)
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
        result = self._cmisClient.binding.get(byTypeIdUrl.encode('utf-8'),
                                              self._cmisClient.username,
                                              self._cmisClient.password,
                                              **kwargs)

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


class AtomPubProperty(Property):

    """
    This class represents an attribute or property definition of an object
    type.
    """

    def __init__(self, propNode):
        """Constructor"""
        self.xmlDoc = propNode
        self.logger = logging.getLogger('cmislib.model.Property')
        self.logger.info('Creating an instance of Property')

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
        return parseBoolValue(self._getElementValue(CMIS_NS, 'updatability'))

    def isInherited(self):
        """Getter for cmis:inherited"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'inherited'))

    def isRequired(self):
        """Getter for cmis:required"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'required'))

    def isQueryable(self):
        """Getter for cmis:queryable"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'queryable'))

    def isOrderable(self):
        """Getter for cmis:orderable"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'orderable'))

    def isOpenChoice(self):
        """Getter for cmis:openChoice"""
        return parseBoolValue(self._getElementValue(CMIS_NS, 'openChoice'))

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


class AtomPubACL(ACL):

    """
    Represents the Access Control List for an object.
    """

    def __init__(self, aceList=None, xmlDoc=None):

        """
        Constructor. Pass in either a dict of :class:`ACE` objects keyed to the
        principalId or the XML representation of the ACL.
        """

        if aceList:
            self._entries = aceList
        else:
            self._entries = None
        if xmlDoc:
            self._xmlDoc = xmlDoc
            self._entries = self._getEntriesFromXml()
        else:
            self._xmlDoc = None

        self.logger = logging.getLogger('cmislib.model.ACL')
        self.logger.info('Creating an instance of ACL')

    def addEntry(self, principalId, access, direct=True):

        """
        Adds an :class:`ACE` entry to the ACL.

        The default for direct is True but you can override it if needed.

        >>> acl = folder.getACL()
        >>> acl.addEntry('jpotts', 'cmis:read')
        >>> acl.addEntry('jsmith', 'cmis:write')
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>, 'jsmith': <cmislib.model.ACE object at 0x1005a2210>}
        """
        ace = AtomPubACE(principalId, access, direct)
        if not self._entries:
            self._entries = {ace.principalId : ace}
        else:
            if self._entries.has_key(principalId):
                if access not in self._entries[principalId].permissions:
                    perms = self._entries[principalId].permissions
                    perms.append(access)
                    self.removeEntry(principalId)
                    if not self._entries:
                        self._entries = {principalId : AtomPubACE(principalId, perms, direct)}
                    else:
                        self._entries[principalId] = AtomPubACE(principalId, perms, direct)
            else:
                self._entries[ace.principalId] = ace

    def removeEntry(self, principalId):

        """
        Removes the :class:`ACE` entry given a specific principalId. If a given
        principalId has more than one permission, calling removeEntry will
        remove the entry completely.

        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>, 'jsmith': <cmislib.model.ACE object at 0x1005a2210>}
        >>> acl.removeEntry('jsmith')
        >>> acl.getEntries()
        {u'GROUP_EVERYONE': <cmislib.model.ACE object at 0x100731410>, u'jdoe': <cmislib.model.ACE object at 0x100731150>, 'jpotts': <cmislib.model.ACE object at 0x1005a22d0>}
        """

        if self._entries.has_key(principalId):
            del self._entries[principalId]
            if len(self._entries) == 0:
                self.clearEntries()

    def clearEntries(self):

        """
        Clears all :class:`ACE` entries from the ACL and removes the internal
        XML representation of the ACL.

        >>> acl = ACL()
        >>> acl.addEntry(ACE('jsmith', 'cmis:write'))
        >>> acl.addEntry(ACE('jpotts', 'cmis:write'))
        >>> acl.entries
        {'jpotts': <cmislib.model.ACE object at 0x1012c7310>, 'jsmith': <cmislib.model.ACE object at 0x100528490>}
        >>> acl.getXmlDoc()
        <xml.dom.minidom.Document instance at 0x1012cbb90>
        >>> acl.clearEntries()
        >>> acl.entries
        >>> acl.getXmlDoc()
        """

        self._entries = None
        self._xmlDoc = None

    def getEntries(self):

        """
        Returns a dictionary of :class:`ACE` objects for each Access Control
        Entry in the ACL. The key value is the ACE principalid.

        >>> acl = ACL()
        >>> acl.addEntry(ACE('jsmith', 'cmis:write'))
        >>> acl.addEntry(ACE('jpotts', 'cmis:write'))
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
                direct = None
                if dirEl and dirEl.childNodes:
                    direct = parseBoolValue(dirEl.childNodes[0].data)
                # create an ACE
                if len(perms) > 0:
                    ace = AtomPubACE(principalId, perms, direct)
                    # append it to the dictionary
                    result[principalId] = ace
        return result

    def getXmlDoc(self):

        """
        This method rebuilds the local XML representation of the ACL based on
        the :class:`ACE` objects in the entries list and returns the resulting
        XML Document.
        """

        xmlDoc = minidom.Document()
        aclEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:acl')
        aclEl.setAttribute('xmlns:cmis', CMIS_NS)
        if self.getEntries():
            for ace in self.getEntries().values():
                # only want direct permissions
                if ace.direct:
                    permEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:permission')
                    # principalId
                    prinEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:principal')
                    prinIdEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:principalId')
                    prinIdElText = xmlDoc.createTextNode(ace.principalId)
                    prinIdEl.appendChild(prinIdElText)
                    prinEl.appendChild(prinIdEl)
                    permEl.appendChild(prinEl)
                    # permissions
                    for perm in ace.permissions:
                        permItemEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:permission')
                        permItemElText = xmlDoc.createTextNode(perm)
                        permItemEl.appendChild(permItemElText)
                        permEl.appendChild(permItemEl)
                    directEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:direct')
                    directElText = xmlDoc.createTextNode(toCMISValue(ace.direct))
                    directEl.appendChild(directElText)
                    permEl.appendChild(directEl)
                    aclEl.appendChild(permEl)
        else:
            permEl = xmlDoc.createElementNS(CMIS_NS, 'cmis:permission')
            aclEl.appendChild(permEl)
        xmlDoc.appendChild(aclEl)
        return xmlDoc

    entries = property(getEntries)


class AtomPubACE(ACE):

    """
    Represents an ACE for the AtomPub binding.
    """

    pass


class AtomPubChangeEntry(ChangeEntry):

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
        self.logger = logging.getLogger('cmislib.model.ChangeEntry')
        self.logger.info('Creating an instance of ChangeEntry')

    def getId(self):
        """
        Returns the unique ID of the change entry.
        """
        if self._changeEntryId is None:
            self._changeEntryId = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'id')[0].firstChild.data
        return self._changeEntryId

    def getObjectId(self):
        """
        Returns the object ID of the object that changed.
        """
        if self._objectId is None:
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

        if self._changeType is None:
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
        if len(aclEls) == 1:
            return AtomPubACL(aceList=aclEls[0])
        elif aclUrl:
            result = self._cmisClient.binding.get(aclUrl.encode('utf-8'),
                                                  self._cmisClient.username,
                                                  self._cmisClient.password)
            return AtomPubACL(xmlDoc=result)

    def getChangeTime(self):

        """
        Returns a datetime object representing the time the change occurred.
        """

        if self._changeTime is None:
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


class AtomPubChangeEntryResultSet(AtomPubResultSet):

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

        if self._results:
            return self._results

        if self._xmlDoc:
            entryElements = self._xmlDoc.getElementsByTagNameNS(ATOM_NS, 'entry')
            entries = []
            for entryElement in entryElements:
                changeEntry = AtomPubChangeEntry(self._cmisClient, self._repository, entryElement)
                entries.append(changeEntry)

            self._results = entries

        return self._results


class AtomPubRendition(Rendition):

    """
    This class represents a Rendition.
    """

    def __init__(self, propNode):
        """Constructor"""
        self.xmlDoc = propNode
        self.logger = logging.getLogger('cmislib.model.Rendition')
        self.logger.info('Creating an instance of Rendition')

    def __str__(self):
        """To string"""
        return self.getStreamId()

    def getStreamId(self):
        """Getter for the rendition's stream ID"""
        if self.xmlDoc.attributes.has_key('streamId'):
            return self.xmlDoc.attributes['streamId'].value

    def getMimeType(self):
        """Getter for the rendition's mime type"""
        if self.xmlDoc.attributes.has_key('type'):
            return self.xmlDoc.attributes['type'].value

    def getLength(self):
        """Getter for the renditions's length"""
        if self.xmlDoc.attributes.has_key('length'):
            return self.xmlDoc.attributes['length'].value

    def getTitle(self):
        """Getter for the renditions's title"""
        if self.xmlDoc.attributes.has_key('title'):
            return self.xmlDoc.attributes['title'].value

    def getKind(self):
        """Getter for the renditions's kind"""
        if self.xmlDoc.hasAttributeNS(CMISRA_NS, 'renditionKind'):
            return self.xmlDoc.getAttributeNS(CMISRA_NS, 'renditionKind')

    def getHeight(self):
        """Getter for the renditions's height"""
        if self.xmlDoc.attributes.has_key('height'):
            return self.xmlDoc.attributes['height'].value

    def getWidth(self):
        """Getter for the renditions's width"""
        if self.xmlDoc.attributes.has_key('width'):
            return self.xmlDoc.attributes['width'].value

    def getHref(self):
        """Getter for the renditions's href"""
        if self.xmlDoc.attributes.has_key('href'):
            return self.xmlDoc.attributes['href'].value

    def getRenditionDocumentId(self):
        """Getter for the renditions's width"""
        if self.xmlDoc.attributes.has_key('renditionDocumentId'):
            return self.xmlDoc.attributes['renditionDocumentId'].value

    streamId = property(getStreamId)
    mimeType = property(getMimeType)
    length = property(getLength)
    title = property(getTitle)
    kind = property(getKind)
    height = property(getHeight)
    width = property(getWidth)
    href = property(getHref)
    renditionDocumentId = property(getRenditionDocumentId)


class AtomPubCmisId(CmisId):

    """
    This is a marker class to be used for Strings that are used as CMIS ID's.
    Making the objects instances of this class makes it easier to create the
    Atom entry XML with the appropriate type, ie, cmis:propertyId, instead of
    cmis:propertyString.
    """

    pass


def getSpecializedObject(obj, **kwargs):

    """
    Returns an instance of the appropriate :class:`CmisObject` class or one
    of its child types depending on the specified baseType.
    """

    moduleLogger.debug('Inside getSpecializedObject')

    if 'cmis:baseTypeId' in obj.getProperties():
        baseType = obj.getProperties()['cmis:baseTypeId']
        if baseType == 'cmis:folder':
            return AtomPubFolder(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)
        if baseType == 'cmis:document':
            return AtomPubDocument(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)
        if baseType == 'cmis:relationship':
            return AtomPubRelationship(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)
        if baseType == 'cmis:policy':
            return AtomPubPolicy(obj._cmisClient, obj._repository, obj.getObjectId(), obj.xmlDoc, **kwargs)

    # if the base type ID wasn't found in the props (this can happen when
    # someone runs a query that doesn't select * or doesn't individually
    # specify baseTypeId) or if the type isn't one of the known base
    # types, give the object back
    return obj


def getEntryXmlDoc(repo=None, objectTypeId=None, properties=None, contentFile=None,
                   contentType=None, contentEncoding=None):

    """
    Internal helper method that knows how to build an Atom entry based
    on the properties and, optionally, the contentFile provided.
    """

    moduleLogger.debug('Inside getEntryXmlDoc')

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
        mediaElement = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:mediatype')
        mediaElementText = entryXmlDoc.createTextNode(mimetype)
        mediaElement.appendChild(mediaElementText)
        base64Element = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:base64')
        base64ElementText = entryXmlDoc.createTextNode(fileData)
        base64Element.appendChild(base64ElementText)
        contentElement = entryXmlDoc.createElementNS(CMISRA_NS, 'cmisra:content')
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

        typeDef = None
        for propName, propValue in properties.items():
            '''
            the name of the element here is significant: it includes the
            data type. I should be able to figure out the right type based
            on the actual type of the object passed in.

            I could do a lookup to the type definition, but that doesn't
            seem worth the performance hit
            '''
            if propValue is None or (type(propValue) == list and propValue[0] is None):
                # grab the prop type from the typeDef
                if typeDef is None:
                    moduleLogger.debug('Looking up type def for: %s', objectTypeId)
                    typeDef = repo.getTypeDefinition(objectTypeId)
                    # TODO what to do if type not found
                propType = typeDef.properties[propName].propertyType
            elif type(propValue) == list:
                propType = type(propValue[0])
            else:
                propType = type(propValue)

            propElementName, propValueStrList = getElementNameAndValues(propType, propName, propValue, type(propValue) == list)

            propElement = entryXmlDoc.createElementNS(CMIS_NS, propElementName)
            propElement.setAttribute('propertyDefinitionId', propName)
            for val in propValueStrList:
                if val is None:
                    continue
                valElement = entryXmlDoc.createElementNS(CMIS_NS, 'cmis:value')
                valText = entryXmlDoc.createTextNode(val)
                valElement.appendChild(valText)
                propElement.appendChild(valElement)
            propsElement.appendChild(propElement)

    return entryXmlDoc


def getElementNameAndValues(propType, propName, propValue, isList=False):

    """
    For a given property type, property name, and property value, this function
    returns the appropriate CMIS Atom entry element name and value list.
    """

    moduleLogger.debug('Inside getElementNameAndValues')
    moduleLogger.debug('propType:%s propName:%s isList:%s', propType, propName, isList)
    if propType == 'id' or propType == CmisId:
        propElementName = 'cmis:propertyId'
        if isList:
            propValueStrList = []
            for val in propValue:
                propValueStrList.append(val)
        else:
            propValueStrList = [propValue]
    elif propType == 'string' or propType == str:
        propElementName = 'cmis:propertyString'
        if isList:
            propValueStrList = []
            for val in propValue:
                propValueStrList.append(val)
        else:
            propValueStrList = [propValue]
    elif propType == 'datetime' or propType == datetime.datetime:
        propElementName = 'cmis:propertyDateTime'
        if isList:
            propValueStrList = []
            for val in propValue:
                if val is not None:
                    propValueStrList.append(val.isoformat())
                else:
                    propValueStrList.append(val)
        else:
            if propValue is not None:
                propValueStrList = [propValue.isoformat()]
            else:
                propValueStrList = [propValue]
    elif propType == 'boolean' or propType == bool:
        propElementName = 'cmis:propertyBoolean'
        if isList:
            propValueStrList = []
            for val in propValue:
                if val is not None:
                    propValueStrList.append(unicode(val).lower())
                else:
                    propValueStrList.append(val)
        else:
            if propValue is not None:
                propValueStrList = [unicode(propValue).lower()]
            else:
                propValueStrList = [propValue]
    elif propType == 'integer' or propType == int:
        propElementName = 'cmis:propertyInteger'
        if isList:
            propValueStrList = []
            for val in propValue:
                if val is not None:
                    propValueStrList.append(unicode(val))
                else:
                    propValueStrList.append(val)
        else:
            if propValue is not None:
                propValueStrList = [unicode(propValue)]
            else:
                propValueStrList = [propValue]
    elif propType == 'decimal' or propType == float:
        propElementName = 'cmis:propertyDecimal'
        if isList:
            propValueStrList = []
            for val in propValue:
                if val is not None:
                    propValueStrList.append(unicode(val))
                else:
                    propValueStrList.append(val)
        else:
            if propValue is not None:
                propValueStrList = [unicode(propValue)]
            else:
                propValueStrList = [propValue]
    else:
        propElementName = 'cmis:propertyString'
        if isList:
            propValueStrList = []
            for val in propValue:
                if val is not None:
                    propValueStrList.append(unicode(val))
                else:
                    propValueStrList.append(val)
        else:
            if propValue is not None:
                propValueStrList = [unicode(propValue)]
            else:
                propValueStrList = [propValue]

    return propElementName, propValueStrList


def getEmptyXmlDoc():

    """
    Internal helper method that knows how to build an empty Atom entry.
    """

    moduleLogger.debug('Inside getEmptyXmlDoc')

    entryXmlDoc = minidom.Document()
    entryElement = entryXmlDoc.createElementNS(ATOM_NS, "entry")
    entryElement.setAttribute('xmlns', ATOM_NS)
    entryXmlDoc.appendChild(entryElement)
    return entryXmlDoc
