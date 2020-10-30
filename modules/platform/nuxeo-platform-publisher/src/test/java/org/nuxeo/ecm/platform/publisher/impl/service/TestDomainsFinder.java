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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.publisher.impl.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.publisher")
@Deploy("org.nuxeo.ecm.platform.publisher.test:OSGI-INF/core-types-contrib.xml")
public class TestDomainsFinder {

    @Inject
    protected CoreSession session;

    @Inject
    protected TrashService trashService;

    DomainsFinder domainFinder;

    @Before
    public void setUp() throws Exception {
        domainFinder = new DomainsFinderTester("default", session);
    }

    @Test
    public void testDomainsFiltered() throws Exception {
        List<DocumentModel> result = domainFinder.getDomainsFiltered();
        assertEquals(0, result.size());

        DocumentModel domain1 = session.createDocumentModel("/", "dom1", "Domain");
        domain1 = session.createDocument(domain1);
        DocumentModel domain2 = session.createDocumentModel("/", "dom1", "Domain");
        domain2 = session.createDocument(domain2);
        DocumentModel socialdomain1 = session.createDocumentModel("/", "social", "SocialDomain");
        socialdomain1 = session.createDocument(socialdomain1);
        session.save();
        result = domainFinder.getDomainsFiltered();
        assertEquals(2, result.size());

        trashService.trashDocument(domain2);
        // Fetch the document again as it could have been moved by the trash service
        domain2 = session.getDocument(domain2.getRef());
        assertTrue(domain2.isTrashed());
        session.saveDocument(domain2);
        session.save();
        result = domainFinder.getDomainsFiltered();
        assertEquals(1, result.size());
    }

}

class DomainsFinderTester extends DomainsFinder {

    public DomainsFinderTester(String repositoryName) {
        super(repositoryName);
    }

    public DomainsFinderTester(String repositoryName, CoreSession session) {
        super(repositoryName);
        this.session = session;
    }
}
