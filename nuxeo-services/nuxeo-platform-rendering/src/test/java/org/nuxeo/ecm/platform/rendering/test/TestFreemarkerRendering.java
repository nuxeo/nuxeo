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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.core.schema.XSDLoader;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.FreemarkerMacro;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.services.streaming.URLSource;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestFreemarkerRendering extends NXRuntimeTestCase {

    FreemarkerEngine engine;

    public static void initSchemaManager() throws Exception {
        SchemaManagerImpl mgr = new SchemaManagerImpl();
        XSDLoader loader = new XSDLoader(mgr);
        loader.loadSchema("dublincore", "dc",
                TestFreemarkerRendering.class.getClassLoader().getResource(
                        "schemas/mySchema.xsd"));
        // set a custom service provider to be able to lookup services without
        // loading the framework
        DefaultServiceProvider provider = new DefaultServiceProvider();
        provider.registerService(SchemaManager.class, mgr);
        DefaultServiceProvider.setProvider(provider);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        initSchemaManager();

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

    public void testRendering() throws Exception {
        final DocumentModelImpl doc1 = new DocumentModelImpl(
                "/root/folder/wiki1", "Test Doc 1", "File");
        doc1.addDataModel(new DataModelImpl("dublincore"));
        DocumentPart documentPart = doc1.getPart("dublincore");
        documentPart.get("title").setValue("The dublincore title for doc1");
        documentPart.get("description").setValue(
                "A descripton *with* wiki code and a WikiName");
        Blob blob = new StreamingBlob(new URLSource(
                TestFreemarkerRendering.class.getClassLoader().getResource(
                        "testdata/blob.wiki")));
        doc1.getPart("dublincore").get("content").setValue(blob);

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

        // File tmpFile = File.createTempFile("test", ".txt");
        // OutputStream fileOutputStream = new FileOutputStream(tmpFile);
        // fileOutputStream.write(writer.toString().getBytes());

        InputStream expected = new FileInputStream(
                getTestFile("expecteddata/c_output.txt"));
        assertEquals(FileUtils.read(expected), writer.toString());

        // System.out.println(writer.getBuffer());
        // System.out.println(">>>>>>>>>> RENDERING TOOK: " + ((e - s) / 1000));
        // System.out.flush();
        //
        // for (int i = 0; i < 1; i++) {
        // writer = new StringWriter();
        // s = System.currentTimeMillis();
        // engine.render("testdata/c.ftl", input, writer);
        // e = System.currentTimeMillis();
        // System.out.println("###############################");
        // System.out.println(writer.getBuffer());
        // System.out.println("###############################");
        // System.out.println(">>>>>>>>>> " + (i + 2) + " RENDERING TOOK: "
        // + ((e - s) / 1000));
        // System.out.println("###############################");
        // }
    }

}
