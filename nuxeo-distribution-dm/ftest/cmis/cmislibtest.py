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
'''
Unit tests for cmislib
'''
import unittest
from cmislib.model import CmisClient, ACE
from cmislib.exceptions import \
                          ObjectNotFoundException, \
                          PermissionDeniedException, \
                          CmisException
from cmislib import messages
import os
from time import sleep, time
import settings


class CmisTestBase(unittest.TestCase):

    """ Common ancestor class for most cmislib unit test classes. """

    def setUp(self):
        """ Create a root test folder for the test. """
        self._cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        self._repo = self._cmisClient.getDefaultRepository()
        self._rootFolder = self._repo.getObjectByPath(settings.TEST_ROOT_PATH)
        self._folderName = " ".join(['cmislib', self.__class__.__name__, str(time())])
        self._testFolder = self._rootFolder.createFolder(self._folderName)

    def tearDown(self):
        """ Clean up after the test. """
        self._testFolder.deleteTree()


class CmisClientTest(unittest.TestCase):

    """ Tests for the :class:`CmisClient` class. """

    def testCmisClient(self):
        '''Instantiate a CmisClient object'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        self.assert_(cmisClient != None)

    def testGetRepositories(self):
        '''Call getRepositories and make sure at least one comes back with
        an ID and a name
        '''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repoInfo = cmisClient.getRepositories()
        self.assert_(len(repoInfo) >= 1)
        self.assert_('repositoryId' in repoInfo[0])
        self.assert_('repositoryName' in repoInfo[0])

    def testDefaultRepository(self):
        '''Get the default repository by calling the repo's service URL'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repo = cmisClient.getDefaultRepository()
        self.assert_(repo != None)
        self.assert_(repo.getRepositoryId() != None)

    def testGetRepository(self):
        '''Get a repository by repository ID'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repo = cmisClient.getDefaultRepository()
        defaultRepoId = repo.getRepositoryId()
        defaultRepoName = repo.getRepositoryName()
        repo = cmisClient.getRepository(defaultRepoId)
        self.assertEquals(defaultRepoId, repo.getRepositoryId())
        self.assertEquals(defaultRepoName, repo.getRepositoryName())

    # Error conditions
    def testCmisClientBadUrl(self):
        '''Try to instantiate a CmisClient object with a known bad URL'''
        cmisClient = CmisClient(settings.REPOSITORY_URL + 'foobar', settings.USERNAME, settings.PASSWORD)
        self.assertRaises(CmisException, cmisClient.getRepositories)

    def testCmisClientBadAuth(self):
        '''Try to instantiate a CmisClient object with bad creds'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, 'BADPASS')
        self.assertRaises(PermissionDeniedException,
                          cmisClient.getRepositories)

    def testGetRepositoryBadId(self):
        '''Try to get a repository with a bad repo ID'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        self.assertRaises(ObjectNotFoundException,
                          cmisClient.getRepository,
                          '123FOO')


class QueryTest(CmisTestBase):

    """ Tests related to running CMIS queries. """

    # TODO: Test the rest of these queries
    #    queryDateRange = "SELECT cmis:name from cmis:document " \
    #                         "where cmis:creationDate >= TIMESTAMP'2009-11-10T00:00:00.000-06:00' and " \
    #                         "cmis:creationDate < TIMESTAMP'2009-11-18T00:00:00.000-06:00'"
    #    queryFolderFullText = "SELECT cmis:name from cmis:document " \
    #                              "where in_folder('workspace://SpacesStore/3935ce21-9f6f-4d46-9e22-4f97e1d5d9d8') " \
    #                              "and contains('contract')"
    #    queryCombined = "SELECT cmis:name from cmis:document " \
    #                        "where in_tree('workspace://SpacesStore/3935ce21-9f6f-4d46-9e22-4f97e1d5d9d8') and " \
    #                        "contains('contract') and cm:description like \"%sign%\""

    def setUp(self):
        """
        Override the base setUp to include creating a couple
        of test docs.
        """
        CmisTestBase.setUp(self)
        # I think this may be an Alfresco bug. The CMIS query results contain
        # 1 less entry element than the number of search results. So this test
        # will create two documents and search for the second one which should
        # work in all repositories.
        testFile = open(settings.TEST_BINARY_2, 'rb')
        self._testContent = self._testFolder.createDocument(testFile.name, contentFile=testFile)
        testFile.close()
        testFile = open(settings.TEST_BINARY_2, 'rb')
        self._testContent2 = self._testFolder.createDocument(settings.TEST_BINARY_2.replace('.', '2.'), contentFile=testFile)
        testFile.close()
        self._maxFullTextTries = settings.MAX_FULL_TEXT_TRIES

    def testSimpleSelect(self):
        '''Execute simple select star from cmis:document'''
        querySimpleSelect = "SELECT * FROM cmis:document"
        resultSet = self._repo.query(querySimpleSelect)
        self.assertTrue(isInResultSet(resultSet, self._testContent))

    def testWildcardPropertyMatch(self):
        '''Find content w/wildcard match on cmis:name property'''
        querySimpleSelect = "SELECT * FROM cmis:document where cmis:name like '" + self._testContent.getProperties()['cmis:name'][:7] + "%'"
        resultSet = self._repo.query(querySimpleSelect)
        self.assertTrue(isInResultSet(resultSet, self._testContent))

    def testPropertyMatch(self):
        '''Find content matching cmis:name property'''
        querySimpleSelect = "SELECT * FROM cmis:document where cmis:name = '" + self._testContent2.getProperties()['cmis:name'] + "'"
        resultSet = self._repo.query(querySimpleSelect)
        self.assertTrue(isInResultSet(resultSet, self._testContent2))

    def testFullText(self):
        '''Find content using a full-text query'''
        queryFullText = "SELECT cmis:objectId, cmis:name FROM cmis:document " \
                        "WHERE contains('whitepaper')"
        # on the first full text search the indexer may need a chance to
        # do its thing
        found = False
        maxTries = self._maxFullTextTries
        while not found and (maxTries > 0):
            resultSet = self._repo.query(queryFullText)
            found = isInResultSet(resultSet, self._testContent2)
            if not found:
                maxTries -= 1
                print 'Not found...sleeping for 10 secs. Remaining tries:%d' % maxTries
                sleep(settings.FULL_TEXT_WAIT)
        self.assertTrue(found)

    def testScore(self):
        '''Find content using FT, sorted by relevance score'''
        queryScore = "SELECT cmis:objectId, cmis:name, Score() as relevance " \
                     "FROM cmis:document WHERE contains('sample') " \
                     "order by relevance DESC"

        # on the first full text search the indexer may need a chance to
        # do its thing
        found = False
        maxTries = self._maxFullTextTries
        while not found and (maxTries > 0):
            resultSet = self._repo.query(queryScore)
            found = isInResultSet(resultSet, self._testContent2)
            if not found:
                maxTries -= 1
                print 'Not found...sleeping for 10 secs. Remaining tries:%d' % maxTries
                sleep(10)
        self.assertTrue(found)


class RepositoryTest(CmisTestBase):

    """ Tests for the :class:`Repository` class. """

    def testRepositoryInfo(self):
        '''Retrieve repository info'''
        repoInfo = self._repo.getRepositoryInfo()
        self.assertTrue('repositoryId' in repoInfo)
        self.assertTrue('repositoryName' in repoInfo)
        self.assertTrue('repositoryDescription' in repoInfo)
        self.assertTrue('vendorName' in repoInfo)
        self.assertTrue('productName' in repoInfo)
        self.assertTrue('productVersion' in repoInfo)
        self.assertTrue('rootFolderId' in repoInfo)
        self.assertTrue('cmisVersionSupported' in repoInfo)

    def testRepositoryCapabilities(self):
        '''Retrieve repository capabilities'''
        caps = self._repo.getCapabilities()
        self.assertTrue('ACL' in caps)
        self.assertTrue('AllVersionsSearchable' in caps)
        self.assertTrue('Changes' in caps)
        self.assertTrue('ContentStreamUpdatability' in caps)
        self.assertTrue('GetDescendants' in caps)
        self.assertTrue('GetFolderTree' in caps)
        self.assertTrue('Multifiling' in caps)
        self.assertTrue('PWCSearchable' in caps)
        self.assertTrue('PWCUpdatable' in caps)
        self.assertTrue('Query' in caps)
        self.assertTrue('Renditions' in caps)
        self.assertTrue('Unfiling' in caps)
        self.assertTrue('VersionSpecificFiling' in caps)
        self.assertTrue('Join' in caps)

    def testGetRootFolder(self):
        '''Get the root folder of the repository'''
        rootFolder = self._repo.getRootFolder()
        self.assert_(rootFolder != None)
        self.assert_(rootFolder.getObjectId() != None)

    def testCreateFolder(self):
        '''Create a new folder in the root folder'''
        folderName = 'testCreateFolder folder'
        newFolder = self._repo.createFolder(self._rootFolder, folderName)
        self.assertEquals(folderName, newFolder.getName())
        newFolder.delete()

    def testCreateDocument(self):
        '''Create a new 'content-less' document'''
        documentName = 'testDocument'
        newDoc = self._repo.createDocument(documentName, parentFolder=self._testFolder)
        self.assertEquals(documentName, newDoc.getName())

    def testGetObject(self):
        '''Create a test folder then attempt to retrieve it as a
        :class:`CmisObject` object using its object ID'''
        folderName = 'testGetObject folder'
        newFolder = self._repo.createFolder(self._testFolder, folderName)
        objectId = newFolder.getObjectId()
        someObject = self._repo.getObject(objectId)
        self.assertEquals(folderName, someObject.getName())
        newFolder.delete()

    def testReturnVersion(self):
        '''Get latest and latestmajor versions of an object'''
        f = open(settings.TEST_BINARY_1, 'rb')
        doc10 = self._testFolder.createDocument(settings.TEST_BINARY_1, contentFile=f)
        doc10Id = doc10.getObjectId()
        pwc = doc10.checkout()
        doc11 = pwc.checkin(major='false') # checkin a minor version, 1.1
        pwc = doc11.checkout()
        doc20 = pwc.checkin() # checkin a major version, 2.0
        doc20Id = doc20.getObjectId()
        pwc = doc20.checkout()
        doc21 = pwc.checkin(major='false') # checkin a minor version, 2.1
        doc21Id = doc21.getObjectId()

        docLatest = self._repo.getObject(doc10Id, returnVersion='latest')
        self.assertEquals(doc21Id, docLatest.getObjectId())

        docLatestMajor = self._repo.getObject(doc10Id, returnVersion='latestmajor')
        self.assertEquals(doc20Id, docLatestMajor.getObjectId())

    def testGetFolder(self):
        '''Create a test folder then attempt to retrieve the Folder object
        using its object ID'''
        folderName = 'testGetFolder folder'
        newFolder = self._repo.createFolder(self._testFolder, folderName)
        objectId = newFolder.getObjectId()
        someFolder = self._repo.getFolder(objectId)
        self.assertEquals(folderName, someFolder.getName())
        newFolder.delete()

    def testGetObjectByPath(self):
        '''Create test objects (one folder, one document) then try to get
        them by path'''
        # names of folders and test docs
        testFolderName = self._testFolder.getName()
        parentFolderName = 'testGetObjectByPath folder'
        subFolderName = 'subfolder'
        docName = 'testdoc'

        # create the folder structure
        parentFolder = self._testFolder.createFolder(parentFolderName)
        subFolder = parentFolder.createFolder(subFolderName)
        searchFolder = self._repo.getObjectByPath(settings.TEST_ROOT_PATH + "/".join([testFolderName, parentFolderName, subFolderName]))
        self.assertEquals(subFolder.getObjectId(), searchFolder.getObjectId())

        # create a test doc
        doc = subFolder.createDocument(docName)
        searchDoc = self._repo.getObjectByPath(settings.TEST_ROOT_PATH + "/".join([testFolderName, parentFolderName, subFolderName, docName]))
        self.assertEquals(doc.getObjectId(), searchDoc.getObjectId())

        # get the subfolder by path, then ask for its children
        subFolder = self._repo.getObjectByPath(settings.TEST_ROOT_PATH + "/".join([testFolderName, parentFolderName, subFolderName]))
        self.assertEquals(len(subFolder.getChildren().getResults()), 1)

    def testGetUnfiledDocs(self):
        '''Tests the repository's unfiled collection'''

        if self._repo.getCapabilities()['Unfiling'] != True:
            print 'Repo does not support unfiling, skipping'
            return

        # create a test folder and test doc
        testFolder = self._testFolder.createFolder('unfile test')
        newDoc = testFolder.createDocument('testdoc')

        # make sure the new doc isn't in the unfiled collection
        self.assertFalse(isInResultSet(self._repo.getUnfiledDocs(), newDoc))

        # delete the test folder and tell it to unfile the testdoc
        objId = newDoc.getObjectId()
        testFolder.deleteTree(unfileObjects='unfile')

        # grab the document by object ID
        newDoc = self._repo.getObject(objId)

        # the doc should now be in the unfiled collection
        self.assertTrue(isInResultSet(self._repo.getUnfiledDocs(), newDoc))
        self.assertEquals('testdoc', newDoc.getTitle())

    #Exceptions

    def testGetObjectBadId(self):
        '''Attempt to get an object using a known bad ID'''
        # this object ID is implementation specific (Alfresco) but is universally
        # bad so it should work for all repositories
        self.assertRaises(ObjectNotFoundException,
                          self._repo.getObject,
                          self._testFolder.getObjectId()[:-5] + 'BADID')

    def testGetObjectBadPath(self):
        '''Attempt to get an object using a known bad path'''
        self.assertRaises(ObjectNotFoundException,
                          self._repo.getObjectByPath,
                          '/123foo/BAR.jtp')


