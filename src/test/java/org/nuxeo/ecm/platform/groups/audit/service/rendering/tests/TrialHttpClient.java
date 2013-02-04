package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;

public class TrialHttpClient {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		HttpAutomationClient client = new HttpAutomationClient(
				"http://localhost:8080/nuxeo/site/automation");

		Session session = client.getSession("Administrator", "Administrator");

		Documents docs = getAll(session);

		for(Document d: docs){
			System.out.println(d.getPath());
			System.out.println(d.getProperties());

			Documents children = getChildren(session, d);

			for(Document child: children){
				System.out.println(child.getPath());
			}
		}

		client.shutdown();
	}

	protected static Document getRoot(Session session) throws Exception{
		return (Document) session.newRequest("Document.Fetch").set("value", "/").execute();
	}

	protected static Documents getChildren(Session session, Document d) throws Exception{
		return (Documents) session.newRequest("Document.GetChildren").setInput(d).execute();
	}

	protected static Documents getAll(Session session) throws Exception{
		return (Documents) session.newRequest("Document.Query").set("query", "SELECT * FROM Document").execute();
	}

	protected static Documents getByPath(Session session, String path) throws Exception{
		return (Documents) session.newRequest("Document.Query").set("query", "SELECT * FROM Document WHERE ecm:path = '" + path + "'").execute();
	}
}
