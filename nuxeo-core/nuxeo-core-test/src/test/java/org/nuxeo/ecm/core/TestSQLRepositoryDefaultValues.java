/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/defaultvalue-docTypes.xml" })
public class TestSQLRepositoryDefaultValues {

    @Inject
    protected CoreSession session;

    @Test
    // FIXME: it should fail because of NXP-19466
    // but works because null value is wrongly recognized as phantom so reinit with default value
    public void testScalarCreatedWithDefaultValue() throws Exception {
        // given a doc saved with a property with a default value not modified
        DocumentModel doc = session.createDocumentModel("/", "doc1", "DocDefaultValue");
        session.createDocument(doc);
        session.save();

        // when I get it
        doc = session.getDocument(doc.getRef());

        // then the default value must be set
        assertNull(doc.getPropertyValue("dv:simpleWithoutDefault"));
        assertEquals("value", doc.getPropertyValue("dv:simpleWithDefault"));
    }

    @Test
    @Ignore
    // FIXME: NXP-19466 - default value lifecycle is not correctly managed
    public void testScalarSetOnNullDontSetDefaultValueAgain() throws Exception {
        // given a doc saved with a property with a default value updated
        DocumentModel doc = session.createDocumentModel("/", "doc1", "DocDefaultValue");
        doc.setPropertyValue("dv:simpleWithDefault", "newValue");
        session.createDocument(doc);
        session.save();

        // when I get the doc and set the value to null
        doc = session.getDocument(doc.getRef());
        doc.setPropertyValue("dv:simpleWithDefault", null);
        session.saveDocument(doc);
        session.save();

        // then the property should remain null
        doc = session.getDocument(doc.getRef());
        assertNull(doc.getPropertyValue("dv:simpleWithDefault"));
    }

    @Test
    @Ignore
    // FIXME: NXP-19466 - default value lifecycle is not correctly managed
    public void testMultiCreatedWithDefaultValue() throws Exception {
        // given a doc saved with a property with a default value not modified
        DocumentModel doc = session.createDocumentModel("/", "doc1", "DocDefaultValue");
        session.createDocument(doc);
        session.save();

        // when I get it
        doc = session.getDocument(doc.getRef());

        // then the default value must be set
        assertNull(doc.getPropertyValue("dv:multiWithoutDefault"));
        assertEquals(Arrays.asList("value1", "value2"), doc.getPropertyValue("dv:multiWithDefault"));
    }

    @Test
    @Ignore
    // FIXME: NXP-19466 - default value lifecycle is not correctly managed
    public void testMultiSetOnNullDontSetDefaultValueAgain() throws Exception {
        // given a doc saved with a property with a default value updated
        DocumentModel doc = session.createDocumentModel("/", "doc1", "DocDefaultValue");
        doc.setPropertyValue("dv:multiWithDefault", new String[] { "newValue1", "newValue2" });
        session.createDocument(doc);
        session.save();

        // when I get the doc and set the value to null
        doc = session.getDocument(doc.getRef());
        doc.setPropertyValue("dv:multiWithDefault", null);
        session.saveDocument(doc);
        session.save();

        // then the property should remain null
        doc = session.getDocument(doc.getRef());
        assertNull(doc.getPropertyValue("dv:multiWithDefault"));
    }

}