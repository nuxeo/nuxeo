/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests memory leak by repeating operations.
 *
 * @author Florent Guillaume
 */
public class TestSQLRepositoryMemoryLeak extends TestCase {

    private static final Log log = LogFactory.getLog(TestSQLRepositoryMemoryLeak.class);

    public static final int ITERATIONS = 10;

    public void testRepeat() throws Exception {
        for (int i = 1; i <= ITERATIONS; i++) {
            log.warn("\n\n\n\n\n---------------------------------- Iteration " +
                    i);
            doAll();
            System.gc();
            Thread.sleep(1);
            System.gc();
            log.warn("----- End Iteration " + i);
            log.warn("-----   Used mem: " + Runtime.getRuntime().totalMemory());
        }
    }

    public void doOne(String name) {
        log.warn("----- " + name);
        new TestSQLRepositoryAPI(name).run();
        System.gc();
        log.warn("----- used mem: " + Runtime.getRuntime().totalMemory());
    }

    public void doAll() {
        doOne("testBasics");
        doOne("testGetRootDocument");
        doOne("testDocumentReferenceEqualitySameInstance");
        doOne("testCancel");
        doOne("testCreateDomainDocumentRefDocumentModel");
        doOne("testCreateFolderDocumentRefDocumentModel");
        doOne("testCreateFileDocumentRefDocumentModel");
        doOne("testCreateFolderDocumentRefDocumentModelArray");
        doOne("testCreateFileDocumentRefDocumentModelArray");
        doOne("testExists");
        doOne("testGetChild");
        doOne("testGetChildrenDocumentRef");
        doOne("testGetChildrenDocumentRef2");
        doOne("testGetFileChildrenDocumentRefString");
        doOne("testGetFileChildrenDocumentRefString2");
        doOne("testGetFolderChildrenDocumentRefString");
        doOne("testGetFolderChildrenDocumentRefString2");
        doOne("testGetChildrenDocumentRefStringFilter");
        doOne("testGetChildrenDocumentRefStringFilter2");
        doOne("testGetChildrenInFolderWithSearch");
        doOne("testGetDocumentDocumentRef");
        doOne("testGetFilesDocumentRef");
        doOne("testGetFilesDocumentRef2");
        doOne("testGetFoldersDocumentRef");
        doOne("testGetFoldersDocumentRef2");
        doOne("testGetParentDocument");
        doOne("testHasChildren");
        doOne("testRemoveChildren");
        doOne("testRemoveDocument");
        doOne("testRemoveDocuments");
        doOne("testRemoveDocumentsWithDeps");
        doOne("testRemoveDocumentsWithDeps2");
        doOne("testSave");
        doOne("testSaveFolder");
        doOne("testSaveFile");
        doOne("testSaveDocuments");
        doOne("testGetDataModel");
        doOne("testGetDataModelField");
        doOne("testGetDataModelFields");
        doOne("testGetContentData");
        doOne("testDocumentReferenceEqualityDifferentInstances");
        doOne("testDocumentReferenceNonEqualityDifferentInstances");
        doOne("testFacets");
        doOne("testLifeCycleAPI");
        doOne("testDataModelLifeCycleAPI");
        doOne("testCopy");
        doOne("testCopyProxyAsDocument");
        doOne("testCopyVersionable");
        doOne("testCopyFolderOfVersionable");
        doOne("testMove");
        doOne("testBlob");
        doOne("testRetrieveSamePropertyInAncestors");
        doOne("testLock");
        doOne("testGetSourceId");
        doOne("testGetRepositoryName");
        doOne("testCreateDocumentModel");
        doOne("testCopyContent");
        doOne("testDocumentModelTreeSort");
        doOne("testPropertyModel");
        doOne("testPropertyXPath");
        doOne("testComplexList");
        doOne("testDataModel");
        doOne("testGetChildrenRefs");
        doOne("testLazyBlob");
        // doOne("testProxy");
    }

}
