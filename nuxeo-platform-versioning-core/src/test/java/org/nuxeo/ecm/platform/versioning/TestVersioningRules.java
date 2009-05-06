/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.lifecycle.LifeCycleConstants;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest.RequestSource;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * Test cases for versioning component. Tests the versions are incremented
 * accordingly to defined rules.
 *
 * @author Dragos Mihalache
 * @author Florent Guillaume
 */
public class TestVersioningRules extends VersioningBaseTestCase {

    private void setMajorVersion(DocumentModel doc, Long version)
            throws Exception {
        String propertyName = versioningService.getMajorVersionPropertyName(doc.getType());
        doc.setProperty(DocumentModelUtils.getSchemaName(propertyName),
                DocumentModelUtils.getFieldName(propertyName), version);
    }

    private void setMinorVersion(DocumentModel doc, Long version)
            throws Exception {
        String propertyName = versioningService.getMinorVersionPropertyName(doc.getType());
        doc.setProperty(DocumentModelUtils.getSchemaName(propertyName),
                DocumentModelUtils.getFieldName(propertyName), version);
    }

    @SuppressWarnings("boxing")
    public void testVersionEditRequest() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "testfolder1", "VerFile");
        doc = session.createDocument(doc);
        session.save();
        checkVersion(doc, 1, 0);

        setMajorVersion(doc, 9L);
        setMinorVersion(doc, 92L);
        session.saveDocument(doc);

        // request a doc version change
        VersionChangeRequest req = new BasicImplVersionChangeRequest(
                VersionChangeRequest.RequestSource.EDIT, doc,
                VersioningActions.ACTION_INCREMENT_MAJOR);
        versioningService.incrementVersions(req);

        checkVersion(doc, 10, 0);
    }

    public void testVersionAutoRequest() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "testfile1", "VerFile");
        doc = session.createDocument(doc);
        session.save();
        checkVersion(doc, 1, 0);

        // request a doc version change
        VersionChangeRequest req = new BasicImplVersionChangeRequest(
                VersionChangeRequest.RequestSource.AUTO, doc,
                VersioningActions.ACTION_INCREMENT_MINOR);
        versioningService.incrementVersions(req);

        checkVersion(doc, 1, 1);
    }

    public void testDefinedRuleAuto() throws DocumentException,
            LifeCycleException, ClientException {
        DocumentModel doc = new DocumentModelImpl("/", "testfile", "VerFile");
        doc.putContextData(
                LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME,
                "project");
        doc = session.createDocument(doc);
        session.save();
        checkVersion(doc, 1, 0);

        // request a doc version change
        VersionChangeRequest req = new NoDefaultVersioningActionRequest(
                VersionChangeRequest.RequestSource.AUTO, doc);

        versioningService.incrementVersions(req);
        checkVersion(doc, 1, 1);

        doc.followTransition("approve"); // test_auto_approved rule
        versioningService.incrementVersions(req);
        checkVersion(doc, 2, 0);

        doc.followTransition("backToProject");
        versioningService.incrementVersions(req);
        checkVersion(doc, 2, 1);

        doc.followTransition("approve");
        versioningService.incrementVersions(req);
        checkVersion(doc, 3, 0);
    }

    /**
     * Tests edit option (inc major/minor) with lifecycle transition specified
     * by major inc option.
     */
    public void testEditOptionWithTransition() throws DocumentException,
            ClientException, LifeCycleException {
        DocumentModel doc = new DocumentModelImpl("/", "testfile", "VerFile");
        doc.putContextData(
                LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME,
                "project");
        doc = session.createDocument(doc);
        session.save();
        checkVersion(doc, 1, 0);

        doc.followTransition("approve");
        assertEquals("approved", doc.getCurrentLifeCycleState());
        checkVersion(doc, 1, 0);

        VersionChangeRequest req = new BasicImplVersionChangeRequest(
                RequestSource.EDIT, doc,
                VersioningActions.ACTION_INCREMENT_MAJOR);
        versioningService.incrementVersions(req);
        // test_edit_approved_major rule applies
        // backToProject followed automatically
        assertEquals("project", doc.getCurrentLifeCycleState());
        checkVersion(doc, 2, 0);
    }

}