class FolderTest(CmisTestBase):

    """ Tests for the :class:`Folder` class """

    def testGetChildren(self):
        '''Get the children of the test folder'''
        childFolderName1 = 'testchild1'
        childFolderName2 = 'testchild2'
        grandChildFolderName = 'testgrandchild'
        childFolder1 = self._testFolder.createFolder(childFolderName1)
        childFolder2 = self._testFolder.createFolder(childFolderName2)
        grandChild = childFolder2.createFolder(grandChildFolderName)
        resultSet = self._testFolder.getChildren()
        self.assert_(resultSet != None)
        self.assertEquals(2, len(resultSet.getResults()))
        self.assertTrue(isInResultSet(resultSet, childFolder1))
        self.assertTrue(isInResultSet(resultSet, childFolder2))
        self.assertFalse(isInResultSet(resultSet, grandChild))

    def testGetDescendants(self):
        '''Get the descendants of the root folder'''
        childFolderName1 = 'testchild1'
        childFolderName2 = 'testchild2'
        grandChildFolderName1 = 'testgrandchild'
        childFolder1 = self._testFolder.createFolder(childFolderName1)
        childFolder2 = self._testFolder.createFolder(childFolderName2)
        grandChild = childFolder1.createFolder(grandChildFolderName1)

        # test getting descendants with depth=1
        resultSet = self._testFolder.getDescendants(depth=1)
        self.assert_(resultSet != None)
        self.assertEquals(2, len(resultSet.getResults()))
        self.assertTrue(isInResultSet(resultSet, childFolder1))
        self.assertTrue(isInResultSet(resultSet, childFolder2))
        self.assertFalse(isInResultSet(resultSet, grandChild))

        # test getting descendants with depth=2
        resultSet = self._testFolder.getDescendants(depth=2)
        self.assert_(resultSet != None)
        self.assertEquals(3, len(resultSet.getResults()))
        self.assertTrue(isInResultSet(resultSet, childFolder1))
        self.assertTrue(isInResultSet(resultSet, childFolder2))
        self.assertTrue(isInResultSet(resultSet, grandChild))

        # test getting descendants with depth=-1
        resultSet = self._testFolder.getDescendants() #-1 is the default depth
        self.assert_(resultSet != None)
        self.assertEquals(3, len(resultSet.getResults()))
        self.assertTrue(isInResultSet(resultSet, childFolder1))
        self.assertTrue(isInResultSet(resultSet, childFolder2))
        self.assertTrue(isInResultSet(resultSet, grandChild))

    def testGetTree(self):
        '''Get the folder tree of the test folder'''
        childFolderName1 = 'testchild1'
        childFolderName2 = 'testchild2'
        grandChildFolderName1 = 'testgrandchild'
        childFolder1 = self._testFolder.createFolder(childFolderName1)
        childFolder1.createDocument('testdoc1')
        childFolder2 = self._testFolder.createFolder(childFolderName2)
        childFolder2.createDocument('testdoc2')
        grandChild = childFolder1.createFolder(grandChildFolderName1)
        grandChild.createDocument('testdoc3')

        # test getting tree with depth=1
        resultSet = self._testFolder.getTree(depth=1)
        self.assert_(resultSet != None)
        self.assertEquals(2, len(resultSet.getResults()))
        self.assertTrue(isInResultSet(resultSet, childFolder1))
        self.assertTrue(isInResultSet(resultSet, childFolder2))
        self.assertFalse(isInResultSet(resultSet, grandChild))

        # test getting tree with depth=2
        resultSet = self._testFolder.getTree(depth=2)
        self.assert_(resultSet != None)
        self.assertEquals(3, len(resultSet.getResults()))
        self.assertTrue(isInResultSet(resultSet, childFolder1))
        self.assertTrue(isInResultSet(resultSet, childFolder2))
        self.assertTrue(isInResultSet(resultSet, grandChild))

    def testDeleteEmptyFolder(self):
        '''Create a test folder, then delete it'''
        folderName = 'testDeleteEmptyFolder folder'
        testFolder = self._testFolder.createFolder(folderName)
        self.assertEquals(folderName, testFolder.getName())
        newFolder = testFolder.createFolder('testFolder')
        testFolderChildren = testFolder.getChildren()
        self.assertEquals(1, len(testFolderChildren.getResults()))
        newFolder.delete()
        testFolderChildren = testFolder.getChildren()
        self.assertEquals(0, len(testFolderChildren.getResults()))

    def testDeleteNonEmptyFolder(self):
        '''Create a test folder with something in it, then delete it'''
        folderName = 'testDeleteNonEmptyFolder folder'
        testFolder = self._testFolder.createFolder(folderName)
        self.assertEquals(folderName, testFolder.getName())
        newFolder = testFolder.createFolder('testFolder')
        testFolderChildren = testFolder.getChildren()
        self.assertEquals(1, len(testFolderChildren.getResults()))
        newFolder.createDocument('testDoc')
        self.assertEquals(1, len(newFolder.getChildren().getResults()))
        newFolder.deleteTree()
        testFolderChildren = testFolder.getChildren()
        self.assertEquals(0, len(testFolderChildren.getResults()))

    def testGetProperties(self):
        '''Get the root folder, then get its properties'''
        props = self._testFolder.getProperties()
        self.assert_(props != None)
        self.assert_('cmis:objectId' in props)
        self.assert_(props['cmis:objectId'] != None)
        self.assert_('cmis:objectTypeId' in props)
        self.assert_(props['cmis:objectTypeId'] != None)
        self.assert_('cmis:name' in props)
        self.assert_(props['cmis:name'] != None)

    def testPropertyFilter(self):
        '''Test the properties filter'''
        # names of folders and test docs
        testFolderName = self._testFolder.getName()
        parentFolderName = 'testGetObjectByPath folder'
        subFolderName = 'subfolder'

        # create the folder structure
        parentFolder = self._testFolder.createFolder(parentFolderName)
        subFolder = parentFolder.createFolder(subFolderName)

        # Per CMIS-170, CMIS providers are not required to filter the
        # properties returned. So these tests will check only for the presence
        # of the properties asked for, not the absence of properties that
        # should be filtered if the server chooses to do so.

        # test when used with getObjectByPath
        searchFolder = self._repo.getObjectByPath(settings.TEST_ROOT_PATH + \
                        "/".join([testFolderName, parentFolderName, subFolderName]), \
                        filter='cmis:objectId,cmis:objectTypeId,cmis:baseTypeId')
        self.assertEquals(subFolder.getObjectId(), searchFolder.getObjectId())
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectTypeId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:baseTypeId'))

        # test when used with getObjectByPath + reload
        searchFolder = self._repo.getObjectByPath(settings.TEST_ROOT_PATH + \
                        "/".join([testFolderName, parentFolderName, subFolderName]), \
                        filter='cmis:objectId,cmis:objectTypeId,cmis:baseTypeId')
        searchFolder.reload()
        self.assertEquals(subFolder.getObjectId(), searchFolder.getObjectId())
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectTypeId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:baseTypeId'))

        # test when used with getObject
        searchFolder = self._repo.getObject(subFolder.getObjectId(), \
                        filter='cmis:objectId,cmis:objectTypeId,cmis:baseTypeId')
        self.assertEquals(subFolder.getObjectId(), searchFolder.getObjectId())
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectTypeId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:baseTypeId'))

        # test when used with getObject + reload
        searchFolder = self._repo.getObject(subFolder.getObjectId(), \
                        filter='cmis:objectId,cmis:objectTypeId,cmis:baseTypeId')
        searchFolder.reload()
        self.assertEquals(subFolder.getObjectId(), searchFolder.getObjectId())
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectTypeId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:baseTypeId'))

        # test that you can do a reload with a reset filter
        searchFolder.reload(filter='*')
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:objectTypeId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:baseTypeId'))
        self.assertTrue(searchFolder.getProperties().has_key('cmis:name'))

    def testUpdateProperties(self):
        '''Create a test folder, then update its properties'''
        folderName = 'testUpdateProperties folder'
        newFolder = self._testFolder.createFolder(folderName)
        self.assertEquals(folderName, newFolder.getName())
        folderName2 = 'testUpdateProperties folder2'
        props = {'cmis:name': folderName2}
        newFolder.updateProperties(props)
        self.assertEquals(folderName2, newFolder.getName())

    def testSubFolder(self):
        '''Create a test folder, then create a test folder within that.'''
        parentFolder = self._testFolder.createFolder('testSubFolder folder')
        self.assert_('cmis:objectId' in parentFolder.getProperties())
        childFolder = parentFolder.createFolder('child folder')
        self.assert_('cmis:objectId' in childFolder.getProperties())
        self.assert_(childFolder.getProperties()['cmis:objectId'] != None)

    def testAllowableActions(self):
        '''Create a test folder, then get its allowable actions'''
        actions = self._testFolder.getAllowableActions()
        self.assert_(len(actions) > 0)

    def testGetParent(self):
        '''Get a folder's parent using the getParent call'''
        childFolder = self._testFolder.createFolder('parentTest')
        parentFolder = childFolder.getParent()
        self.assertEquals(self._testFolder.getObjectId(), parentFolder.getObjectId())

    # Exceptions

    def testBadParentFolder(self):
        '''Try to create a folder on a bad/bogus/deleted parent
        folder object'''
        firstFolder = self._testFolder.createFolder('testBadParentFolder folder')
        self.assert_('cmis:objectId' in firstFolder.getProperties())
        firstFolder.delete()
        # folder isn't in the repo anymore, but I still have the object
        # really, this seems like it ought to be an ObjectNotFoundException but
        # not all CMIS providers report it as such
        self.assertRaises(CmisException,
                          firstFolder.createFolder,
                          'bad parent')

