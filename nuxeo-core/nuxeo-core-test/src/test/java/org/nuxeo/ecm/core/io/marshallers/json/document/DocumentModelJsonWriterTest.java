/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertyJsonWriter.OMIT_PHANTOM_SECURED_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.DepthValues;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class DocumentModelJsonWriterTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public static final String PROP_DOC_ID_ONLY_REF = "dr:docIdOnlyRef";

    public static final String PROP_DOC_PATH_ONLY_REF = "dr:docPathOnlyRef";

    public static final String PROP_DOC_REPO_AND_ID_REF = "dr:docRepoAndIdRef";

    public static final String PROP_DOC_REPO_AND_PATH_REF = "dr:docRepoAndPathRef";

    public static final String PROP_DOC_PATH_REF_LIST = "dr:docRepoAndPathRefList";

    public static final String PROP_DOC_PATH_REF_SIMPLE_LIST = "dr:docRepoAndPathRefSimpleList";

    public static final String PROP_DOC_REF_TYPE = "dr:docRefType";

    public static final String SUBPROP_REPO_AND_ID = "docRefRepoAndId";

    public static final String SUBPROP_REPO_AND_PATH = "docRefRepoAndPath";

    public static final String REPO = "test";

    protected static final int BASE_PROPERTIES = 19;

    public DocumentModelJsonWriterTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    private DocumentModel document;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    private CoreSession session;

    @Inject
    private SchemaManager schemaManager;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "myDoc", "RefDoc");
        document = session.createDocument(document);
    }

    @Test
    public void testDefault() throws Exception {
        JsonAssert json = jsonAssert(document);
        json.isObject();
        json.properties(BASE_PROPERTIES);
        json.has("entity-type").isEquals("document");
        json.has("repository").isEquals(REPO);
        json.has("uid").isEquals(document.getId());
        json.has("path").isEquals("/myDoc");
        json.has("type").isEquals("RefDoc");
        json.has("state").isEquals("undefined");
        json.has("parentRef").isEquals(document.getParentRef().toString());
        json.has("isCheckedOut").isTrue();
        json.has("isVersion").isFalse();
        json.has("isProxy").isFalse();
        json.has("isTrashed").isFalse();
        json.has("changeToken").isNull();
        json.has("title").isEquals("myDoc");
        json.has("facets").contains("Folderish");
        json.has("isRecord").isFalse();
        json.has("retainUntil").isNull();
        json.has("hasLegalHold").isFalse();
        json.has("isUnderRetentionOrLegalHold").isFalse();
    }

    /**
     * @since 11.1
     */
    @Test
    public void testHasSchemas() throws Exception {
        JsonAssert json = jsonAssert(document);
        json.isObject();
        json.has("schemas").length(3);
        json.has("schemas").get(0).has("name").isEquals("documentResolver");
        json.has("schemas").get(0).has("prefix").isEquals("dr");
        json.has("schemas").get(1).has("name").isEquals("dublincore");
        json.has("schemas").get(1).has("prefix").isEquals("dc");
        json.has("schemas").get(2).has("name").isEquals("noPrefix");
        json.has("schemas").get(2).has("prefix").isEquals("noPrefix");
    }

    @Test
    public void testIsVersion() throws Exception {
        DocumentRef versionDocRef = document.checkIn(VersioningOption.MAJOR, "CheckIn comment");
        DocumentModel versionDoc = session.getDocument(versionDocRef);
        JsonAssert json = jsonAssert(versionDoc);
        json.isObject();
        json.has("isVersion").isTrue();
        json.has("versionableId").isEquals(document.getId());
    }

    /**
     * @since 10.3
     */
    @Test
    public void testIsProxy() throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "folder", "DummyDoc");
        folder = session.createDocument(folder);
        DocumentRef versionDocRef = document.checkIn(VersioningOption.MAJOR, "CheckIn comment");
        DocumentModel versionDoc = session.getDocument(versionDocRef);
        DocumentModel proxyDoc = session.publishDocument(versionDoc, folder);
        JsonAssert json = jsonAssert(proxyDoc);
        json.isObject();
        json.has("isProxy").isTrue();
        json.has("versionableId").isEquals(document.getId());
        json.has("proxyTargetId").isEquals(versionDoc.getId());
    }

    @Test
    public void testWithVersion() throws Exception {
        JsonAssert json = jsonAssert(document, CtxBuilder.fetchInDoc("versionLabel").get());
        json.isObject();
        json.properties(BASE_PROPERTIES + 1);
        json.has("versionLabel").isEquals("");
    }

    @Test
    public void testWithLastModified() throws Exception {
        document.setPropertyValue("dc:modified", new Date());
        JsonAssert json = jsonAssert(document);
        json.isObject();
        json.properties(BASE_PROPERTIES + 1);
        json.has("lastModified").isText();
    }

    @Test
    public void testRetentionAndHold() throws Exception {
        session.makeRecord(document.getRef());
        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.HOUR, -1); // one hour ago
        session.setRetainUntil(document.getRef(), retainUntil, null);
        session.setLegalHold(document.getRef(), true, null);
        document.refresh();
        JsonAssert json = jsonAssert(document);
        json.isObject();
        json.has("isRecord").isTrue();
        String expectedRetainUntil = ISODateTimeFormat.dateTime().print(new DateTime(retainUntil));
        json.has("retainUntil").isEquals(expectedRetainUntil);
        json.has("hasLegalHold").isTrue();
        json.has("isUnderRetentionOrLegalHold").isTrue();
    }

    @Test
    public void testTitleIsDcTitle() throws Exception {
        String title = "My document";
        document.setPropertyValue("dc:title", title);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").get());
        json.has("title").isEquals(title);
    }

    @Test
    public void testWithAllProperties() throws Exception {
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").get());
        json = json.has("properties");
        json.isObject();
        int nbProperties = 0;
        for (String schemaName : document.getSchemas()) {
            Schema schema = schemaManager.getSchema(schemaName);
            for (Field field : schema.getFields()) {
                nbProperties++;
                String prefixedName = field.getName().getPrefixedName();
                if (!prefixedName.contains(":")) {
                    prefixedName = schemaName + ":" + prefixedName;
                }
                json.has(prefixedName);
            }
        }
        json.properties(nbProperties);
    }

    @Test
    public void testStringPropertyValue() throws Exception {
        String value = "toto";
        String propName = "dr:propString";
        document.setPropertyValue(propName, value);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + propName).isEquals(value);
    }

    @Test
    public void testIntPropertyValue() throws Exception {
        int value = 123;
        String propName = "dr:propInt";
        document.setPropertyValue(propName, value);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + propName).isEquals(value);
    }

    @Test
    public void testDeltaLongPropertyValue() throws Exception {
        DeltaLong delta = DeltaLong.valueOf(Long.valueOf(123), 456);
        String propName = "dr:propInt";
        document.setPropertyValue(propName, delta);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + propName).isEquals(delta.longValue());
    }

    @Test
    public void testDoublePropertyValue() throws Exception {
        double value = 123.123;
        String propName = "dr:propDouble";
        document.setPropertyValue(propName, value);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + propName).isEquals(value, 0.0);
    }

    @Test
    public void testBooleanPropertyValue() throws Exception {
        String propName = "dr:propBoolean";
        document.setPropertyValue(propName, true);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + propName).isTrue();
    }

    @Test
    public void testDatePropertyValue() throws Exception {
        Date value = new Date();
        String propName = "dr:propDate";
        document.setPropertyValue(propName, value);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + propName).isEquals(DateParser.formatW3CDateTime(value));
    }

    @Test
    public void testListPropertyValue() throws Exception {
        String pathRef1 = REPO + ":/";
        String pathRef2 = REPO + ":/myDoc";
        Property list = document.getProperty(PROP_DOC_PATH_REF_LIST);
        list.addValue(pathRef1);
        list.addValue(pathRef2);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties." + PROP_DOC_PATH_REF_LIST).contains(pathRef1, pathRef2);
    }

    @Test
    public void testComplexPropertyValue() throws Exception {
        String pathRef = REPO + ":/";
        String idRef = REPO + ":" + document.getId();
        Property list = document.getProperty(PROP_DOC_REF_TYPE);
        list.setValue(SUBPROP_REPO_AND_PATH, pathRef);
        list.setValue(SUBPROP_REPO_AND_ID, idRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json = json.has("properties." + PROP_DOC_REF_TYPE).isObject();
        json.has(SUBPROP_REPO_AND_PATH).isEquals(pathRef);
        json.has(SUBPROP_REPO_AND_ID).isEquals(idRef);
    }

    @Test
    public void testNoFetching() throws Exception {
        String pathRef = REPO + ":/";
        String xpath = PROP_DOC_REPO_AND_PATH_REF;
        document.setPropertyValue(xpath, pathRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").get());
        json.has("properties." + xpath).isEquals(pathRef);
    }

    @Test
    public void testSimpleFetchingRepoAndPath() throws Exception {
        String xpath = PROP_DOC_REPO_AND_PATH_REF;

        // test with repo + path
        String pathRef = REPO + ":/myDoc";
        document.setPropertyValue(xpath, pathRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");

        // test with missing repo
        pathRef = "/myDoc";
        document.setPropertyValue(xpath, pathRef);
        json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");

        // TODO other repo
    }

    @Test
    public void testSimpleFetchingRepoAndId() throws Exception {
        String xpath = PROP_DOC_REPO_AND_ID_REF;

        // test with repo + id
        String idRef = REPO + ":" + document.getId();
        document.setPropertyValue(xpath, idRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");

        // test with missing repo
        idRef = document.getId();
        document.setPropertyValue(xpath, idRef);
        json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");

        // TODO other repo
    }

    @Test
    public void testSimpleFetchingPathOnly() throws Exception {
        String xpath = PROP_DOC_PATH_ONLY_REF;

        // test with path
        String pathRef = "/myDoc";
        document.setPropertyValue(xpath, pathRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");

        // test with repo + path
        pathRef = REPO + ":/myDoc";
        document.setPropertyValue(xpath, pathRef);
        json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");
    }

    @Test
    public void testSimpleFetchingIdOnly() throws Exception {
        String xpath = PROP_DOC_ID_ONLY_REF;

        // test with id
        String idRef = document.getId();
        document.setPropertyValue(xpath, idRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");

        // test with repo + id
        idRef = REPO + ":" + document.getId();
        document.setPropertyValue(xpath, idRef);
        json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/myDoc");
    }

    @Test
    public void testArrayPropertiesFetching() throws Exception {
        String pathRef = REPO + ":/";
        String xpath = PROP_DOC_PATH_REF_SIMPLE_LIST;
        document.setPropertyValue(xpath, new String[] { pathRef, pathRef, pathRef });
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isArray();
        json.length(3);
        json.childrenContains("entity-type", "document", "document", "document");
        json.childrenContains("path", "/", "/", "/");
    }

    @Test
    public void testInvalidValueFetching() throws Exception {
        String pathRef = REPO + ":/toto/is/doing/something";
        String xpath = PROP_DOC_PATH_REF_SIMPLE_LIST;
        document.setPropertyValue(xpath, new String[] { pathRef, pathRef });
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isArray();
        json.length(2);
        json.contains(pathRef, pathRef);
    }

    @Test
    public void testFullFetching() throws Exception {
        String pathRef = REPO + ":/";
        document.setPropertyValue(PROP_DOC_REPO_AND_PATH_REF, pathRef);
        document.getProperty(PROP_DOC_REF_TYPE).setValue(SUBPROP_REPO_AND_PATH, pathRef);
        document.getProperty(PROP_DOC_PATH_REF_LIST).addValue(pathRef);
        document.getProperty(PROP_DOC_PATH_REF_SIMPLE_LIST).setValue(new String[] { pathRef, pathRef });
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc("properties").get());
        JsonAssert child;
        child = json.has("properties." + PROP_DOC_REPO_AND_PATH_REF).isObject();
        child.has("entity-type").isEquals("document");
        child.has("path").isEquals("/");
        child = json.has("properties." + PROP_DOC_REF_TYPE + "." + SUBPROP_REPO_AND_PATH).isObject();
        child.has("entity-type").isEquals("document");
        child.has("path").isEquals("/");
        child = json.has("properties." + PROP_DOC_PATH_REF_LIST + "[0]").isObject();
        child.has("entity-type").isEquals("document");
        child.has("path").isEquals("/");
        child = json.has("properties." + PROP_DOC_PATH_REF_SIMPLE_LIST).isArray();
        child.length(2);
        child.childrenContains("entity-type", "document", "document");
        child.childrenContains("path", "/", "/");
    }

    @Test
    public void testDepthControl() throws Exception {
        DocumentModel root = session.createDocumentModel("/", "root", "RefDoc");
        root = session.createDocument(root);
        DocumentModel child = session.createDocumentModel("/root", "child", "RefDoc");
        child = session.createDocument(child);
        DocumentModel max = session.createDocumentModel("/root/child", "max", "RefDoc");
        max = session.createDocument(max);
        DocumentModel over = session.createDocumentModel("/root/child/max", "over", "RefDoc");
        over = session.createDocument(over);
        // default: expect properties and enrichers loading for root but not for children
        RenderingContext ctxDefault = CtxBuilder.properties("*").enrichDoc("children").get();
        JsonAssert jsonDefault = jsonAssert(root, ctxDefault);
        jsonDefault.has("properties");
        jsonDefault.has("contextParameters");
        jsonDefault = jsonDefault.has("contextParameters.children.entries[0]");
        jsonDefault.hasNot("properties");
        jsonDefault.hasNot("contextParameters");
        // root: same as default
        RenderingContext ctxRoot = CtxBuilder.properties("*").enrichDoc("children").depth(DepthValues.root).get();
        JsonAssert jsonRoot = jsonAssert(root, ctxRoot);
        jsonRoot.has("properties");
        jsonRoot.has("contextParameters");
        jsonRoot = jsonRoot.has("contextParameters.children.entries[0]");
        jsonRoot.hasNot("properties");
        jsonRoot.hasNot("contextParameters");
        // children: expect properties and enrichers loaded for root and children but not for grant children
        RenderingContext ctxChildren = CtxBuilder.properties("*")
                                                 .enrichDoc("children")
                                                 .depth(DepthValues.children)
                                                 .get();
        JsonAssert jsonChildren = jsonAssert(root, ctxChildren);
        jsonChildren.has("properties");
        jsonChildren.has("contextParameters");
        jsonChildren = jsonChildren.has("contextParameters.children.entries[0]");
        jsonChildren.has("properties");
        jsonChildren.has("contextParameters");
        jsonChildren = jsonChildren.has("contextParameters.children.entries[0]");
        jsonChildren.hasNot("properties");
        jsonChildren.hasNot("contextParameters");
        // max: expect properties and enrichers loaded for root and children and grant children but not anymore
        RenderingContext ctxMax = CtxBuilder.properties("*").enrichDoc("children").depth(DepthValues.max).get();
        JsonAssert jsonMax = jsonAssert(root, ctxMax);
        jsonMax.has("properties");
        jsonMax.has("contextParameters");
        jsonMax = jsonMax.has("contextParameters.children.entries[0]");
        jsonMax.has("properties");
        jsonMax.has("contextParameters");
        jsonMax = jsonMax.has("contextParameters.children.entries[0]");
        jsonMax.has("properties");
        jsonMax.has("contextParameters");
        jsonMax = jsonMax.has("contextParameters.children.entries[0]");
        jsonMax.hasNot("properties");
        jsonMax.hasNot("contextParameters");
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-secured-types-contrib.xml")
    public void testDetachedEmptyDocWithDefault() throws IOException {
        testDetachedEmptyDoc(null);
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-secured-types-contrib.xml")
    public void testDetachedEmptyDocWithEmptySecuredProperties() throws IOException {
        testDetachedEmptyDoc(false);
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-secured-types-contrib.xml")
    public void testDetachedEmptyDocWithoutEmptySecuredProperties() throws IOException {
        testDetachedEmptyDoc(true);
    }

    protected void testDetachedEmptyDoc(Boolean omitPhantomSecured) throws IOException {
        DocumentModel emptyDoc = session.createDocumentModel("/", "myEmptyDoc", "Secured");
        emptyDoc.detach(false);
        JsonAssert json = jsonAssert(emptyDoc,
                CtxBuilder.properties("*").param(OMIT_PHANTOM_SECURED_PROPERTY, omitPhantomSecured).get());
        json = json.has("properties");
        json.isObject();
        int nbProperties = 0;
        for (String schemaName : emptyDoc.getSchemas()) {
            Schema schema = schemaManager.getSchema(schemaName);
            for (Field field : schema.getFields()) {
                // filter out secured properties
                if (!(Boolean.TRUE.equals(omitPhantomSecured)
                        && schemaManager.isSecured(schemaName, field.getName().getLocalName()))) {
                    nbProperties++;
                    String prefixedName = field.getName().getPrefixedName();
                    if (!prefixedName.contains(":")) {
                        prefixedName = schemaName + ":" + prefixedName;
                    }
                    json.has(prefixedName);
                }
            }
        }
        json.properties(nbProperties);
    }

    // NXP-30192
    @Test
    public void testFetchDocumentListWithBrowsePermission() throws IOException {
        // bob can read
        DocumentModel file1 = session.createDocumentModel("/", "file1", "File");
        file1 = session.createDocument(file1);
        ACP acp = file1.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("bob", "Read", true));
        file1.setACP(acp, true);
        // bob can only browse
        DocumentModel file2 = session.createDocumentModel("/", "file2", "File");
        file2 = session.createDocument(file2);
        acp = file2.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("bob", "Browse", true));
        file2.setACP(acp, true);
        // bob can read
        DocumentModel file3 = session.createDocumentModel("/", "file3", "File");
        file3 = session.createDocument(file3);
        acp = file3.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("bob", "Read", true));
        file3.setACP(acp, true);
        // bob can read
        DocumentModel refDoc = session.createDocumentModel("/", "refDoc", "RefDoc");
        refDoc.setPropertyValue(PROP_DOC_PATH_REF_SIMPLE_LIST,
                (Serializable) Arrays.asList(file1.getPathAsString(), file2.getPathAsString(), file3.getPathAsString()));
        refDoc = session.createDocument(refDoc);
        acp = refDoc.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("bob", "Read", true));
        refDoc.setACP(acp, true);
        transactionalFeature.nextTransaction();

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            DocumentModel bobRefDoc = bobSession.getDocument(refDoc.getRef());
            RenderingContext ctxDefault = CtxBuilder.properties("*")
                    .fetchInDoc(PROP_DOC_PATH_REF_SIMPLE_LIST)
                    .session(bobSession)
                    .get();
            JsonAssert json = jsonAssert(bobRefDoc, ctxDefault);
            assertNotNull(json);
            json = json.has("properties." + PROP_DOC_PATH_REF_SIMPLE_LIST).isArray();
            json.length(3);
            // bob can read
            JsonAssert child = json.get(0);
            child.childrenContains("entity-type", "document");
            child.childrenContains("path", "/file1");
            // file bob can't read
            child = json.get(1);
            child.isEquals("/file2");
            // bob can read
            child = json.get(2);
            child.childrenContains("entity-type", "document");
            child.childrenContains("path", "/file3");
        }
    }

    // NXP-30615
    @Test
    @Deploy("org.nuxeo.ecm.core.test:OSGI-INF/other-repo.xml")
    public void testMultiRepo() throws IOException {
        // create a document in another repo
        DocumentModel doc;
        try (CloseableCoreSession secondSession = CoreInstance.openCoreSession("import")) {
            doc = secondSession.createDocumentModel("/", "file", "File");
            doc.setPropertyValue("dc:title", "bar foo");
            doc = secondSession.createDocument(doc);
        }

        // write the document with the default session in ctx
        // the session on the "import" repository is now closed
        RenderingContext ctxDefault = CtxBuilder.properties("*").session(session).get();
        JsonAssert json = jsonAssert(doc, ctxDefault);
        assertNotNull(json);
    }

}
