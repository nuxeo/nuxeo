/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.io.marshallers.json.document;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.DepthValues;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class DocumentModelJsonWriterTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public DocumentModelJsonWriterTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    private DocumentModel document;

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
        json.properties(11);
        json.has("entity-type").isEquals("document");
        json.has("repository").isEquals("test");
        json.has("uid").isEquals(document.getId());
        json.has("path").isEquals("/myDoc");
        json.has("type").isEquals("RefDoc");
        json.has("state").isEquals("undefined");
        json.has("parentRef").isEquals(document.getParentRef().toString());
        json.has("isCheckedOut").isTrue();
        json.has("changeToken").isNull();
        json.has("title").isEquals("myDoc");
        json.has("facets").contains("Folderish");
    }

    @Test
    public void testWithVersion() throws Exception {
        JsonAssert json = jsonAssert(document, CtxBuilder.fetchInDoc("versionLabel").get());
        json.isObject();
        json.properties(12);
        json.has("versionLabel").isEquals("");
    }

    @Test
    public void testWithLastModified() throws Exception {
        document.setPropertyValue("dc:modified", new Date());
        JsonAssert json = jsonAssert(document);
        json.isObject();
        json.properties(12);
        json.has("lastModified").isText();
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
        String pathRef1 = "test:/";
        String pathRef2 = "test:/myDoc";
        Property list = document.getProperty("dr:docPathRefList");
        list.addValue(pathRef1);
        list.addValue(pathRef2);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json.has("properties.dr:docPathRefList").contains(pathRef1, pathRef2);
    }

    @Test
    public void testComplexPropertyValue() throws Exception {
        String pathRef = "test:/";
        String idRef = "test:" + document.getId();
        Property list = document.getProperty("dr:docRefType");
        list.setValue("docRefTypePath", pathRef);
        list.setValue("docRefTypeId", idRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("documentResolver").get());
        json = json.has("properties.dr:docRefType").isObject();
        json.has("docRefTypePath").isEquals(pathRef);
        json.has("docRefTypeId").isEquals(idRef);
    }

    @Test
    public void testNoFetching() throws Exception {
        String pathRef = "test:/";
        String xpath = "dr:docPathRef";
        document.setPropertyValue(xpath, pathRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").get());
        json.has("properties." + xpath).isEquals(pathRef);
    }

    @Test
    public void testSimpleFetching() throws Exception {
        String pathRef = "test:/";
        String xpath = "dr:docPathRef";
        document.setPropertyValue(xpath, pathRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc(xpath).get());
        json = json.has("properties." + xpath).isObject();
        json.has("entity-type").isEquals("document");
        json.has("path").isEquals("/");
    }

    @Test
    public void testFullFetching() throws Exception {
        String pathRef = "test:/";
        document.setPropertyValue("dr:docPathRef", pathRef);
        document.getProperty("dr:docRefType").setValue("docRefTypePath", pathRef);
        document.getProperty("dr:docPathRefList").addValue(pathRef);
        JsonAssert json = jsonAssert(document, CtxBuilder.properties("*").fetchInDoc("properties").get());
        JsonAssert child;
        child = json.has("properties.dr:docPathRef").isObject();
        child.has("entity-type").isEquals("document");
        child.has("path").isEquals("/");
        child = json.has("properties.dr:docRefType.docRefTypePath").isObject();
        child.has("entity-type").isEquals("document");
        child.has("path").isEquals("/");
        child = json.has("properties.dr:docPathRefList[0]").isObject();
        child.has("entity-type").isEquals("document");
        child.has("path").isEquals("/");
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
        RenderingContext ctxChildren = CtxBuilder.properties("*").enrichDoc("children").depth(DepthValues.children).get();
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

}
