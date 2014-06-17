package org.nuxeo.ecm.platform.rendering.test;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 *
 * $Id$
 */

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.FreemarkerMacro;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentTemplate {

    FreemarkerEngine engine;

    DocumentModel doc;

    @Inject
    protected CoreSession session;

    @Before
    public void setUp() throws Exception {

        System.out.println("Setup");

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

        doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:issued", Calendar.getInstance());
        doc.setPropertyValue("dc:subjects", new String[] { "A", "B", "C" });
        List<Map<String, Serializable>> blobs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Blob blob = new StringBlob("something");
            blob.setFilename("file" + i + ".something");
            blob.setMimeType("application/something");
            Map<String, Serializable> blobEntry = new HashMap<>();
            blobEntry.put("file", (Serializable) blob);
            blobEntry.put("filename", blob.getFilename());
            blobs.add(blobEntry);
        }
        doc.setPropertyValue("files:files", (Serializable) blobs);
        doc = session.createDocument(doc);
    }

    @Test
    public void testIteratorOnDocumentTemplate() throws Exception {

        StringWriter writer = new StringWriter();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("doc", doc);

        engine.render("testdata/testlists.ftl", input, writer);

        String output = writer.toString();
        Assert.assertTrue(output.contains("#versionLabel#"));
        Assert.assertTrue(output.contains("#children#"));
        Assert.assertTrue(output.contains("A|B|C|"));
        Assert.assertTrue(output.contains("file3.something 9"));
    }
}
