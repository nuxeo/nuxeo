/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.impl.blob;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestMediaType extends NXRuntimeTestCase {

    public static final String SCHEMA_NAME = "media";

    public static final String SCHEMA_PREFIX = "media";

    protected SchemaManager typeMgr;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-media-types-contrib.xml");
        typeMgr = Framework.getService(SchemaManager.class);
    }

    // shema name != prefix name
    @Test
    public void testDifferentPrefix() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "mediaDoc", "Media");
        doc.setPropertyValue("media:title", "Media Title");
        Object o1 = doc.getProperty("media:title");
        Object o2 = doc.getProperty("media:/title");
        assertEquals(o1, o2);

        // using the prefix
        o2 = doc.getProperty("m:title");
        assertEquals(o1, o2);

        o2 = doc.getProperty("m:/title");
        assertEquals(o1, o2);
    }

    // shema name = prefix name
    @Test
    public void testSamePrefix() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "mediaDoc", "SameMedia");
        doc.setPropertyValue("sameMedia:title", "Media Title");
        Object o1 = doc.getProperty("sameMedia:title");
        Object o2 = doc.getProperty("sameMedia:/title");
        assertEquals(o1, o2);
    }

}