# Per CMIS-169, nothing in the spec says that an exception should be thrown
# when a duplicate folder is created, so this test is really not necessary.
#    def testDuplicateFolder(self):
#        '''Try to create a folder that already exists'''
#        folderName = 'testDupFolder folder'
#        firstFolder = self._testFolder.createFolder(folderName)
#        self.assert_('cmis:objectId' in firstFolder.getProperties())
#        # really, this needs to be ContentAlreadyExistsException but
#        # not all CMIS providers report it as such
#        self.assertRaises(CmisException,
#                          self._testFolder.createFolder,
#                          folderName)


class ChangeEntryTest(CmisTestBase):

    """ Tests for the :class:`ChangeEntry` class """

    def testGetContentChanges(self):

        """Get the content changes and inspect Change Entry props"""

        # need to check changes capability
        if not self._repo.capabilities['Changes']:
            print messages.NO_CHANGE_LOG_SUPPORT
            return

        # at least one change should have been made due to the creation of the
        # test documents
        rs = self._repo.getContentChanges()
        self.assertTrue(len(rs) > 0)
        changeEntry = rs[0]
        self.assertTrue(changeEntry.id)
        self.assertTrue(changeEntry.changeType in ['created', 'updated', 'deleted'])
        self.assertTrue(changeEntry.changeTime)

    def testGetACL(self):

        """Gets the ACL that is included with a Change Entry."""

        # need to check changes capability
        if not self._repo.capabilities['Changes']:
            print messages.NO_CHANGE_LOG_SUPPORT
            return

        # need to check ACL capability
        if not self._repo.capabilities['ACL']:
            print messages.NO_ACL_SUPPORT
            return

        # need to test once with includeACL set to true
        rs = self._repo.getContentChanges(includeACL='true')
        self.assertTrue(len(rs) > 0)
        changeEntry = rs[0]
        acl = changeEntry.getACL()
        self.assertTrue(acl)
        for entry in acl.getEntries().values():
            self.assertTrue(entry.principalId)
            self.assertTrue(entry.permissions)

        # need to test once without includeACL set
        rs = self._repo.getContentChanges()
        self.assertTrue(len(rs) > 0)
        changeEntry = rs[0]
        acl = changeEntry.getACL()
        self.assertTrue(acl)
        for entry in acl.getEntries().values():
            self.assertTrue(entry.principalId)
            self.assertTrue(entry.permissions)

    def testGetProperties(self):

        """Gets the properties of an object included with a Change Entry."""

        # need to check changes capability
        changeCap = self._repo.capabilities['Changes']
        if not changeCap:
            print messages.NO_CHANGE_LOG_SUPPORT
            return

        # need to test once without includeProperties set. the objectID should be there
        rs = self._repo.getContentChanges()
        self.assertTrue(len(rs) > 0)
        changeEntry = rs[0]
        self.assertTrue(changeEntry.properties['cmis:objectId'])

        # need to test once with includeProperties set. the objectID should be there plus object props
        if changeCap in ['properties', 'all']:
            rs = self._repo.getContentChanges(includeProperties='true')
            self.assertTrue(len(rs) > 0)
            changeEntry = rs[0]
            self.assertTrue(changeEntry.properties['cmis:objectId'])
            self.assertTrue(changeEntry.properties['cmis:name'])


