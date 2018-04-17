/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.content.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.content.template:OSGI-INF/multiple-domains-content-template-contrib.xml")
public class MultipleDomainsCreationTest {

    @Inject
    CoreSession session;

    @Test
    public void threeDomainsCreated() throws Exception {
        assertNotNull(session);
        assertNotNull(session.getDocument(new PathRef("/domain1")));
        assertNotNull(session.getDocument(new PathRef("/domain2")));
        assertNotNull(session.getDocument(new PathRef("/domain3")));

        assertEquals(3, session.getChildren(session.getRootDocument().getRef()).size());
    }

    @Test
    public void testMerge() throws Exception {
        assertNotNull(session);
        DocumentModel domain = session.getDocument(new PathRef("/domain3"));
        assertNotNull(domain);

        // DomainFactory should be merged with DomainFactory2
        assertEquals(4, session.getChildren(domain.getRef()).size());
        assertEquals(4, session.getACP(domain.getRef()).getACLs()[0].getACEs().length);

        // FolderFactory1 should be overridden by FolderFactory2
        DocumentModel folder = session.getDocument(new PathRef("/domain3/fd1"));
        assertNotNull(folder);
        assertEquals(2, session.getChildren(folder.getRef()).size());
    }

}
