#!/usr/bin/python

import cmislibtest
import unittest
import sys
import os

VERBOSITY=1
if os.environ.has_key("VERBOSITY"):
    try:
        VERBOSITY=int(os.environ["VERBOSITY"])
    except:
        pass

# These are the tests (from cmislibtest) that are actually run against Nuxeo.
# Please uncomment (remove leading '#') when a new test passes.
TESTS = """
#cmislibtest.ACLTest.testApplyACL
#cmislibtest.ACLTest.testGetObjectACL
#cmislibtest.ACLTest.testPermissionDefinitions
#cmislibtest.ACLTest.testPermissionMap
#cmislibtest.ACLTest.testPropagation
#cmislibtest.ACLTest.testSupportedPermissions

#cmislibtest.ChangeEntryTest.testGetACL
#cmislibtest.ChangeEntryTest.testGetContentChanges
#cmislibtest.ChangeEntryTest.testGetProperties

cmislibtest.CmisClientTest.testCmisClient
cmislibtest.CmisClientTest.testCmisClientBadAuth
cmislibtest.CmisClientTest.testCmisClientBadUrl
cmislibtest.CmisClientTest.testDefaultRepository
cmislibtest.CmisClientTest.testGetRepositories
cmislibtest.CmisClientTest.testGetRepository
cmislibtest.CmisClientTest.testGetRepositoryBadId

#cmislibtest.DocumentTest.testAllowableActions
#cmislibtest.DocumentTest.testCancelCheckout
#cmislibtest.DocumentTest.testCheckin
#cmislibtest.DocumentTest.testCheckinAfterGetPWC
#cmislibtest.DocumentTest.testCheckinComment
#cmislibtest.DocumentTest.testCheckout
cmislibtest.DocumentTest.testCreateDocumentBinary
cmislibtest.DocumentTest.testCreateDocumentPlain
#cmislibtest.DocumentTest.testDeleteContentStreamPWC
cmislibtest.DocumentTest.testDeleteDocument
#cmislibtest.DocumentTest.testGetAllVersions
#cmislibtest.DocumentTest.testGetLatestVersion
cmislibtest.DocumentTest.testGetProperties
#cmislibtest.DocumentTest.testGetPropertiesOfLatestVersion
cmislibtest.DocumentTest.testSetContentStreamDoc
#cmislibtest.DocumentTest.testSetContentStreamPWC
cmislibtest.DocumentTest.testUpdateProperties

#cmislibtest.FolderTest.testAllowableActions
cmislibtest.FolderTest.testBadParentFolder
cmislibtest.FolderTest.testDeleteEmptyFolder
cmislibtest.FolderTest.testDeleteNonEmptyFolder
cmislibtest.FolderTest.testGetChildren
cmislibtest.FolderTest.testGetDescendants
cmislibtest.FolderTest.testGetParent
cmislibtest.FolderTest.testGetProperties
cmislibtest.FolderTest.testGetTree
cmislibtest.FolderTest.testPropertyFilter
cmislibtest.FolderTest.testSubFolder
cmislibtest.FolderTest.testUpdateProperties

cmislibtest.QueryTest.testFullText
cmislibtest.QueryTest.testPropertyMatch
cmislibtest.QueryTest.testScore
cmislibtest.QueryTest.testSimpleSelect
cmislibtest.QueryTest.testWildcardPropertyMatch

cmislibtest.RepositoryTest.testCreateDocument
cmislibtest.RepositoryTest.testCreateFolder
cmislibtest.RepositoryTest.testGetFolder
cmislibtest.RepositoryTest.testGetObject
cmislibtest.RepositoryTest.testGetObjectBadId
cmislibtest.RepositoryTest.testGetObjectBadPath
cmislibtest.RepositoryTest.testGetObjectByPath
cmislibtest.RepositoryTest.testGetRootFolder
cmislibtest.RepositoryTest.testGetUnfiledDocs
cmislibtest.RepositoryTest.testRepositoryCapabilities
cmislibtest.RepositoryTest.testRepositoryInfo
#cmislibtest.RepositoryTest.testReturnVersion

cmislibtest.TypeTest.testTypeChildren
cmislibtest.TypeTest.testTypeDefinition
#cmislibtest.TypeTest.testTypeDescendants
cmislibtest.TypeTest.testTypeProperties
"""

testNames = []
for line in TESTS.split("\n"):
    if not line or line.startswith("#"):
        continue
    testNames.append(line)

suite = unittest.TestLoader().loadTestsFromNames(testNames)

status = unittest.TextTestRunner(verbosity=VERBOSITY).run(suite)

if not status.wasSuccessful():
    sys.exit(1)