class DocumentTest(CmisTestBase):

    """ Tests for the :class:`Document` class """

    def testCheckout(self):
        '''Create a document in a test folder, then check it out'''
        newDoc = self._testFolder.createDocument('testDocument')
        pwcDoc = newDoc.checkout()
        try:
            self.assertTrue(newDoc.isCheckedOut())
            self.assert_('cmis:objectId' in newDoc.getProperties())
            self.assert_('cmis:objectId' in pwcDoc.getProperties())
            checkedOutDocs = self._repo.getCollection('checkedout')
            self.assertTrue(isInResultSet(checkedOutDocs, pwcDoc))
        finally:
            pwcDoc.delete()

    def testCheckin(self):
        '''Create a document in a test folder, check it out, then in'''
        testFilename = settings.TEST_BINARY_1
        contentFile = open(testFilename, 'rb')
        testDoc = self._testFolder.createDocument(testFilename, contentFile=contentFile)
        contentFile.close()
        self.assertEquals(testFilename, testDoc.getName())
        pwcDoc = testDoc.checkout()

        try:
            self.assertTrue(testDoc.isCheckedOut())
            self.assert_('cmis:objectId' in testDoc.getProperties())
            self.assert_('cmis:objectId' in pwcDoc.getProperties())
            testDoc = pwcDoc.checkin()
            self.assertFalse(testDoc.isCheckedOut())
        finally:
            if testDoc.isCheckedOut():
                pwcDoc.delete()

    def testCheckinComment(self):
        '''Checkin a document with a comment'''
        testFilename = settings.TEST_BINARY_1
        contentFile = open(testFilename, 'rb')
        testDoc = self._testFolder.createDocument(testFilename, contentFile=contentFile)
        contentFile.close()
        self.assertEquals(testFilename, testDoc.getName())
        pwcDoc = testDoc.checkout()

        try:
            self.assertTrue(testDoc.isCheckedOut())
            testDoc = pwcDoc.checkin(checkinComment='Just a few changes')
            self.assertFalse(testDoc.isCheckedOut())
            self.assertEquals('Just a few changes',
                          testDoc.getProperties()['cmis:checkinComment'])
        finally:
            if testDoc.isCheckedOut():
                pwcDoc.delete()

    def testCheckinAfterGetPWC(self):
        '''Create a document in a test folder, check it out, call getPWC, then checkin'''
        testFilename = settings.TEST_BINARY_1
        contentFile = open(testFilename, 'rb')
        testDoc = self._testFolder.createDocument(testFilename, contentFile=contentFile)
        contentFile.close()
        self.assertEquals(testFilename, testDoc.getName())
        # Alfresco has a bug where if you get the PWC this way
        # the checkin will not be successful
        testDoc.checkout()
        pwcDoc = testDoc.getPrivateWorkingCopy()
        try:
            self.assertTrue(testDoc.isCheckedOut())
            self.assert_('cmis:objectId' in testDoc.getProperties())
            self.assert_('cmis:objectId' in pwcDoc.getProperties())
            testDoc = pwcDoc.checkin()
            self.assertFalse(testDoc.isCheckedOut())
        finally:
            if testDoc.isCheckedOut():
                pwcDoc.delete()

    def testCancelCheckout(self):
        '''Create a document in a test folder, check it out, then cancel
        checkout'''
        newDoc = self._testFolder.createDocument('testDocument')
        pwcDoc = newDoc.checkout()
        try:
            self.assertTrue(newDoc.isCheckedOut())
            self.assert_('cmis:objectId' in newDoc.getProperties())
            self.assert_('cmis:objectId' in pwcDoc.getProperties())
            checkedOutDocs = self._repo.getCollection('checkedout')
            self.assertTrue(isInResultSet(checkedOutDocs, pwcDoc))
        finally:
            pwcDoc.delete()
        self.assertFalse(newDoc.isCheckedOut())
        checkedOutDocs = self._repo.getCollection('checkedout')
        self.assertFalse(isInResultSet(checkedOutDocs, pwcDoc))

    def testDeleteDocument(self):
        '''Create a document in a test folder, then delete it'''
        newDoc = self._testFolder.createDocument('testDocument')
        children = self._testFolder.getChildren()
        self.assertEquals(1, len(children.getResults()))
        newDoc.delete()
        children = self._testFolder.getChildren()
        self.assertEquals(0, len(children.getResults()))

    def testGetLatestVersion(self):
        '''Get latest version of an object'''
        f = open(settings.TEST_BINARY_1, 'rb')
        doc10 = self._testFolder.createDocument(settings.TEST_BINARY_1, contentFile=f)
        pwc = doc10.checkout()
        doc11 = pwc.checkin(major='false') # checkin a minor version, 1.1
        pwc = doc11.checkout()
        doc20 = pwc.checkin() # checkin a major version, 2.0
        doc20Id = doc20.getObjectId()
        pwc = doc20.checkout()
        doc21 = pwc.checkin(major='false') # checkin a minor version, 2.1
        doc21Id = doc21.getObjectId()

        docLatest = doc10.getLatestVersion()
        self.assertEquals(doc21Id, docLatest.getObjectId())

        docLatestMajor = doc10.getLatestVersion(major='true')
        self.assertEquals(doc20Id, docLatestMajor.getObjectId())

    def testGetPropertiesOfLatestVersion(self):
        '''Get properties of latest version of an object'''
        f = open(settings.TEST_BINARY_1, 'rb')
        doc10 = self._testFolder.createDocument(settings.TEST_BINARY_1, contentFile=f)
        pwc = doc10.checkout()
        doc11 = pwc.checkin(major='false') # checkin a minor version, 1.1
        pwc = doc11.checkout()
        doc20 = pwc.checkin() # checkin a major version, 2.0
        doc20Label = doc20.getProperties()['cmis:versionLabel']
        pwc = doc20.checkout()
        doc21 = pwc.checkin(major='false') # checkin a minor version, 2.1
        doc21Label = doc21.getProperties()['cmis:versionLabel']

        propsLatest = doc10.getPropertiesOfLatestVersion()
        self.assertEquals(doc21Label, propsLatest['cmis:versionLabel'])

        propsLatestMajor = doc10.getPropertiesOfLatestVersion(major='true')
        self.assertEquals(doc20Label, propsLatestMajor['cmis:versionLabel'])

    def testGetProperties(self):
        '''Create a document in a test folder, then get its properties'''
        newDoc = self._testFolder.createDocument('testDocument')
        self.assertEquals('testDocument', newDoc.getName())
        self.assertTrue('cmis:objectTypeId' in newDoc.getProperties())
        self.assertTrue('cmis:objectId' in newDoc.getProperties())

    def testAllowableActions(self):
        '''Create document in a test folder, then get its allowable actions'''
        newDoc = self._testFolder.createDocument('testDocument')
        actions = newDoc.getAllowableActions()
        self.assert_(len(actions) > 0)

    def testUpdateProperties(self):
        '''Create a document in a test folder, then update its properties'''
        newDoc = self._testFolder.createDocument('testDocument')
        self.assertEquals('testDocument', newDoc.getName())
        props = {'cmis:name': 'testDocument2', 'cmis:versionLabel': 'foo'}
        newDoc.updateProperties(props)
        self.assertEquals('testDocument2', newDoc.getName())

    def testSetContentStreamPWC(self):
        '''Set the content stream on the PWC'''
        if self._repo.getCapabilities()['ContentStreamUpdatability'] == 'none':
            print 'This repository does not allow content stream updates, skipping'
            return

        testFile1 = settings.TEST_BINARY_1
        testFile1Size = os.path.getsize(testFile1)
        exportFile1 = testFile1.replace('.', 'export.')
        testFile2 = settings.TEST_BINARY_2
        testFile2Size = os.path.getsize(testFile2)
        exportFile2 = testFile1.replace('.', 'export.')

        # create a test document
        contentFile = open(testFile1, 'rb')
        newDoc = self._testFolder.createDocument(testFile1, contentFile=contentFile)
        contentFile.close()

        # export the test document
        result = newDoc.getContentStream()
        outfile = open(exportFile1, 'wb')
        outfile.write(result.read())
        result.close()
        outfile.close()

        # the file we exported should be the same size as the file we
        # originally created
        self.assertEquals(testFile1Size, os.path.getsize(exportFile1))

        # checkout the file
        pwc = newDoc.checkout()

        # update the PWC with a new file
        f = open(testFile2, 'rb')
        pwc.setContentStream(f)
        f.close()

        # checkin the PWC
        newDoc = pwc.checkin()

        # export the checked in document
        result = newDoc.getContentStream()
        outfile = open(exportFile2, 'wb')
        outfile.write(result.read())
        result.close()
        outfile.close()

        # the file we exported should be the same size as the file we
        # checked in after updating the PWC
        self.assertEquals(testFile2Size, os.path.getsize(exportFile2))
        os.remove(exportFile2)

    def testSetContentStreamDoc(self):
        '''Set the content stream on a doc that's not checked out'''
        if self._repo.getCapabilities()['ContentStreamUpdatability'] != 'anytime':
            print 'This repository does not allow content stream updates on the doc, skipping'
            return

        testFile1 = settings.TEST_BINARY_1
        testFile1Size = os.path.getsize(testFile1)
        exportFile1 = testFile1.replace('.', 'export.')
        testFile2 = settings.TEST_BINARY_2
        testFile2Size = os.path.getsize(testFile2)
        exportFile2 = testFile1.replace('.', 'export.')

        # create a test document
        contentFile = open(testFile1, 'rb')
        newDoc = self._testFolder.createDocument(testFile1, contentFile=contentFile)
        contentFile.close()

        # export the test document
        result = newDoc.getContentStream()
        outfile = open(exportFile1, 'wb')
        outfile.write(result.read())
        result.close()
        outfile.close()

        # the file we exported should be the same size as the file we
        # originally created
        self.assertEquals(testFile1Size, os.path.getsize(exportFile1))

        # update the PWC with a new file
        f = open(testFile2, 'rb')
        newDoc.setContentStream(f)
        f.close()

        # export the checked in document
        result = newDoc.getContentStream()
        outfile = open(exportFile2, 'wb')
        outfile.write(result.read())
        result.close()
        outfile.close()

        # the file we exported should be the same size as the file we
        # checked in after updating the PWC
        self.assertEquals(testFile2Size, os.path.getsize(exportFile2))
        os.remove(exportFile2)

    def testDeleteContentStreamPWC(self):
        '''Delete the content stream of a PWC'''
        if self._repo.getCapabilities()['ContentStreamUpdatability'] == 'none':
            print 'This repository does not allow content stream updates, skipping'
            return

        # create a test document
        contentFile = open(settings.TEST_BINARY_1, 'rb')
        newDoc = self._testFolder.createDocument(settings.TEST_BINARY_1, contentFile=contentFile)
        contentFile.close()

        pwc = newDoc.checkout()
        pwc.deleteContentStream()
        self.assertRaises(CmisException, pwc.getContentStream)
        pwc.delete()

    def testCreateDocumentBinary(self):
        '''Create a binary document using a file from the file system'''
        testFilename = settings.TEST_BINARY_1
        contentFile = open(testFilename, 'rb')
        newDoc = self._testFolder.createDocument(testFilename, contentFile=contentFile)
        contentFile.close()
        self.assertEquals(testFilename, newDoc.getName())

        # test to make sure the file we get back is the same length
        # as the file we sent
        result = newDoc.getContentStream()
        exportFilename = testFilename.replace('.', 'export.')
        outfile = open(exportFilename, 'wb')
        outfile.write(result.read())
        result.close()
        outfile.close()
        self.assertEquals(os.path.getsize(testFilename),
                          os.path.getsize(exportFilename))

        # cleanup
        os.remove(exportFilename)

    def testCreateDocumentPlain(self):
        '''Create a plain document using a file from the file system'''
        testFilename = 'plain.txt'
        testFile = open(testFilename, 'w')
        testFile.write('This is a sample text file line 1.\n')
        testFile.write('This is a sample text file line 2.\n')
        testFile.write('This is a sample text file line 3.\n')
        testFile.close()
        contentFile = open(testFilename, 'r')
        newDoc = self._testFolder.createDocument(testFilename, contentFile=contentFile)
        contentFile.close()
        self.assertEquals(testFilename, newDoc.getName())

        # test to make sure the file we get back is the same length as the
        # file we sent
        result = newDoc.getContentStream()
        exportFilename = testFilename.replace('txt', 'export.txt')
        outfile = open(exportFilename, 'w')
        outfile.write(result.read())
        result.close()
        outfile.close()
        self.assertEquals(os.path.getsize(testFilename),
                          os.path.getsize(exportFilename))

        # export
        os.remove(exportFilename)
        os.remove(testFilename)

    def testGetAllVersions(self):
        '''Get all versions of an object'''
        testDoc = self._testFolder.createDocument('testdoc')
        pwc = testDoc.checkout()
        doc = pwc.checkin() # 2.0
        pwc = doc.checkout()
        doc = pwc.checkin() # 3.0
        self.assertEquals('3.0', doc.getProperties()['cmis:versionLabel'])
        rs = doc.getAllVersions()
        self.assertEquals(3, len(rs.getResults()))
