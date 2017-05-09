/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.directory.DirectoryEntryResolver.NAME;
import static org.nuxeo.ecm.directory.DirectoryEntryResolver.PARAM_DIRECTORY;
import static org.nuxeo.ecm.directory.DirectoryEntryResolver.PARAM_PARENT_FIELD;
import static org.nuxeo.ecm.directory.DirectoryEntryResolver.PARAM_SEPARATOR;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.DirectoryEntryResolver;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.directory.tests:test-directory-resolver-contrib.xml")
public class TestDirectoryEntryResolver {

    private static final String REFERENCED_DIRECTORY2 = "referencedDirectory2";

    private static final String REFERENCED_DIRECTORY1 = "referencedDirectory1";

    private static final String HIERARCHICAL_DIRECTORY = "hierarchicalDirectory";

    private static final String REF1_XPATH = "dr:directory1Ref";

    private static final String REF2_XPATH = "dr:directory2Ref";

    private static final String HIERARCHICAL_REF_XPATH = "dr:hierarchicalDirectoryRef";

    private static final String ENTRY_ID = "123";

    private static final String ENTRY_LABEL = "Label123";

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected DocumentValidationService validator;

    @Inject
    protected DirectoryService directoryService;

    protected DocumentModel doc;

    protected DocumentModel entry1;

    @Before
    public void setup() throws Exception {
        try (Session session1 = directoryService.open(REFERENCED_DIRECTORY1)) {
            entry1 = session1.getEntry(ENTRY_ID);
        }
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
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(REFERENCED_DIRECTORY1, derr.getDirectory().getName());
        Map<String, Serializable> outputParameters = derr.getParameters();
        assertEquals(REFERENCED_DIRECTORY1, outputParameters.get(PARAM_DIRECTORY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigurationMissingDirectory() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        derr.configure(new HashMap<>());
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
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(NAME, derr.getName());
    }

    @Test
    public void testValidateGoodDir1Ref() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertTrue(derr.validate(ENTRY_ID));
    }

    @Test
    public void testValidateDir1RefFailedWithBadValue() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertFalse(derr.validate("BAD id !"));
    }

    @Test
    public void testFetchGoodDir1Ref() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        Object entity = derr.fetch(ENTRY_ID);
        assertTrue(entity instanceof DirectoryEntry);
        assertEquals(ENTRY_LABEL, ((DirectoryEntry) entity).getDocumentModel().getPropertyValue("drs:label"));
    }

    @Test
    public void testFetchDir1RefFailedWithBadValue() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch("BAD id !"));
    }

    @Test
    public void testFetchCastDocumentModel() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<>();
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
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch(Reference.class, ENTRY_ID));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch(List.class, ENTRY_ID));
    }

    @Test
    public void testGetReferenceDir1Ref() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(ENTRY_ID, derr.getReference(entry1));
    }

    @Test
    public void testGetReferenceInvalid() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.getReference("nothing"));
    }

    @Test
    public void testGetHierarchicalReference() {
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, HIERARCHICAL_DIRECTORY);
        parameters.put(PARAM_PARENT_FIELD, "parent");
        parameters.put(PARAM_SEPARATOR, "/");
        derr.configure(parameters);

        try (Session session = directoryService.open(HIERARCHICAL_DIRECTORY)) {
            // root reference returns its id
            DocumentModel root = session.getEntry("level0");
            assertEquals("level0", derr.getReference(root));

            // leaf reference returns complete id chain
            DocumentModel leaf = session.getEntry("level2");
            assertEquals("level0/level1/level2", derr.getReference(leaf));
        }
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
    public void testDocumentHierarchicalRef() {
        // test the resolving
        doc.setPropertyValue(HIERARCHICAL_REF_XPATH, "level0/level1/level2");
        DirectoryEntry level2Entry = (DirectoryEntry) doc.getProperty(HIERARCHICAL_REF_XPATH).getObjectResolver().fetch();
        DocumentModel document = level2Entry.getDocumentModel();
        assertNotNull(document);
        assertEquals("level2", document.getPropertyValue("hd:id"));

        // test the edit
        DocumentModel newDoc = coreSession.createDocumentModel("/", "doc2", "DirectoryReferencer");
        newDoc.getProperty(HIERARCHICAL_REF_XPATH).getObjectResolver().setObject(level2Entry);
        assertEquals("level0/level1/level2", newDoc.getPropertyValue(HIERARCHICAL_REF_XPATH));
    }

    @Test
    public void testTranslation() {
        DirectoryEntryResolver idderr = new DirectoryEntryResolver();
        Map<String, String> userParams = new HashMap<>();
        userParams.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        idderr.configure(userParams);
        checkMessage(idderr);
        DirectoryEntryResolver pathderr = new DirectoryEntryResolver();
        Map<String, String> groupParams = new HashMap<>();
        groupParams.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY2);
        pathderr.configure(groupParams);
        checkMessage(pathderr);

    }

    @Test
    public void testSerialization() throws Exception {
        // create it
        DirectoryEntryResolver derr = new DirectoryEntryResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        // write it
        byte[] buffer = SerializationUtils.serialize(derr);
        // forget the resolver
        derr = null;
        // read it
        Object readObject = SerializationUtils.deserialize(buffer);
        // check it's a dir resolver
        assertTrue(readObject instanceof DirectoryEntryResolver);
        DirectoryEntryResolver readDerr = (DirectoryEntryResolver) readObject;
        // check the configuration
        assertEquals(REFERENCED_DIRECTORY1, readDerr.getDirectory().getName());
        Map<String, Serializable> outputParameters = readDerr.getParameters();
        assertEquals(REFERENCED_DIRECTORY1, outputParameters.get(PARAM_DIRECTORY));
        // test it works: validate
        assertTrue(readDerr.validate(ENTRY_ID));
        // test it works: fetch
        Object entity = readDerr.fetch(ENTRY_ID);
        assertTrue(entity instanceof DirectoryEntry);
        assertEquals(ENTRY_LABEL, ((DirectoryEntry) entity).getDocumentModel().getPropertyValue("drs:label"));
        // test it works: getReference
        assertEquals(ENTRY_ID, readDerr.getReference(entry1));
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
