/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.test;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.schema.Prefetch;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.FreemarkerMacro;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.runtime.services.streaming.URLSource;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestFreemarkerRendering extends NXRuntimeTestCase {

    FreemarkerEngine engine;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.platform.rendering.tests",
                "OSGI-INF/test-schema.xml");

        engine = new FreemarkerEngine();
        engine.setResourceLocator(new MyResourceLocator());

        WikiTransformer tr = new WikiTransformer();
        tr.getSerializer().addFilter(
                new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*",
                        "<link>$0</link>"));
        tr.getSerializer().addFilter(
                new PatternFilter("NXP-[0-9]+",
                        "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));
        tr.getSerializer().registerMacro(new FreemarkerMacro());
        engine.setSharedVariable("wiki", tr);
    }

    public static String getTestFile(String filePath)
            throws UnsupportedEncodingException {
        return FileUtils.getResourcePathFromContext(filePath);
    }

    @Test
    public void testRendering() throws Exception {
        DocumentModelImpl doc1 = new DocumentModelImpl(null, "File", null,
                new Path("/root/folder/wiki1"), null, null, null, new String[] {
                        "dublincore", "file" }, null, null, "default");
        doc1.addDataModel(new DataModelImpl("dublincore"));
        DocumentPart documentPart = doc1.getPart("dublincore");
        documentPart.get("title").setValue("The dublincore title for doc1");
        documentPart.get("description").setValue(
                "A descripton *with* wiki code and a WikiName");
        Blob blob = new StreamingBlob(new URLSource(
                TestFreemarkerRendering.class.getClassLoader().getResource(
                        "testdata/blob.wiki")));
        doc1.getPart("dublincore").get("content").setValue(blob);
        // also add something prefetched (dm not loaded)
        Prefetch prefetch = new Prefetch();
        prefetch.put("filename", "file", "filename", "somefile");
        doc1.prefetch = prefetch;

        DocumentModelImpl doc2 = new DocumentModelImpl("/root/folder/wiki2",
                "Test Doc 2", "File");
        doc2.addDataModel(new DataModelImpl("dublincore"));
        doc2.getPart("dublincore").get("title").setValue(
                "The dublincore title for doc1");
        engine.setSharedVariable("doc", doc2);

        StringWriter writer = new StringWriter();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("doc", doc1);

        // double s = System.currentTimeMillis();
        engine.render("testdata/c.ftl", input, writer);
        // double e = System.currentTimeMillis();

        InputStream expected = new FileInputStream(
                getTestFile("expecteddata/c_output.txt"));
        assertTextEquals(FileUtils.read(expected), writer.toString());

    }

    protected void assertTextEquals(String expected, String actual) {
        actual = actual.replace("\r", "");
        assertEquals(expected, actual);
    }

    @Test
    public void testUrlEscaping() throws Exception {
        StringWriter writer = new StringWriter();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("parameter", "\u00e9/");
        engine.render("testdata/url.ftl", input, writer);
        assertEquals("<p>http://google.com?q=%C3%A9%2F</p>", writer.toString());
    }
}