#        for count in range(0, 3):
#            if count == 0:
#                self.assertEquals('true',
#                             rs.getResults().values()[count].getProperties()['cmis:isLatestVersion'])
#            else:
#                self.assertEquals('false',
#                             rs.getResults().values()[count].getProperties()['cmis:isLatestVersion'])


class TypeTest(unittest.TestCase):

    """
    Tests for the :class:`ObjectType` class (and related methods in the
    :class:`Repository` class.
    """

    def testTypeDescendants(self):
        '''Get the descendant types of the repository.'''

        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repo = cmisClient.getDefaultRepository()
        typeDefs = repo.getTypeDescendants()
        folderDef = None
        for typeDef in typeDefs:
            if typeDef.getTypeId() == 'cmis:folder':
                folderDef = typeDef
                break
        self.assertTrue(folderDef)
        self.assertTrue(folderDef.baseId)

    def testTypeChildren(self):
        '''Get the child types for this repository and make sure cmis:folder
        is in the list.'''

        #This test would be more interesting if there was a standard way to
        #deploy a custom model. Then we could look for custom types.

        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repo = cmisClient.getDefaultRepository()
        typeDefs = repo.getTypeChildren()
        folderDef = None
        for typeDef in typeDefs:
            if typeDef.getTypeId() == 'cmis:folder':
                folderDef = typeDef
                break
        self.assertTrue(folderDef)
        self.assertTrue(folderDef.baseId)

    def testTypeDefinition(self):
        '''Get the cmis:document type and test a few props of the type.'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repo = cmisClient.getDefaultRepository()
        docTypeDef = repo.getTypeDefinition('cmis:document')
        self.assertEquals('cmis:document', docTypeDef.getTypeId())
        self.assertTrue(docTypeDef.baseId)

    def testTypeProperties(self):
        '''Get the properties for a type.'''
        cmisClient = CmisClient(settings.REPOSITORY_URL, settings.USERNAME, settings.PASSWORD)
        repo = cmisClient.getDefaultRepository()
        docTypeDef = repo.getTypeDefinition('cmis:document')
        self.assertEquals('cmis:document', docTypeDef.getTypeId())
        props = docTypeDef.getProperties().values()
        self.assertTrue(len(props) > 0)
        for prop in props:
            if prop.queryable:
                self.assertTrue(prop.queryName)
            self.assertTrue(prop.propertyType)


class ACLTest(CmisTestBase):

    """
    Tests related to :class:`ACL` and :class:`ACE`
    """

    def testSupportedPermissions(self):
        '''Test the value of supported permissions enum'''
        if not self._repo.getCapabilities()['ACL']:
            print messages.NO_ACL_SUPPORT
            return
        self.assertTrue(self._repo.getSupportedPermissions() in ['basic', 'repository', 'both'])

    def testPermissionDefinitions(self):
        '''Test the list of permission definitions'''
        if not self._repo.getCapabilities()['ACL']:
            print messages.NO_ACL_SUPPORT
            return
        supportedPerms = self._repo.getPermissionDefinitions()
        self.assertTrue(supportedPerms.has_key('cmis:write'))

    def testPermissionMap(self):
        '''Test the permission mapping'''
        if not self._repo.getCapabilities()['ACL']:
            print messages.NO_ACL_SUPPORT
            return
        permMap = self._repo.getPermissionMap()
        self.assertTrue(permMap.has_key('canGetProperties.Object'))
        self.assertTrue(len(permMap['canGetProperties.Object']) > 0)

    def testPropagation(self):
        '''Test the propagation setting'''
        if not self._repo.getCapabilities()['ACL']:
            print messages.NO_ACL_SUPPORT
            return
        self.assertTrue(self._repo.getPropagation() in ['objectonly', 'propagate', 'repositorydetermined'])

    def testGetObjectACL(self):
        '''Test getting an object's ACL'''
        if not self._repo.getCapabilities()['ACL']:
            print messages.NO_ACL_SUPPORT
            return
        acl = self._testFolder.getACL()
        for entry in acl.getEntries().values():
            self.assertTrue(entry.principalId)
            self.assertTrue(entry.permissions)

    def testApplyACL(self):
        '''Test updating an object's ACL'''
        if not self._repo.getCapabilities()['ACL']:
            print messages.NO_ACL_SUPPORT
            return
        if not self._repo.getCapabilities()['ACL'] == 'manage':
            print 'Repository does not support manage ACL'
            return
        if not self._repo.getSupportedPermissions() in ['both', 'basic']:
            print 'Repository needs to support either both or basic permissions for this test'
            return
        acl = self._testFolder.getACL()
        acl.addEntry(ACE('jpotts', 'cmis:write', 'true'))
        acl = self._testFolder.applyACL(acl)
        # would be good to check that the permission we get back is what we set
        # but at least one server (Alf) appears to map the basic perm to a
        # repository-specific perm
        self.assertTrue(acl.getEntries().has_key('jpotts'))


def isInCollection(collection, targetDoc):
    '''
    Util function that searches a list of objects for a matching target
    object.
    '''
    for doc in collection:
        # hacking around a bizarre thing in Alfresco which is that when the
        # PWC comes back it has an object ID of say 123ABC but when you look
        # in the checked out collection the object ID of the PWC is now
        # 123ABC;1.0. What is that ;1.0? I don't know, but object IDs are
        # supposed to be immutable so I'm not sure what's going on there.
        if doc.getObjectId().startswith(targetDoc.getObjectId()):
            return True
    return False


def isInResultSet(resultSet, targetDoc):
    """
    Util function that searches a :class:`ResultSet` for a specified target
    object. Note that this function will do a getNext on every page of the
    result set until it finds what it is looking for or reaches the end of
    the result set. For every item in the result set, the properties
    are retrieved. Long story short: this could be an expensive call.
    """
    done = False
    while not done:
        if resultSet.hasObject(targetDoc.getObjectId()):
            return True
        if resultSet.hasNext():
            resultSet.getNext()
        else:
            done = True

if __name__ == "__main__":
    unittest.main()
