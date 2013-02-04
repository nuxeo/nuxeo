package org.nuxeo.ecm.platform.groups.audit.service.rendering;

import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class AclExtractor {
    private static final Log log = LogFactory.getLog(AclExtractor.class);

    /**
     * Return a compact version of a document ACLs, e.g.:
     * <ul>
     * <li>user1 -> [(READ,true), (WRITE,false), (ADD_CHILDREN,false), ...]
     * <li>user2 -> [(READ,true), (WRITE,true), (ADD_CHILDREN,true), ...]
     * <li>
     * </ul>
     * @param doc
     * @return
     * @throws ClientException
     */
	public Multimap<String,Pair<String,Boolean>> getAclByUser(DocumentModel doc) throws ClientException{
		Multimap<String,Pair<String,Boolean>> aclByUser = HashMultimap.create();

		ACP acp = doc.getACP();
		ACL[] acls = acp.getACLs();

		for (ACL acl : acls) {
			for (ACE ace : acl.getACEs()) {
				String userOrGroup = ace.getUsername();
				String permission = ace.getPermission();
				boolean allow = ace.isGranted();
				Pair<String,Boolean> pair = Pair.of(permission, allow);
				aclByUser.put(userOrGroup, pair);

				if(ace.isGranted() && ace.isDenied())
					log.warn("stupid state: ace granted and denied at the same time. Considered granted");
			}
		}
		return aclByUser;
	}

	public boolean hasLockInheritanceACE(DocumentModel doc) throws ClientException{
		ACP acp = doc.getACP();
		ACL[] acls = acp.getACLs();

		for (ACL acl : acls) {
			for (ACE ace : acl.getACEs()) {
				if(isLockInheritance(ace))
					return true;
			}
		}
		return false;
	}

	public boolean isLockInheritance(ACE ace){
		return (SecurityConstants.EVERYONE.equals(ace.getUsername())
				&& SecurityConstants.EVERYTHING.equals(ace.getPermission())
				&& ace.isDenied()
				);
	}

	public Pair<HashSet<String>, HashSet<String>> getAclSummary(
			DocumentModel doc) throws ClientException {
		Pair<HashSet<String>, HashSet<String>> summary = newSummary();
		ACP acp = doc.getACP();
		ACL[] acls = acp.getACLs();

		for (ACL acl : acls) {
			for (ACE ace : acl.getACEs()) {
				String userOrGroup = ace.getUsername();
				String permission = ace.getPermission();
				summary.getLeft().add(userOrGroup);
				summary.getRight().add(permission);
			}
		}
		return summary;
	}

	protected Pair<HashSet<String>, HashSet<String>> newSummary() {
		return Pair.of(new HashSet<String>(), new HashSet<String>());
	}
}
