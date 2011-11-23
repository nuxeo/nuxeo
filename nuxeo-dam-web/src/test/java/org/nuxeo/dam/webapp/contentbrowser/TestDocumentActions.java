/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.contentbrowser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.dam.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
public class TestDocumentActions {

    private static final Log log = LogFactory.getLog(TestDocumentActions.class);

    @Inject
    protected CoreSession session;

    @Test
    public void testGetTitleCropped() throws Exception {
        String croppedTitle;
        assertNotNull("session is null ?", session);
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        assertNotNull("doc is null", doc);
        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                "Image Name with longish name");
        // now create the doc in the session and save it to the session so that
        // it gets given an id
        doc = session.createDocument(doc);
        session.save();
        // get the document's id for later tests
        String docId = doc.getId();

        DamDocumentActions damDocumentActions = new DamDocumentActions();

        // Now test a variety of croppings including boundary cases
        croppedTitle = damDocumentActions.getTitleCropped(doc, 20);
        assertEquals("Image Nam...ish name", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, "1234");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 5);
        assertEquals("1234", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, "12345");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 4);
        assertEquals("12345", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, "12345");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 5);
        assertEquals("12345", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, "12345");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 6);
        log.warn(croppedTitle);
        assertEquals("12345", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, "123456");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 5);
        assertEquals("1...6", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                "1234567890123456789");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 20);
        assertEquals("1234567890123456789", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                "12345678901234567890");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 20);
        assertEquals("12345678901234567890", croppedTitle);

        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                "123456789012345678901");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 20);
        assertEquals("123456789...45678901", croppedTitle);

        // test odd numbered maxLength
        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY,
                "123456789012345678901");
        croppedTitle = damDocumentActions.getTitleCropped(doc, 19);
        assertEquals("12345678...45678901", croppedTitle);

        // test null or empty title. This should come back with the cropped id.
        // Not really a good test cos it
        // uses same code as function being tested
        doc.setPropertyValue(Constants.DUBLINCORE_TITLE_PROPERTY, null);
        // we will crop to 21 so that the cropped id will contain the first 9
        // and last 9 characters
        String idStart = docId.substring(0, 9);
        String idEnd = docId.substring(docId.length() - 9, docId.length());
        croppedTitle = damDocumentActions.getTitleCropped(doc, 21);
        assertEquals(idStart + "..." + idEnd, croppedTitle);
    }

}
