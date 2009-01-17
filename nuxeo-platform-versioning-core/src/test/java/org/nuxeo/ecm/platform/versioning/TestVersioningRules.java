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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest.RequestSource;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * Test cases for versioning component. Tests the versions are incremented
 * accordingly to defined rules.
 * <p>
 * Document (JCRDocument) objects are used in this test (at the core level).
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestVersioningRules extends VersioningBaseTestCase {

    private static final Log log = LogFactory.getLog(TestVersioningRules.class);

    private long getMajorVersion(DocumentModel doc) throws Exception {
        VersioningService service = getVersioningService();
        String propertyName = service.getMajorVersionPropertyName(doc.getType());
        return (Long) doc.getProperty(
                DocumentModelUtils.getSchemaName(propertyName),
                DocumentModelUtils.getFieldName(propertyName));
    }

    private long getMinorVersion(DocumentModel doc) throws Exception {
        VersioningService service = getVersioningService();
        String propertyName = service.getMinorVersionPropertyName(doc.getType());
        return (Long) doc.getProperty(
                DocumentModelUtils.getSchemaName(propertyName),
                DocumentModelUtils.getFieldName(propertyName));
    }

    private void setMajorVersion(DocumentModel doc, Long version)
            throws Exception {
        VersioningService service = getVersioningService();
        String propertyName = service.getMajorVersionPropertyName(doc.getType());
        doc.setProperty(DocumentModelUtils.getSchemaName(propertyName),
                DocumentModelUtils.getFieldName(propertyName), version);
    }

    private void setMinorVersion(DocumentModel doc, Long version)
            throws Exception {
        VersioningService service = getVersioningService();
        String propertyName = service.getMinorVersionPropertyName(doc.getType());
        doc.setProperty(DocumentModelUtils.getSchemaName(propertyName),
                DocumentModelUtils.getFieldName(propertyName), version);
    }

    public void testVersionEditRequest() throws Exception {
        Document folder1 = root.addChild("testfolder1", "VerFile");
        session.save();
        DocumentRef docRef = new IdRef(folder1.getUUID());
        DocumentModel doc = coreSession.getDocument(docRef);

        setMajorVersion(doc, 9L);
        setMinorVersion(doc, 92L);

        // request a doc version change
        final VersionChangeRequest req = new BasicImplVersionChangeRequest(
                VersionChangeRequest.RequestSource.EDIT, doc,
                VersioningActions.ACTION_INCREMENT_MAJOR);

        final VersioningService service = getVersioningService();

        service.incrementVersions(req);

        assertEquals(10L, getMajorVersion(doc));
        assertEquals(0L, getMinorVersion(doc));
    }

    public void testVersionAutoRequest() throws Exception {
        Document verfile = root.addChild("testfile1", "VerFile");
        session.save();
        DocumentRef docRef = new IdRef(verfile.getUUID());
        DocumentModel doc = coreSession.getDocument(docRef);

        // request a doc version change
        final VersionChangeRequest req = new BasicImplVersionChangeRequest(
                VersionChangeRequest.RequestSource.AUTO, doc,
                VersioningActions.ACTION_INCREMENT_MINOR);

        final VersioningService service = getVersioningService();

        service.incrementVersions(req);

        assertEquals(0L, getMajorVersion(doc));
        assertEquals(1L, getMinorVersion(doc));
    }

    public void testDefinedRuleAuto() throws DocumentException,
            LifeCycleException, ClientException {
        Document verfile = root.addChild("testfile", "VerFile");
        String stateName = "project";
        verfile.setCurrentLifeCycleState(stateName);
        session.save();

        DocumentRef docRef = new IdRef(verfile.getUUID());
        DocumentModel doc = coreSession.getDocument(docRef);

        // request a doc version change
        final VersionChangeRequest req = new NoDefaultVersioningActionRequest(
                VersionChangeRequest.RequestSource.AUTO, doc);

        final VersioningService service = getVersioningService();

        checkVersion(doc, null, null);

        service.incrementVersions(req);

        checkVersion(doc, 0L, 1L);

        doc.followTransition("review");

        service.incrementVersions(req);

        checkVersion(doc, 0L, 2L);

        doc.followTransition("back_to_project");

        service.incrementVersions(req);

        checkVersion(doc, 0L, 3L);

        doc.followTransition("review");

        doc.followTransition("approve");

        service.incrementVersions(req);

        checkVersion(doc, 1L, 0L);
    }

    /**
     * Tests edit option (inc major/minor) with lifecycle transition specified
     * by major inc option.
     *
     * @throws DocumentException
     * @throws ClientException
     * @throws LifeCycleException
     */
    public void testEditOptionWithTransition() throws DocumentException,
            ClientException, LifeCycleException {
        Document verfile = root.addChild("testfile", "VerFile");
        // init the doc lifecycle state
        String stateName = "project";
        verfile.setCurrentLifeCycleState(stateName);
        session.save();

        DocumentRef docRef = new IdRef(verfile.getUUID());
        DocumentModel doc = coreSession.getDocument(docRef);

        assertEquals("project", doc.getCurrentLifeCycleState());

        doc.followTransition("review");

        assertEquals("review", doc.getCurrentLifeCycleState());

        doc.followTransition("approve");

        assertEquals("approved", doc.getCurrentLifeCycleState());

        VersionChangeRequest req = new BasicImplVersionChangeRequest(
                RequestSource.EDIT, doc,
                VersioningActions.ACTION_INCREMENT_MAJOR);

        final VersioningService service = getVersioningService();

        service.incrementVersions(req);

        assertEquals("project", doc.getCurrentLifeCycleState());
    }
}
