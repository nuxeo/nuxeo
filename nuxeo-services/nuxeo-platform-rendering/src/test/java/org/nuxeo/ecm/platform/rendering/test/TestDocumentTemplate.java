/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.rendering.test;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.FreemarkerMacro;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import junit.framework.Assert;

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
        tr.getSerializer().addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
        tr.getSerializer().addFilter(
                new PatternFilter("NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));
        tr.getSerializer().registerMacro(new FreemarkerMacro());
        engine.setSharedVariable("wiki", tr);

        doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:issued", Calendar.getInstance());
        doc.setPropertyValue("dc:subjects", new String[] { "A", "B", "C" });
        List<Map<String, Serializable>> blobs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Blob blob = Blobs.createBlob("something", "application/something", null, "file" + i + ".something");
            Map<String, Serializable> blobEntry = new HashMap<>();
            blobEntry.put("file", (Serializable) blob);
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
