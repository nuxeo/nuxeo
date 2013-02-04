package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;

public class AbstractAclLayoutTest {

	public AbstractAclLayoutTest() {
		super();
	}

	protected DocumentModel makeFolder(CoreSession session, String path,
			String name) throws ClientException, PropertyException {
		return makeItem(session, path, name, "Folder");
	}

	protected DocumentModel makeDoc(CoreSession session, String path,
			String name) throws ClientException, PropertyException {
		return makeItem(session, path, name, "Document");
	}

	protected DocumentModel makeItem(CoreSession session, String path,
			String name, String type) throws ClientException, PropertyException {
		DocumentModel folder = session.createDocumentModel(path, name, type);
		folder = session.createDocument(folder);
		session.saveDocument(folder);
		return folder;
	}

	protected DocumentModel makeGroup(UserManager userManager, String groupId)
			throws Exception {
		DocumentModel newGroup = userManager.getBareGroupModel();
		newGroup.setProperty("group", "groupname", groupId);
		return newGroup;
	}

	protected DocumentModel makeUser(UserManager userManager, String userId)
			throws Exception {
		DocumentModel newUser = userManager.getBareUserModel();
		newUser.setProperty("user", "username", userId);
		return newUser;
	}

	protected void addAcl(CoreSession session, DocumentModel doc,
			String userOrGroup, String right, boolean allow)
			throws ClientException {
		addAcl(session, doc, userOrGroup, right, allow, false);
	}

	protected void addAcl(CoreSession session, DocumentModel doc,
			String userOrGroup, String right, boolean allow,
			boolean blockInheritance) throws ClientException {
		ACP acp = doc.getACP();
		ACL acl = acp.getOrCreateACL();// local
		acl.add(new ACE(userOrGroup, right, allow));
		doc.setACP(acp, true);
		session.saveDocument(doc);
	}

	protected void addAclLockInheritance(CoreSession session,
			DocumentModel doc, String userOrGroup)
			throws ClientException {
		ACP acp = doc.getACP();
		ACL acl = acp.getOrCreateACL();// local
		acl.add(new ACE(SecurityConstants.EVERYONE,
				SecurityConstants.EVERYTHING, false));
		doc.setACP(acp, true);
		session.saveDocument(doc);
	}
}