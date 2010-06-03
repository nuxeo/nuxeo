package org.nuxeo.ecm.platform.publisher.impl.service;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestDomainsFinder extends SQLRepositoryTestCase {

	DomainsFinder domainFinder;
	List<DocumentModel> result;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		deployContrib("org.nuxeo.ecm.platform.publisher.test", "OSGI-INF/publisher-lifecycle-contrib.xml");

        openSession();

        domainFinder = new DomainsFinderTester("default", session);
	}

	public void testDomainsFiltered() throws Exception {

//		result = domainFinder.getDomainsFiltered();
//		assertEquals(0, result.size());
//
//        DocumentModel domain1 = session.createDocumentModel("/",
//                "dom1", "Domain");
//        domain1 = session.createDocument(domain1);
//        DocumentModel domain2 = session.createDocumentModel("/",
//                "dom1", "Domain");
//        domain2 = session.createDocument(domain1);
//        session.save();
//		result = domainFinder.getDomainsFiltered();
//		assertEquals(2, result.size());
//
//
//		domain2.followTransition("delete");
//		assertEquals("deleted", domain2.getCurrentLifeCycleState());
//		session.saveDocument(domain2);
//		session.save();
//		result = domainFinder.getDomainsFiltered();
//		assertEquals(1, result.size());


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
