/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.directory.DirectoryEntryResolver.NAME;
import static org.nuxeo.ecm.directory.DirectoryEntryResolver.PARAM_DIRECTORY;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.sql.SQLDirectoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestDirectoryEntryResolver extends SQLDirectoryTestCase {

    private static final String REFERENCED_DIRECTORY2 = "referencedDirectory2";

    private static final String REFERENCED_DIRECTORY1 = "referencedDirectory1";

    private static final String REF1_XPATH = "dr:directory1Ref";

    private static final String REF2_XPATH = "dr:directory2Ref";

    private static final String ENTRY_ID = "123";

    private static final String ENTRY_LABEL = "Label123";

    protected CoreSession coreSession;

    protected DocumentValidationService validator;

    protected DocumentModel doc;

    protected DocumentModel entry1;

    protected DocumentModel entry2;

    @Before
    public void setup() throws Exception {
        deployContrib("org.nuxeo.ecm.directory.sql.tests", "test-directory-resolver-contrib.xml");
        coreSession = Framework.getService(CoreSession.class);
        validator = Framework.getService(DocumentValidationService.class);
        Session session1 = getSession(REFERENCED_DIRECTORY1);
        entry1 = session1.getEntry(ENTRY_ID);
        session1.close();
        Session session2 = getSession(REFERENCED_DIRECTORY2);
        entry2 = session2.getEntry(ENTRY_ID);
        session2.close();
        doc = coreSession.createDocumentModel("/", "doc1", "DirectoryReferencer");
    }

    @Test
    public void supportedClasses() throws Exception {
        List<Class<?>> classes = new DirectoryEntryResolver().getManagedClasses();
        assertEquals(1, classes.size());
        assertTrue(classes.contains(DirectoryEntry.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetch() {
        new DirectoryEntryResolver().fetch(ENTRY_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetchCast() {
        new DirectoryEntryResolver().fetch(DocumentModel.class, ENTRY_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetReference() {
        new DirectoryEntryResolver().getReference(entry1);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetParameters() {
        new DirectoryEntryResolver().getParameters();
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetConstraintErrorMessage() {
        new DirectoryEntryResolver().getConstraintErrorMessage(null, Locale.ENGLISH);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleConfigurationTwice() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        derr.configure(parameters);
    }

    @Test
    public void testConfigurationDir1() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(REFERENCED_DIRECTORY1, derr.getDirectory().getName());
        Map<String, Serializable> outputParameters = derr.getParameters();
        assertEquals(REFERENCED_DIRECTORY1, outputParameters.get(PARAM_DIRECTORY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigurationMissingDirectory() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        derr.configure(new HashMap<String, String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigurationNonExistingDirectory() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, "aBadDirectoryName");
        derr.configure(parameters);
    }

    @Test
    public void testName() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(NAME, derr.getName());
    }

    @Test
    public void testValidateGoodDir1Ref() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertTrue(derr.validate(ENTRY_ID));
    }

    @Test
    public void testValidateDir1RefFailedWithBadValue() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertFalse(derr.validate("BAD id !"));
    }

    @Test
    public void testFetchGoodDir1Ref() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        Object entity = derr.fetch(ENTRY_ID);
        assertTrue(entity instanceof DirectoryEntry);
        assertEquals(ENTRY_LABEL, ((DirectoryEntry) entity).getDocumentModel().getPropertyValue("drs:label"));
    }

    @Test
    public void testFetchDir1RefFailedWithBadValue() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch("BAD id !"));
    }

    @Test
    public void testFetchCastDocumentModel() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        DocumentModel document = derr.fetch(DocumentModel.class, ENTRY_ID);
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
        assertNull(derr.fetch(DocumentModel.class, "toto"));
    }

    @Test
    public void testFetchCastDoesntSupportReferenceType() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch(Reference.class, ENTRY_ID));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch(List.class, ENTRY_ID));
    }

    @Test
    public void testGetReferenceDir1Ref() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(ENTRY_ID, derr.getReference(entry1));
    }

    @Test
    public void testGetReferenceInvalid() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        DirectoryEntryResolver idResolver = (DirectoryEntryResolver) ((SimpleType) doc.getProperty(REF1_XPATH).getType()).getObjectResolver();
        assertEquals(REFERENCED_DIRECTORY1, idResolver.getDirectory().getName());
        assertEquals(REFERENCED_DIRECTORY1, idResolver.getParameters().get(PARAM_DIRECTORY));
        DirectoryEntryResolver pathResolver = (DirectoryEntryResolver) ((SimpleType) doc.getProperty(REF2_XPATH).getType()).getObjectResolver();
        assertEquals(REFERENCED_DIRECTORY2, pathResolver.getDirectory().getName());
        assertEquals(REFERENCED_DIRECTORY2, pathResolver.getParameters().get(PARAM_DIRECTORY));
    }

    @Test
    public void testNullValueReturnNull() {
        assertNull(doc.getObjectResolver(REF1_XPATH).fetch());
        assertNull(doc.getObjectResolver(REF1_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(REF1_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(REF1_XPATH).getObjectResolver().fetch(DocumentModel.class));
        assertNull(doc.getObjectResolver(REF2_XPATH).fetch());
        assertNull(doc.getObjectResolver(REF2_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(REF2_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(REF2_XPATH).getObjectResolver().fetch(DocumentModel.class));
    }

    @Test
    public void testBadValuesValidationFailed() {
        doc.setPropertyValue(REF1_XPATH, "BAD id !");
        assertNull(doc.getProperty(REF1_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(REF1_XPATH).getObjectResolver().validate());
        doc.setPropertyValue(REF2_XPATH, "BAD path !");
        assertNull(doc.getProperty(REF2_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(REF2_XPATH).getObjectResolver().validate());
        assertEquals(2, validator.validate(doc).numberOfErrors());
    }

    @Test
    public void testRefCorrectValues() {
        doc.setPropertyValue(REF1_XPATH, ENTRY_ID);
        DirectoryEntry entry = (DirectoryEntry) doc.getProperty(REF1_XPATH).getObjectResolver().fetch();
        DocumentModel document = entry.getDocumentModel();
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
        entry = (DirectoryEntry) doc.getObjectResolver(REF1_XPATH).fetch();
        document = entry.getDocumentModel();
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
        document = doc.getProperty(REF1_XPATH).getObjectResolver().fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
        document = doc.getObjectResolver(REF1_XPATH).fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
    }

    @Test
    public void testTranslation() {
        DirectoryEntryResolver idderr = new DirectoryEntryResolver();
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        idderr.configure(userParams);
        checkMessage(idderr);
        DirectoryEntryResolver pathderr = new DirectoryEntryResolver();
        Map<String, String> groupParams = new HashMap<String, String>();
        groupParams.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY2);
        pathderr.configure(groupParams);
        checkMessage(pathderr);

    }

    private void checkMessage(DirectoryEntryResolver derr) {
        for (Locale locale : Arrays.asList(Locale.FRENCH, Locale.ENGLISH)) {
            String message = derr.getConstraintErrorMessage("abc123", locale);
            assertNotNull(message);
            assertFalse(message.trim().isEmpty());
            System.out.println(message);
        }
    }

}
