/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.core.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.NAME;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.PARAM_STORE;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.STORE_ID_ONLY;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.STORE_PATH_ONLY;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.STORE_REPO_AND_ID;
import static org.nuxeo.ecm.core.model.DocumentModelResolver.STORE_REPO_AND_PATH;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentModelResolver;
import org.nuxeo.ecm.core.model.DocumentModelResolver.MODE;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-document-resolver-contrib.xml")
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentModelResolver {

    protected static final String REPO_AND_ID_XPATH = "dr:docRepoAndIdRef";

    protected static final String REPO_AND_PATH_XPATH = "dr:docRepoAndPathRef";

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentValidationService validator;

    protected DocumentModel doc;

    protected String repoAndIdRef;

    protected String idOnlyRef;

    protected String repoAndPathRef;

    protected String pathOnlyRef;

    @Before
    public void setup() throws Exception {
        doc = session.createDocumentModel("/", "doc1", "TestResolver");
        doc = session.createDocument(doc);
        repoAndIdRef = doc.getRepositoryName() + ":" + doc.getId();
        idOnlyRef = doc.getId();
        repoAndPathRef = doc.getRepositoryName() + ":" + doc.getPathAsString();
        pathOnlyRef = doc.getPathAsString();
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
        new DocumentModelResolver().fetch(DocumentModel.class, repoAndIdRef);
        new DocumentModelResolver().fetch(DocumentModel.class, idOnlyRef);
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
        Map<String, String> parameters = new HashMap<>();
        dmrr.configure(parameters);
        dmrr.configure(parameters);
    }

    @Test
    public void testConfigurationDefaultIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertEquals(MODE.REPO_AND_ID_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_REPO_AND_ID, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testConfigurationIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);
        assertEquals(MODE.REPO_AND_ID_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_REPO_AND_ID, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testConfigurationPathRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertEquals(MODE.REPO_AND_PATH_REF, dmrr.getMode());
        Map<String, Serializable> outputParameters = dmrr.getParameters();
        assertEquals(STORE_REPO_AND_PATH, outputParameters.get(PARAM_STORE));
    }

    @Test
    public void testName() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertEquals(NAME, dmrr.getName());
    }

    @Test
    public void testValidateGoodIdRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertTrue(dmrr.validate(repoAndIdRef));
        assertTrue(dmrr.validate(idOnlyRef));
    }

    @Test
    public void testValidateGoodIdRefWithIdRefMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);
        assertTrue(dmrr.validate(repoAndIdRef));
        assertTrue(dmrr.validate(idOnlyRef));
    }

    @Test
    public void testValidateIdRefFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertFalse(dmrr.validate("BAD uuid !"));
    }

    @Test
    public void testValidateIdRefFailedWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertFalse(dmrr.validate(repoAndIdRef));
        assertFalse(dmrr.validate(idOnlyRef));
    }

    @Test
    public void testValidateGoodPathRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertFalse(dmrr.validate(repoAndPathRef));
        assertFalse(dmrr.validate(pathOnlyRef));
    }

    @Test
    public void testValidateGoodPathRefWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertTrue(dmrr.validate(repoAndPathRef));
        assertTrue(dmrr.validate(pathOnlyRef));
    }

    @Test
    public void testValidatePathRedFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertFalse(dmrr.validate("test:BAD path !"));
    }

    @Test
    public void testValidatePathRedFailedWithBadRepository() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertFalse(dmrr.validate("badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testValidatePathRefFailedWithIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);
        assertFalse(dmrr.validate(repoAndPathRef));
        assertFalse(dmrr.validate(pathOnlyRef));
    }

    @Test
    public void testFetchGoodIdRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());

        Object entity = dmrr.fetch(repoAndIdRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());

        entity = dmrr.fetch(idOnlyRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchGoodIdRefWithIdRef() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);

        Object entity = dmrr.fetch(repoAndIdRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());

        entity = dmrr.fetch(idOnlyRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchIdRefFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertNull(dmrr.fetch("test:BAD value !"));
    }

    @Test
    public void testFetchIdRefFailedWithBadRepository() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertNull(dmrr.fetch("badrepo:" + doc.getId()));
    }

    @Test
    public void testFetchIdRefFailedWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch(repoAndIdRef));
        assertNull(dmrr.fetch(idOnlyRef));
    }

    @Test
    public void testFetchGoodPathRefWithDefaultConf() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertNull(dmrr.fetch(repoAndPathRef));
        assertNull(dmrr.fetch(pathOnlyRef));
    }

    @Test
    public void testFetchGoodPathRefWithPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);

        Object entity = dmrr.fetch(repoAndPathRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());

        entity = dmrr.fetch(pathOnlyRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals("doc1", ((DocumentModel) entity).getName());
    }

    @Test
    public void testFetchPathRefFailedWithBadValue() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch("test:BAD value !"));
    }

    @Test
    public void testFetchPathRefFailedWithBadRepository() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch("badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testFetchPathRefFailedWithIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);
        assertNull(dmrr.fetch(repoAndPathRef));
        assertNull(dmrr.fetch(pathOnlyRef));
    }

    @Test
    public void testFetchCastDocumentModelIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);

        DocumentModel document = dmrr.fetch(DocumentModel.class, repoAndIdRef);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = dmrr.fetch(DocumentModel.class, idOnlyRef);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        assertNull(dmrr.fetch(DocumentModel.class, repoAndPathRef));
        assertNull(dmrr.fetch(DocumentModel.class, pathOnlyRef));
        assertNull(dmrr.fetch(DocumentModel.class, "test:uuid1234567890"));
        assertNull(dmrr.fetch(DocumentModel.class, "badrepo:" + doc.getId()));
    }

    @Test
    public void testFetchCastAdapterIdMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);

        VersioningDocument document = dmrr.fetch(VersioningDocument.class, repoAndIdRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());

        document = dmrr.fetch(VersioningDocument.class, idOnlyRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());
    }

    @Test
    public void testFetchCastDocumentModelPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        DocumentModel document = dmrr.fetch(DocumentModel.class, repoAndPathRef);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = dmrr.fetch(DocumentModel.class, pathOnlyRef);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        assertNull(dmrr.fetch(DocumentModel.class, repoAndIdRef));
        assertNull(dmrr.fetch(DocumentModel.class, idOnlyRef));
        assertNull(dmrr.fetch(DocumentModel.class, "test:/doc/toto"));
        assertNull(dmrr.fetch(DocumentModel.class, "badrepo:" + doc.getPathAsString()));
    }

    @Test
    public void testFetchCastAdapterPathMode() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);

        VersioningDocument document = dmrr.fetch(VersioningDocument.class, repoAndPathRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());

        document = dmrr.fetch(VersioningDocument.class, pathOnlyRef);
        assertNotNull(document);
        assertNotNull(document.getVersionLabel());
    }

    @Test
    public void testFetchCastDoesntSupportDocumentType() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertNull(dmrr.fetch(Document.class, repoAndIdRef));
        assertNull(dmrr.fetch(Document.class, idOnlyRef));
    }

    @Test
    public void testFetchCastDoesntSupportStupidTypes() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertNull(dmrr.fetch(List.class, repoAndIdRef));
        assertNull(dmrr.fetch(List.class, idOnlyRef));
    }

    @Test
    public void testGetReferenceRepoAndId() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        dmrr.configure(parameters);
        assertEquals(repoAndIdRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceRepoAndPath() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_PATH);
        dmrr.configure(parameters);
        assertEquals(repoAndPathRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceIdOnly() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_ID_ONLY);
        dmrr.configure(parameters);
        assertEquals(idOnlyRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferencePathOnly() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_PATH_ONLY);
        dmrr.configure(parameters);
        assertEquals(pathOnlyRef, dmrr.getReference(doc));
    }

    @Test
    public void testGetReferenceInvalid() {
        DocumentModelResolver dmrr = new DocumentModelResolver();
        dmrr.configure(new HashMap<>());
        assertNull(dmrr.getReference("nothing"));
    }

    @Test
    public void testConfigurationIsLoaded() {
        DocumentModelResolver idResolver = (DocumentModelResolver) doc.getProperty(REPO_AND_ID_XPATH)
                                                                      .getType()
                                                                      .getObjectResolver();
        assertEquals(MODE.REPO_AND_ID_REF, idResolver.getMode());
        assertEquals(STORE_REPO_AND_ID, idResolver.getParameters().get(PARAM_STORE));
        DocumentModelResolver pathResolver = (DocumentModelResolver) doc.getProperty(REPO_AND_PATH_XPATH)
                                                                        .getType()
                                                                        .getObjectResolver();
        assertEquals(MODE.REPO_AND_PATH_REF, pathResolver.getMode());
        assertEquals(STORE_REPO_AND_PATH, pathResolver.getParameters().get(PARAM_STORE));
    }

    @Test
    public void testNullValueReturnNull() {
        assertNull(doc.getObjectResolver(REPO_AND_ID_XPATH).fetch());
        assertNull(doc.getObjectResolver(REPO_AND_ID_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(REPO_AND_ID_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(REPO_AND_ID_XPATH).getObjectResolver().fetch(DocumentModel.class));
        assertNull(doc.getObjectResolver(REPO_AND_PATH_XPATH).fetch());
        assertNull(doc.getObjectResolver(REPO_AND_PATH_XPATH).fetch(DocumentModel.class));
        assertNull(doc.getProperty(REPO_AND_PATH_XPATH).getObjectResolver().fetch());
        assertNull(doc.getProperty(REPO_AND_PATH_XPATH).getObjectResolver().fetch(DocumentModel.class));
    }

    @Test
    public void testBadValuesValidationFailed() {
        doc.setPropertyValue(REPO_AND_ID_XPATH, "BAD id !");
        assertNull(doc.getProperty(REPO_AND_ID_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(REPO_AND_ID_XPATH).getObjectResolver().validate());
        doc.setPropertyValue(REPO_AND_PATH_XPATH, "BAD path !");
        assertNull(doc.getProperty(REPO_AND_PATH_XPATH).getObjectResolver().fetch());
        assertFalse(doc.getProperty(REPO_AND_PATH_XPATH).getObjectResolver().validate());
        assertEquals(2, validator.validate(doc).numberOfErrors());
    }

    @Test
    public void testIdRefCorrectValues() {
        doTestIdRefCorrectValues(REPO_AND_ID_XPATH, repoAndIdRef);
        doTestIdRefCorrectValues(REPO_AND_ID_XPATH, idOnlyRef);
    }

    protected void doTestIdRefCorrectValues(String xpath, String ref) {
        doc.setPropertyValue(xpath, ref);
        DocumentModel document = (DocumentModel) doc.getProperty(xpath).getObjectResolver().fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getProperty(xpath).getObjectResolver().fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = (DocumentModel) doc.getObjectResolver(xpath).fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getObjectResolver(xpath).fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
    }

    @Test
    public void testIdRefDoesntSupportPath() {
        doTestIdRefDoesntSupportPath(repoAndPathRef);
        doTestIdRefDoesntSupportPath(pathOnlyRef);
    }

    protected void doTestIdRefDoesntSupportPath(String ref) {
        doc.setPropertyValue(REPO_AND_ID_XPATH, ref);
        assertNull(doc.getProperty(REPO_AND_ID_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testPathRefCorrectValues() {
        doTestPathRefCorrectValues(REPO_AND_PATH_XPATH, repoAndPathRef);
        doTestPathRefCorrectValues(REPO_AND_PATH_XPATH, pathOnlyRef);
    }

    protected void doTestPathRefCorrectValues(String xpath, String ref) {
        doc.setPropertyValue(xpath, ref);
        DocumentModel document = (DocumentModel) doc.getProperty(xpath).getObjectResolver().fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getProperty(xpath).getObjectResolver().fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = (DocumentModel) doc.getObjectResolver(xpath).fetch();
        assertNotNull(document);
        assertEquals("doc1", document.getName());
        document = doc.getObjectResolver(xpath).fetch(DocumentModel.class);
        assertNotNull(document);
        assertEquals("doc1", document.getName());
    }

    @Test
    public void testPathRefFieldDoesntSupportId() {
        doTestPathRefFieldDoesntSupportId(repoAndIdRef);
        doTestPathRefFieldDoesntSupportId(idOnlyRef);
    }

    protected void doTestPathRefFieldDoesntSupportId(String ref) {
        doc.setPropertyValue(REPO_AND_PATH_XPATH, ref);
        assertNull(doc.getProperty(REPO_AND_PATH_XPATH).getObjectResolver().fetch());
    }

    @Test
    public void testTranslation() {
        DocumentModelResolver iddmrr = new DocumentModelResolver();
        Map<String, String> userParams = new HashMap<>();
        userParams.put(PARAM_STORE, STORE_REPO_AND_ID);
        iddmrr.configure(userParams);
        checkMessage(iddmrr);
        DocumentModelResolver pathdmrr = new DocumentModelResolver();
        Map<String, String> groupParams = new HashMap<>();
        groupParams.put(PARAM_STORE, STORE_REPO_AND_PATH);
        pathdmrr.configure(groupParams);
        checkMessage(pathdmrr);

    }

    @Test
    public void testSerialization() throws Exception {
        // create it
        DocumentModelResolver resolver = new DocumentModelResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(PARAM_STORE, STORE_REPO_AND_ID);
        resolver.configure(parameters);
        // write it
        byte[] buffer = SerializationUtils.serialize(resolver);
        // forget the resolver
        resolver = null;
        // read it
        Object readObject = SerializationUtils.deserialize(buffer);
        // check it's a dir resolver
        assertTrue(readObject instanceof DocumentModelResolver);
        DocumentModelResolver readResolver = (DocumentModelResolver) readObject;
        // check the configuration
        Map<String, Serializable> outputParameters = readResolver.getParameters();
        assertEquals(STORE_REPO_AND_ID, outputParameters.get(PARAM_STORE));
        // test it works: validate
        assertTrue(readResolver.validate(repoAndIdRef));
        assertTrue(readResolver.validate(idOnlyRef));
        // test it works: fetch
        Object entity = readResolver.fetch(repoAndIdRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals(doc.getPathAsString(), ((DocumentModel) entity).getPathAsString());
        entity = readResolver.fetch(idOnlyRef);
        assertTrue(entity instanceof DocumentModel);
        assertEquals(doc.getPathAsString(), ((DocumentModel) entity).getPathAsString());
        // test it works: getReference
        assertEquals(repoAndIdRef, readResolver.getReference(doc));
    }

    @Test
    public void testUserWithNoAccessToDocument() {
        String username = "foo";
        DocumentModelResolver resolver = new DocumentModelResolver();
        Map<String, String> params = Collections.singletonMap(PARAM_STORE, STORE_REPO_AND_ID);
        resolver.configure(params);

        DocumentModel adminDoc = resolver.fetch(DocumentModel.class, repoAndIdRef);
        assertNotNull(adminDoc);
        adminDoc = resolver.fetch(DocumentModel.class, idOnlyRef);
        assertNotNull(adminDoc);

        try {
            ClientLoginModule.getThreadLocalLogin().push(new UserPrincipal(username, Collections.emptyList(), false, false),
                                                         null, null);
            DocumentModel doc = resolver.fetch(DocumentModel.class, repoAndIdRef);
            assertNull(doc);
            doc = resolver.fetch(DocumentModel.class, idOnlyRef);
            assertNull(doc);
        } finally {
            ClientLoginModule.getThreadLocalLogin().pop();
        }
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
