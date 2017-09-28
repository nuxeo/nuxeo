/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class for tag service based on facet
 *
 * @since 9.3
 */
@LocalDeploy("org.nuxeo.ecm.platform.tag:faceted-tag-service-override.xml")
public class TestFacetedTagService extends AbstractTestTagService {

    @Override
    protected void createTags() {
        DocumentModel file1 = session.getDocument(new PathRef("/file1"));
        DocumentModel file2 = session.getDocument(new PathRef("/file2"));

        Map<String, Serializable> tag1 = new HashMap<>();
        tag1.put("label", "tag1");
        tag1.put("username", "Administrator");

        Map<String, Serializable> tag2 = new HashMap<>();
        tag2.put("label", "tag2");
        tag2.put("username", "Administrator");

        file1.setPropertyValue("nxtag:tags", (Serializable) Arrays.asList(tag1, tag2));
        file2.setPropertyValue("nxtag:tags", (Serializable) Arrays.asList(tag1));

        session.saveDocument(file1);
        session.saveDocument(file2);
        session.save();
    }
}
