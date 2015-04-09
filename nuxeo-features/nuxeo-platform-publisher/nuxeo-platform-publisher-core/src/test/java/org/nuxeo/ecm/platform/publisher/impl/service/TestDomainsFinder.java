/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.publisher.impl.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.platform.publisher.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.publisher.test:OSGI-INF/core-types-contrib.xml")
public class TestDomainsFinder {

    @Inject
    protected CoreSession session;

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

        domain2.followTransition("delete");
        assertEquals("deleted", domain2.getCurrentLifeCycleState());
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
