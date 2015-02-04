package org.nuxeo.ecm.core.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.NAME;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.PARAM_STORE;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.STORE_ID_REF;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.STORE_PATH_REF;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentModelResolver;
import org.nuxeo.ecm.core.model.DocumentModelResolver.MODE;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-document-resolver-contrib.xml" })
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentModelResolver {

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
        doc = session.createDocumentModel("/", "doc1", "TestResolver");
        doc = session.createDocument(doc);
        idRef = doc.getRepositoryName() + ":" + doc.getId();
        pathRef = doc.getRepositoryName() + ":" + doc.getPathAsString();
        session.save();
    }

    @Test
    public void supportedClasses() throws Exception {
        List<Class<?>> classes = new DocumentModelResolver().getManagedClasses();
        assertEquals(1, classes.size());
        assertTrue(classes.contains(DocumentModel.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetch() {
        new DocumentModelResolver().fetch("/doc1");
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationFetchCast() {
        new DocumentModelResolver().fetch(DocumentModel.class, idRef);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetReference() {
        new DocumentModelResolver().getReference(doc);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetParameters() {
        new DocumentModelResolver().getParameters();
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleNoConfigurationGetConstraintErrorMessage() {
        new DocumentModelResolver().getConstraintErrorMessage(null, Locale.ENGLISH);
    }

    @Test(expected = IllegalStateException.class)
    public void testLifecycleConfigurationTwice() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        dmrr.configure(parameters);
        dmrr.configure(parameters);
    }

    @Test
    public void testConfigurationDefaultIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertEquals(MODE.ID_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_ID_REF, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testConfigurationIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertEquals(MODE.ID_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_ID_REF, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testConfigurationPathRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertEquals(MODE.PATH_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_PATH_REF, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testName() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertEquals(NAME, dmrr.getName());
    }

    @Test
    public void testValidateGoodIdRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertTrue(dmrr.validate(idRef));
    }

    @Test
    public void testValidateGoodIdRefWithIdRefMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertTrue(dmrr.validate(idRef));
    }

    @Test
    public void testValidateIdRefFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate("BAD uuid !"));
    }

    @Test
    public void testValidateIdRefFailedWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertFalse(dmrr.validate(idRef));
    }

    @Test
    public void testValidateGoodPathRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate(pathRef));
    }

    @Test
    public void testValidateGoodPathRefWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertTrue(dmrr.validate(pathRef));
    }

    @Test
    public void testValidatePathRedFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate("test:BAD path !"));
    }

    @Test
    public void testValidatePathRedFailedWithBadRepository() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertFalse(dmrr.validate("badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testValidatePathRefFailedWithIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertFalse(dmrr.validate(pathRef));
    }

    @Test
    public void testFetchGoodIdRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        Object entity = dmrr.fetch(idRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchGoodIdRefWithIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        Object entity = dmrr.fetch(idRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchIdRefFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch("test:BAD value !"));
    }

    @Test
    public void testFetchIdRefFailedWithBadRepository() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch("badrepo:" + doc.getId()));
    }

    @Test
    public void testFetchIdRefFailedWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch(idRef));
    }

    @Test
    public void testFetchGoodPathRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch(pathRef));
    }

    @Test
    public void testFetchGoodPathRefWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        Object entity = dmrr.fetch(pathRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchPathRefFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch("test:BAD value !"));
    }

    @Test
    public void testFetchPathRefFailedWithBadRepository() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch("badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testFetchPathRefFailedWithIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch(pathRef));
    }

    @Test
    public void testFetchCastDocumentModelIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
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
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_ID_REF);
        dmrr.configure(parameters);
        VersioningDocument document = dmrr.fetch(VersioningDocument.class, idRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());
    }

    @Test
    public void testFetchCastDocumentModelPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
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
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        VersioningDocument document = dmrr.fetch(VersioningDocument.class, pathRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());
    }

    @Test
    public void testFetchCastDoesntSupportDocumentType() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch(Document.class, idRef));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.fetch(List.class, idRef));
    }

    @Test
    public void testGetReferenceIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertEquals(idRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceGroup() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(PARAM_STORE, STORE_PATH_REF);
        dmrr.configure(parameters);
        assertEquals(pathRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceInvalid() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<String, String>());
        assertNull(dmrr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        DocumentModelResolver idResolver = (DocumentModelResolver) ((SimpleType) doc.getProperty(ID_XPATH).getType()).getObjectResolver();
        assertEquals(MODE.ID_REF, idResolver.getMode());
        assertEquals(STORE_ID_REF, idResolver.getParameters().get(PARAM_STORE));
        DocumentModelResolver pathResolver = (DocumentModelResolver) ((SimpleType) doc.getProperty(PATH_XPATH).getType()).getObjectResolver();
        assertEquals(MODE.PATH_REF, pathResolver.getMode());
        assertEquals(STORE_PATH_REF, pathResolver.getParameters().get(PARAM_STORE));
    }

    @Test
    public void testNullValueReturnNull() {
        assertNull(doc.getObjectResolver(ID_XPATH).fetch());
        assertNull(doc.getObjectResolver(ID_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(ID_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(ID_XPATH).getObjectResolver().fetch(DocumentModel.class));
        assertNull(doc.getObjectResolver(PATH_XPATH).fetch());
        assertNull(doc.getObjectResolver(PATH_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(PATH_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(PATH_XPATH).getObjectResolver().fetch(DocumentModel.class));
    }

    @Test
    public void testBadValuesValidationFailed() {
        doc.setPropertyValue(ID_XPATH, "BAD id !");
        assertNull(doc.getProperty(ID_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(ID_XPATH).getObjectResolver().validate());
        doc.setPropertyValue(PATH_XPATH, "BAD path !");
        assertNull(doc.getProperty(PATH_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(PATH_XPATH).getObjectResolver().validate());
        assertEquals(2, validator.validate(doc).numberOfErrors());
    }

    @Test
    public void testIdRefCorrectValues() {
        doc.setPropertyValue(ID_XPATH, idRef);
        DocumentModel document = (DocumentModel) doc.getProperty(ID_XPATH).getObjectResolver().fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getProperty(ID_XPATH).getObjectResolver().fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = (DocumentModel) doc.getObjectResolver(ID_XPATH).fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getObjectResolver(ID_XPATH).fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
    }

    @Test
    public void testIdRefDoesntSupportPath() {
        doc.setPropertyValue(ID_XPATH, pathRef);
        assertNull(doc.getProperty(ID_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testPathRefCorrectValues() {
        doc.setPropertyValue(PATH_XPATH, pathRef);
        DocumentModel document = (DocumentModel) doc.getProperty(PATH_XPATH).getObjectResolver().fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getProperty(PATH_XPATH).getObjectResolver().fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = (DocumentModel) doc.getObjectResolver(PATH_XPATH).fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getObjectResolver(PATH_XPATH).fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
    }

    @Test
    public void testPathRefFieldDoesntSupportId() {
        doc.setPropertyValue(PATH_XPATH, idRef);
        assertNull(doc.getProperty(PATH_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testTranslation() {
        DocumentModelResolver iddmrr = new DocumentModelResolver();
        Map<String, String> userParams = new HashMap<String, String>();
        userParams.put(PARAM_STORE, STORE_ID_REF);
        iddmrr.configure(userParams);
        checkMessage(iddmrr);
        DocumentModelResolver pathdmrr = new DocumentModelResolver();
        Map<String, String> groupParams = new HashMap<String, String>();
        groupParams.put(PARAM_STORE, STORE_PATH_REF);
        pathdmrr.configure(groupParams);
        checkMessage(pathdmrr);

    }

    private void checkMessage(DocumentModelResolver dmrr) {
        for (Locale locale : Arrays.asList(Locale.FRENCH, Locale.ENGLISH)) {
            String message = dmrr.getConstraintErrorMessage("abc123", locale);
            assertNotNull(message);
            assertFalse(message.trim().isEmpty());
            System.out.println(message);
        }
    }

}
