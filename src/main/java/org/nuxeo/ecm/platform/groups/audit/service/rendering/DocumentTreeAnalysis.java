package org.nuxeo.ecm.platform.groups.audit.service.rendering;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Gather various data and statistics about a document tree
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class DocumentTreeAnalysis {
	protected int documentTreeDepth;
	protected Set<String> userAndGroups;
	protected Set<String> permissions;

	protected AclExtractor acl = new AclExtractor();

	public void init() {
		userAndGroups = new HashSet<String>();
		permissions = new HashSet<String>();
		documentTreeDepth = 0;
	}

	public void analyze(CoreSession session) throws ClientException {
		analyze(session, session.getRootDocument());
	}

	/**
	 * Analyze recursively the document tree.
	 *
	 * After calling this method, on can retrieve:
	 * <ul>
	 * <li>the tree depth
	 * <li>all user and groups mentioned in the documents' ACLs
	 * <li>all permission names mentioned in the documents' ACLs
	 * </ul>
	 *
	 * Note that root is considered as a document, so a repository made of:
	 * <code>
	 * <pre>
	 *  /
	 *  |-folder1
	 *  |-folder2
	 * </pre>
	 * </code>
	 * has a depth of 2.
     *
	 *
	 * Once called, the method erase previous results.
	 *
	 * @param session
	 * @param doc
	 * @throws ClientException
	 */
	public void analyze(CoreSession session, DocumentModel doc)
			throws ClientException {
		init();
		doAnalyze(session, doc, 0);
	}

	protected void doAnalyze(CoreSession session, DocumentModel doc, int depth)
			throws ClientException {
		// report all found groups or user names as well as permissions
		Pair<HashSet<String>, HashSet<String>> s = acl.getAclSummary(doc);
		userAndGroups.addAll(s.getLeft());
		permissions.addAll(s.getRight());

		// report maximal doc tree depth
		int docDepth = depth + 1;
		if (docDepth > documentTreeDepth)
			documentTreeDepth = docDepth;

		// continue working recursively
		DocumentModelList list = session.getChildren(doc.getRef());
		for (DocumentModel child : list) {
			doAnalyze(session, child, docDepth);
		}
	}

	/* RESULTS */

	public Set<String> getUserAndGroups() {
		return userAndGroups;
	}

	public Set<String> getPermissions() {
		return permissions;
	}

	/**
	 * Return the tree depth.
	 *
	 * Note that root is considered as a document, so a repository made of
	 * <pre>
	 *  /
	 *   folder1
	 *   folder2
	 * </pre>
	 * has a depth of 2.
	 */
	public int getDocumentTreeDepth() {
		return documentTreeDepth;
	}
}
