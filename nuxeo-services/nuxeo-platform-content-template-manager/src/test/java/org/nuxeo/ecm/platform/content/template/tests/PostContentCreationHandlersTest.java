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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.content.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.content.template.tests:test-content-template-handlers-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.content.template.tests:test-content-template-handlers-contrib.xml")
public class PostContentCreationHandlersTest {

    @Inject
    protected ContentTemplateService service;

    @Inject
    protected CoreSession session;

    @Test
    public void testHandler() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModelList rootChildren = session.getChildren(root.getRef());
        assertEquals(2, rootChildren.size());

        // query result order is not fixed
        if (!"default-domain".equals(rootChildren.get(0).getName())) {
            Collections.reverse(rootChildren);
        }
        assertEquals("default-domain", rootChildren.get(0).getName());

        DocumentModel child = rootChildren.get(1);
        assertNotNull(child);
        assertEquals(SimplePostContentCreationHandler.DOC_NAME, child.getName());
        assertEquals(SimplePostContentCreationHandler.DOC_TYPE, child.getType());
    }

}
