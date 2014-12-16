package org.nuxeo.ecm.core.schema.types.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.model.DocumentModelReferenceResolver.NAME;
import static org.nuxeo.ecm.core.model.DocumentModelReferenceResolver.PARAM_STORE;
import static org.nuxeo.ecm.core.model.DocumentModelReferenceResolver.STORE_ID_REF;
import static org.nuxeo.ecm.core.model.DocumentModelReferenceResolver.STORE_PATH_REF;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentModelReferenceResolver;
import org.nuxeo.ecm.core.model.DocumentModelReferenceResolver.MODE;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-document-resolver-contrib.xml" })
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentModelReferenceResolver {

    private static final String ID_XPATH = "dr:docIdRef";

    private static final String PATH_XPATH = "dr:docPathRef";

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentValidationService validator;

    protected DocumentModel doc;

    protected String idRef;

    protected String pathRef;

    @Before
    public void setup() throws Exception {
        doc = session.createDocumentModel("/", "doc1", "DocumentReferencer");
        doc = session.createDocument(doc);
        idRef = doc.getRepositoryName() + ":" + doc.getId();
        pathRef = doc.getRepositoryName() + ":" + doc.getPathAsString();
        session.save();
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetch() {
        new DocumentModelReferenceResolver().fetch("/doc1");
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetchCast() {
        new DocumentModelReferenceResolver().fetch(DocumentModel.class, idRef);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetReference() {
        new DocumentModelReferenceResolver().getReference(doc);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetParameters() {
        new DocumentModelReferenceResolver().getParameters();
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetConstraintErrorMessage() {
        new DocumentModelReferenceResolver().getConstraintErrorMessage(null, Locale.ENGLISH);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleConfigurationTwice() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        dmrr.configure(parameters);
        dmrr.configure(parameters);
    }

    @Test
    public void testConfigurationDefaultIdRef() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertEquals(MODE.ID_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_ID_REF, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testConfigurationIdRef() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertEquals(MODE.ID_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_ID_REF, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testConfigurationPathRef() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertEquals(MODE.PATH_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_PATH_REF, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testName() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertEquals(NAME, dmrr.getName());
    }

    @Test
    public void testValidateGoodIdRefWithDefaultConf() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertTrue(dmrr.validate(idRef));
    }

    @Test
    public void testValidateGoodIdRefWithIdRefMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertTrue(dmrr.validate(idRef));
    }

    @Test
    public void testValidateIdRefFailedWithBadValue() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate("BAD uuid !"));
    }

    @Test
    public void testValidateIdRefFailedWithPathMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertFalse(dmrr.validate(idRef));
    }

    @Test
    public void testValidateGoodPathRefWithDefaultConf() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate(pathRef));
    }

    @Test
    public void testValidateGoodPathRefWithPathMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertTrue(dmrr.validate(pathRef));
    }

    @Test
    public void testValidatePathRedFailedWithBadValue() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate("test:BAD path !"));
    }

    @Test
    public void testValidatePathRedFailedWithBadRepository() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate("badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testValidatePathRefFailedWithIdMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertFalse(dmrr.validate(pathRef));
    }

    @Test
    public void testFetchGoodIdRefWithDefaultConf() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        Object entity = dmrr.fetch(idRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchGoodIdRefWithIdRef() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        Object entity = dmrr.fetch(idRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchIdRefFailedWithBadValue() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch("test:BAD value !"));
    }

    @Test
    public void testFetchIdRefFailedWithBadRepository() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch("badrepo:" + doc.getId()));
    }

    @Test
    public void testFetchIdRefFailedWithPathMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch(idRef));
    }

    @Test
    public void testFetchGoodPathRefWithDefaultConf() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch(pathRef));
    }

    @Test
    public void testFetchGoodPathRefWithPathMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        Object entity = dmrr.fetch(pathRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchPathRefFailedWithBadValue() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch("test:BAD value !"));
    }

    @Test
    public void testFetchPathRefFailedWithBadRepository() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch("badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testFetchPathRefFailedWithIdMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch(pathRef));
    }

    @Test
    public void testFetchCastDocumentModelIdMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        DocumentModel document = dmrr.fetch(DocumentModel.class, idRef);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        assertNull(dmrr.fetch(DocumentModel.class, pathRef));
        assertNull(dmrr.fetch(DocumentModel.class, "test:uuid1234567890"));
        assertNull(dmrr.fetch(DocumentModel.class, "badrepo:" + doc.getId()));
    }

    @Test
    public void testFetchCastAdapterIdMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        VersioningDocument document = dmrr.fetch(VersioningDocument.class, idRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());
    }

    @Test
    public void testFetchCastDocumentModelPathMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        DocumentModel document = dmrr.fetch(DocumentModel.class, pathRef);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        assertNull(dmrr.fetch(DocumentModel.class, idRef));
        assertNull(dmrr.fetch(DocumentModel.class, "test:/doc/toto"));
        assertNull(dmrr.fetch(DocumentModel.class, "badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testFetchCastAdapterPathMode() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        VersioningDocument document = dmrr.fetch(VersioningDocument.class, pathRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());
    }

    @Test
    public void testFetchCastDoesntSupportDocumentType() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch(Document.class, idRef));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch(List.class, idRef));
    }

    @Test
    public void testGetReferenceIdRef() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertEquals(idRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceGroup() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertEquals(pathRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceInvalid() {
        DocumentModelReferenceResolver dmrr = new DocumentModelReferenceResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        DocumentModelReferenceResolver idResolver = (DocumentModelReferenceResolver) ((SimpleType) doc.getProperty(
                ID_XPATH).getType()).getResolver();
        assertEquals(MODE.ID_REF, idResolver.getMode());
        assertEquals(STORE_ID_REF, idResolver.getParameters().get(PARAM_STORE));
        DocumentModelReferenceResolver pathResolver = (DocumentModelReferenceResolver) ((SimpleType) doc.getProperty(
                PATH_XPATH).getType()).getResolver();
        assertEquals(MODE.PATH_REF, pathResolver.getMode());
        assertEquals(STORE_PATH_REF, pathResolver.getParameters().get(PARAM_STORE));
    }

    @Test
    public void testNullValueReturnNullDocument() {
        assertNull(doc.getProperty(ID_XPATH).getReferencedEntity());
        assertNull(doc.getProperty(ID_XPATH).getValue(NuxeoPrincipal.class));
        assertNull(doc.getProperty(PATH_XPATH).getReferencedEntity());
        assertNull(doc.getProperty(PATH_XPATH).getValue(NuxeoPrincipal.class));
    }

    @Test
    public void testBadValuesValidationFailed() {
        doc.setPropertyValue(ID_XPATH, "BAD id !");
        assertNull(doc.getProperty(ID_XPATH).getReferencedEntity());
        doc.setPropertyValue(PATH_XPATH, "BAD path !");
        assertNull(doc.getProperty(PATH_XPATH).getReferencedEntity());
        assertEquals(2, validator.validate(doc).size());
    }

    @Test
    public void testIdRefCorrectValues() {
        doc.setPropertyValue(ID_XPATH, idRef);
        DocumentModel document = (DocumentModel) doc.getProperty(ID_XPATH).getReferencedEntity();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getProperty(ID_XPATH).getValue(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
    }

    @Test
    public void testIdRefDoesntSupportPath() {
        doc.setPropertyValue(ID_XPATH, pathRef);
        assertNull(doc.getProperty(ID_XPATH).getReferencedEntity());
    }

    @Test
    public void testPathRefCorrectValues() {
        doc.setPropertyValue(PATH_XPATH, pathRef);
        DocumentModel document = (DocumentModel) doc.getProperty(PATH_XPATH).getReferencedEntity();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getProperty(PATH_XPATH).getValue(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
    }

    @Test
    public void testPathRefFieldDoesntSupportId() {
        doc.setPropertyValue(PATH_XPATH, idRef);
        assertNull(doc.getProperty(PATH_XPATH).getReferencedEntity());
    }

    @Test
    public void testTranslation() {
        DocumentModelReferenceResolver iddmrr = new DocumentModelReferenceResolver();
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put(PARAM_STORE, STORE_ID_REF);
        iddmrr.configure(userParams);
        checkMessage(iddmrr);
        DocumentModelReferenceResolver pathdmrr = new DocumentModelReferenceResolver();
        Map<String, String> groupParams = new HashMap<String, String>();
        groupParams.put(PARAM_STORE, STORE_PATH_REF);
        pathdmrr.configure(groupParams);
        checkMessage(pathdmrr);

    }

    private void checkMessage(DocumentModelReferenceResolver dmrr) {
        for (Locale locale : Arrays.asList(Locale.FRENCH, Locale.ENGLISH)) {
            String message = dmrr.getConstraintErrorMessage("abc123", locale);
            assertNotNull(message);
            assertFalse(message.trim().isEmpty());
            System.out.println(message);
        }
    }

}
