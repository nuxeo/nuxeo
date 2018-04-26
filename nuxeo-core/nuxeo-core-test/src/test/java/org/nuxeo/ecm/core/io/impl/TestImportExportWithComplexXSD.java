/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Nuxeo
 */
package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = ComplexDocRepositoryInit.class)
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class TestImportExportWithComplexXSD {

    @Inject
    protected CoreSession session;

    @Test
    public void checkComplexTypeExportImport() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/testDoc"));
        assertNotNull(doc);

        verifyProperties(doc);
        File out = Framework.createTempFile("model-io", ".zip");

        DocumentReader reader = new SingleDocumentReader(session, doc);
        DocumentWriter writer = new NuxeoArchiveWriter(out);

        try {
            // creating a pipe
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } finally {
            writer.close();
            reader.close();
        }

        Assert.assertTrue(out.length() > 0);

        DocumentModel newParent = session.createDocumentModel("/", "importRoot", "Folder");
        newParent.setPropertyValue("dc:title", "Import Root");
        newParent = session.createDocument(newParent);
        session.save();

        reader = new NuxeoArchiveReader(out);
        writer = new DocumentModelWriter(session, newParent.getPathAsString());

        try {
            // creating a pipe
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } finally {
            writer.close();
            reader.close();
            out.delete();
        }

        DocumentModelList children = session.getChildren(newParent.getRef());
        Assert.assertEquals(1, children.totalSize());
        verifyProperties(children.get(0));
    }

    protected void verifyProperties(DocumentModel doc) throws Exception {
        Assert.assertEquals("Insurance", doc.getPropertyValue("cs:modelType"));

        Assert.assertEquals("V1", doc.getPropertyValue("cs:currentVersion"));
        Assert.assertEquals("Internal", doc.getPropertyValue("cs:origin"));

        Map<String, Serializable> segment = (Map<String, Serializable>) doc.getPropertyValue("cs:segmentVariable");

        Assert.assertEquals("MySegment", segment.get("name"));
        Assert.assertEquals("SomeTarget", segment.get("target"));
        Assert.assertEquals("rawVariable", segment.get("variableType"));
        Assert.assertNull(segment.get("dataType"));

        List<String> roles = (List<String>) segment.get("roles");

        Assert.assertEquals(3, roles.size());
        Assert.assertTrue(roles.contains("Score"));
        Assert.assertTrue(roles.contains("ComparisonScore"));
        Assert.assertTrue(roles.contains("Decision"));

        ACP acp = doc.getACP();
        assertNotNull(acp);
        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        assertEquals(2, acl.size());
        ACE ace = acl.get(0);
        assertEquals("leela", ace.getUsername());
        assertEquals("Read", ace.getPermission());
        assertNull(ace.getCreator());
        assertNull(ace.getBegin());
        assertNull(ace.getEnd());
        ace = acl.get(1);
        assertEquals("fry", ace.getUsername());
        assertEquals("Write", ace.getPermission());
        assertEquals("leela", ace.getCreator());
        Calendar begin = ace.getBegin();
        assertNotNull(begin);
        Calendar expectedBegin = new GregorianCalendar(2000, 10, 10);
        assertEquals(expectedBegin, begin);
        Calendar end = ace.getEnd();
        assertNotNull(end);
        Calendar expectedEnd = new GregorianCalendar(2010, 10, 10);
        assertEquals(expectedEnd, end);
    }

}
