/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * DocumentModel objects (from the core API level) are used in these tests.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestVersioningDocEdit extends VersioningBaseTestCase {

    private static final Log log = LogFactory.getLog(TestVersioningDocEdit.class);

    public void testVersionDocEditLockedState() throws ClientException {
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel childFile = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        // should fill datamodel
        childFile = coreSession.createDocument(childFile);

        DocumentModel doc = childFile;

        // -- sanity check
        // assertEquals(, arg1)
        DataModel dm = doc.getDataModel(VERSIONING_SCHEMA_NAME);
        assertNotNull(dm);

        log.info("DataModel fields: " + dm.getMap().keySet());

        assertNotNull(dm.getData("major_version"));
        assertNotNull(dm.getData("minor_version"));
        assertEquals(1L, dm.getData("major_version"));
        assertEquals(0L, dm.getData("minor_version"));

        dm = coreSession.getDataModel(doc.getRef(), VERSIONING_SCHEMA_NAME);
        assertNotNull(dm);
        assertNotNull(dm.getData("major_version"));
        assertNotNull(dm.getData("minor_version"));
        assertEquals(1L, dm.getData("major_version"));
        assertEquals(0L, dm.getData("minor_version"));

        assertEquals(1L, doc.getProperty(VERSIONING_SCHEMA_NAME,
                "major_version"));
        assertEquals(0L, doc.getProperty(VERSIONING_SCHEMA_NAME,
                "minor_version"));
        // ------

        // -- set state prop

        // req.setWfStateInitial("assigned");
        // req.setWfStateFinal("inprogress");

        final VersioningService service = getVersioningService();
        checkVersion(doc, 1L, 0L);

        service.incrementMinor(doc);
        checkVersion(doc, 1L, 1L);

        coreSession.saveDocument(doc);
        coreSession.save();
        checkVersion(doc, 1L, 1L);

        service.incrementMajor(doc);
        coreSession.save();
        checkVersion(doc, 2L, 0L);
    }

    /**
     * Will test if the version is incremented in case the DocumentModel env
     * context is added with inc option.
     */
    public void testDocumentSaveWithIncOption() throws ClientException {
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel docModel = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");
        // should fill datamodel
        docModel = coreSession.createDocument(docModel);

        VersioningActions selectedOption = VersioningActions.ACTION_INCREMENT_MINOR;
        docModel.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                selectedOption);
        checkVersion(docModel, 1L, 0L);

        docModel = coreSession.saveDocument(docModel);
        checkVersion(docModel, 1L, 1L);

        selectedOption = VersioningActions.ACTION_INCREMENT_MAJOR;
        docModel.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                selectedOption);
        checkVersion(docModel, 1L, 1L);

        docModel = coreSession.saveDocument(docModel);
        checkVersion(docModel, 2L, 0L);
    }

    /**
     * Will test if the version is incremented in case the DocumentModel env
     * context is added with inc option.
     */
    public void testVersioningChangeListener() throws ClientException {
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel docModel = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");
        // should fill datamodel
        docModel = coreSession.createDocument(docModel);

        VersioningActions selectedOption = VersioningActions.ACTION_INCREMENT_MINOR;
        docModel.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                selectedOption);
        checkVersion(docModel, 1L, 0L);

        docModel = coreSession.saveDocument(docModel);
        DocumentModel readDocModel = coreSession.getDocument(docModel.getRef());
        checkVersion(readDocModel, 1L, 1L);
        checkVersion(docModel, 1L, 1L);

        selectedOption = VersioningActions.ACTION_INCREMENT_MAJOR;
        docModel.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                selectedOption);
        docModel.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        checkVersion(docModel, 1L, 1L);

        coreSession.save();
        VersioningChangeListenerForTesting.setVersionsToCheck(1L, 1L, 2L, 0L);
        docModel = coreSession.saveDocument(docModel);
        DocumentModel docVersion = coreSession.getVersions(docModel.getRef()).get(
                0);
        checkVersion(docVersion, 1L, 1L);
        checkVersion(docModel, 2L, 0L);

        VersioningChangeListenerForTesting vcListener = VersioningChangeListenerForTesting.instance;
        assertNotNull(vcListener);

        checkVersion(vcListener.oldDoc, 1L, 1L);
        checkVersion(vcListener.newDoc, 2L, 0L);
    }

    // FIXME
    public void XXXtestDefinedRules() throws ClientException {
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel childFile = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        // should fill datamodel
        childFile = coreSession.createDocument(childFile);
        DocumentModel doc = childFile;
        checkVersion(doc, 1L, 0L);

        DocumentRef docRef = doc.getRef();
        assertEquals("project", coreSession.getCurrentLifeCycleState(docRef));

        coreSession.followTransition(docRef, "review");
        assertEquals("review", coreSession.getCurrentLifeCycleState(docRef));

        // doc = coreSession.saveDocument(doc);
        // reload document...
        doc = coreSession.getDocument(docRef);
        checkVersion(doc, 1L, 1L);
    }

}
