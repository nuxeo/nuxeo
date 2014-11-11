/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.content.template.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy("org.nuxeo.ecm.platform.content.template")
@LocalDeploy("org.nuxeo.ecm.platform.content.template:OSGI-INF/multiple-domains-content-template-contrib.xml")
public class MultipleDomainsCreationTest {

    @Inject
    CoreSession session;

    @Test
    public void threeDomainsCreated() throws Exception {
        assertNotNull(session);
        assertNotNull(session.getDocument(new PathRef("/domain1")));
        assertNotNull(session.getDocument(new PathRef("/domain2")));
        assertNotNull(session.getDocument(new PathRef("/domain3")));

        assertEquals(3,
                session.getChildren(session.getRootDocument().getRef()).size());
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
