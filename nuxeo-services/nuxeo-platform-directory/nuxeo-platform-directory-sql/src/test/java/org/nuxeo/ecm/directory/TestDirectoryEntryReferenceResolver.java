package org.nuxeo.ecm.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.directory.DirectoryEntryReferenceResolver.NAME;
import static org.nuxeo.ecm.directory.DirectoryEntryReferenceResolver.PARAM_DIRECTORY;

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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.directory.sql.SQLDirectoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestDirectoryEntryReferenceResolver extends SQLDirectoryTestCase {

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

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetch() {
        new DirectoryEntryReferenceResolver().fetch(ENTRY_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetchCast() {
        new DirectoryEntryReferenceResolver().fetch(DocumentModel.class, ENTRY_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetReference() {
        new DirectoryEntryReferenceResolver().getReference(entry1);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetParameters() {
        new DirectoryEntryReferenceResolver().getParameters();
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetConstraintErrorMessage() {
        new DirectoryEntryReferenceResolver().getConstraintErrorMessage(null, Locale.ENGLISH);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleConfigurationTwice() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        derr.configure(parameters);
    }

    @Test
    public void testConfigurationDir1() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(REFERENCED_DIRECTORY1, derr.getDirectory().getName());
        Map<String, Serializable> outputParameters = derr.getParameters();
        assertEquals(REFERENCED_DIRECTORY1, outputParameters.get(PARAM_DIRECTORY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigurationMissingDirectory() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        derr.configure(new HashMap<String, String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigurationNonExistingDirectory() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, "aBadDirectoryName");
        derr.configure(parameters);
    }

    @Test
    public void testName() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(NAME, derr.getName());
    }

    @Test
    public void testValidateGoodDir1Ref() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertTrue(derr.validate(ENTRY_ID));
    }

    @Test
    public void testValidateDir1RefFailedWithBadValue() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertFalse(derr.validate("BAD id !"));
    }

    @Test
    public void testFetchGoodDir1Ref() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        Object entity = derr.fetch(ENTRY_ID);
        assertTrue(entity instanceof DocumentModel);
        assertEquals(ENTRY_LABEL, ((DocumentModel) entity).getPropertyValue("drs:label"));
    }

    @Test
    public void testFetchDir1RefFailedWithBadValue() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch("BAD id !"));
    }

    @Test
    public void testFetchCastDocumentModel() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
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
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch(Reference.class, ENTRY_ID));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.fetch(List.class, ENTRY_ID));
    }

    @Test
    public void testGetReferenceDir1Ref() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertEquals(ENTRY_ID, derr.getReference(entry1));
    }

    @Test
    public void testGetReferenceInvalid() {
        DirectoryEntryReferenceResolver derr = new DirectoryEntryReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        derr.configure(parameters);
        assertNull(derr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        DirectoryEntryReferenceResolver idResolver = (DirectoryEntryReferenceResolver) ((SimpleType) doc.getProperty(
                REF1_XPATH).getType()).getResolver();
        assertEquals(REFERENCED_DIRECTORY1, idResolver.getDirectory().getName());
        assertEquals(REFERENCED_DIRECTORY1, idResolver.getParameters().get(PARAM_DIRECTORY));
        DirectoryEntryReferenceResolver pathResolver = (DirectoryEntryReferenceResolver) ((SimpleType) doc.getProperty(
                REF2_XPATH).getType()).getResolver();
        assertEquals(REFERENCED_DIRECTORY2, pathResolver.getDirectory().getName());
        assertEquals(REFERENCED_DIRECTORY2, pathResolver.getParameters().get(PARAM_DIRECTORY));
    }

    @Test
    public void testNullValueReturnNullDocument() {
        assertNull(doc.getProperty(REF1_XPATH).getReferencedEntity());
        assertNull(doc.getProperty(REF1_XPATH).getValue(NuxeoPrincipal.class));
        assertNull(doc.getProperty(REF2_XPATH).getReferencedEntity());
        assertNull(doc.getProperty(REF2_XPATH).getValue(NuxeoPrincipal.class));
    }

    @Test
    public void testBadValuesValidationFailed() {
        doc.setPropertyValue(REF1_XPATH, "BAD id !");
        assertNull(doc.getProperty(REF1_XPATH).getReferencedEntity());
        doc.setPropertyValue(REF2_XPATH, "BAD id !");
        assertNull(doc.getProperty(REF2_XPATH).getReferencedEntity());
        assertEquals(2, validator.validate(doc).size());
    }

    @Test
    public void testRefCorrectValues() {
        doc.setPropertyValue(REF1_XPATH, ENTRY_ID);
        DocumentModel document = (DocumentModel) doc.getProperty(REF1_XPATH).getReferencedEntity();
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
        document = doc.getProperty(REF1_XPATH).getValue(DocumentModel.class);
        assertNotNull(document);
        assertEquals(ENTRY_LABEL, document.getPropertyValue("drs:label"));
    }

    @Test
    public void testTranslation() {
        DirectoryEntryReferenceResolver idderr = new DirectoryEntryReferenceResolver();
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY1);
        idderr.configure(userParams);
        checkMessage(idderr);
        DirectoryEntryReferenceResolver pathderr = new DirectoryEntryReferenceResolver();
        Map<String, String> groupParams = new HashMap<String, String>();
        groupParams.put(PARAM_DIRECTORY, REFERENCED_DIRECTORY2);
        pathderr.configure(groupParams);
        checkMessage(pathderr);

    }

    private void checkMessage(DirectoryEntryReferenceResolver derr) {
        for (Locale locale : Arrays.asList(Locale.FRENCH, Locale.ENGLISH)) {
            String message = derr.getConstraintErrorMessage("abc123", locale);
            assertNotNull(message);
            assertFalse(message.trim().isEmpty());
            System.out.println(message);
        }
    }

}
