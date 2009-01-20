/*
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: WorkflowDocumentSecurityBean.java 20781 2007-06-19 05:58:22Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserAccess;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.CoreDocumentManagerBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkflowDocumentSecurityLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkflowDocumentSecurityRemote;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityException;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;

/**
 * Workflow security manager bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(WorkflowDocumentSecurityLocal.class)
@Remote(WorkflowDocumentSecurityRemote.class)
public class WorkflowDocumentSecurityBean implements
        WorkflowDocumentSecurityManager {

    private static final long serialVersionUID = 4800384487274281699L;

    private static final Log log = LogFactory.getLog(WorkflowDocumentSecurityBean.class);

    protected String repositoryUri;

    protected final CoreDocumentManagerBusinessDelegate documentManagerBusinessDelegate;

    protected final WorkflowDocumentSecurityPolicyBusinessDelegate wDocRightsPolicyBusinessDelegate;

    protected transient CoreSession documentManager;

    public WorkflowDocumentSecurityBean() {
        documentManagerBusinessDelegate = new CoreDocumentManagerBusinessDelegate();
        wDocRightsPolicyBusinessDelegate = new WorkflowDocumentSecurityPolicyBusinessDelegate();
    }

    public WorkflowDocumentSecurityBean(String repositoryUri) {
        this();
        this.repositoryUri = repositoryUri;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            documentManager = getDocumentManager();
        } catch (ClientException e) {
            log.error(e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        try {
            documentManager.disconnect();
        } catch (ClientException e) {
            log.error(e);
        }
    }

    protected CoreSession getDocumentManager() throws ClientException {
        if (documentManager == null) {
            documentManager = documentManagerBusinessDelegate.getDocumentManager(
                    repositoryUri, null);
        }
        return documentManager;
    }

    protected WorkflowDocumentSecurityPolicyManager getWorkflowDocumentRightsPolicy()
            throws Exception {
        return wDocRightsPolicyBusinessDelegate.getWorkflowDocumentRightsPolicyManager();
    }

    public void unlockDocument(DocumentRef docRef) throws ClientException {
        documentManager = getDocumentManager();
        if (docRef != null) {
            documentManager.unlock(docRef);
            documentManager.save();
            log.debug("Document has been unlocked.... docRef=" + docRef);
        }
    }

    public DocumentModel getDocumentModelFor(DocumentRef docRef)
            throws ClientException {
        DocumentModel dm;
        dm = getDocumentManager().getDocument(docRef);
        return dm;
    }

    public String getRepositoryUri() {
        return repositoryUri;
    }

    public void setRepositoryUri(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

    protected ACP getACP(DocumentRef docRef) throws ClientException {
        CoreSession docManager = getDocumentManager();
        return docManager.getACP(docRef);
    }

    public ACL getACL(DocumentRef docRef, String pid)
            throws WorkflowDocumentSecurityException {

        ACP acp;
        try {
            acp = getACP(docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentSecurityException(ce);
        }

        String aclName = getACLNameFor(pid);
        ACL acl = null;
        if (acp != null) {
            acl = acp.getACL(aclName);
        }

        return acl;
    }

    public String getACLNameFor(String pid) {
        return "workflow_" + pid;
    }

    public void grantPrincipal(DocumentRef docRef, String principalName,
            String perm, String pid) throws WorkflowDocumentSecurityException {

        if (principalName == null) {
            throw new WorkflowDocumentSecurityException(
                    "Principal name cannot be null");
        }

        ACP docACP;
        try {
            docACP = getACP(docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentSecurityException(ce);
        }

        if (docACP == null) {
            docACP = new ACPImpl();
        }

        UserEntry userEntry = new UserEntryImpl(principalName);
        userEntry.addPrivilege(perm, true, false);
        List<UserEntry> userEntries = new ArrayList<UserEntry>();
        userEntries.add(userEntry);
        docACP.setRules(getACLNameFor(pid),
                userEntries.toArray(new UserEntry[userEntries.size()]));

        try {
            CoreSession docManager = getDocumentManager();
            docManager.setACP(docRef, docACP, true);
            docManager.save();
            log.debug("Modify acp, granting : " + principalName);
        } catch (SecurityException se) {
            throw new WorkflowDocumentSecurityException(se);
        } catch (ClientException ce) {
            throw new WorkflowDocumentSecurityException(ce);
        }
    }

    public void denyPrincipal(DocumentRef docRef, String principalName,
            String perm, String pid) throws WorkflowDocumentSecurityException {

        if (principalName == null) {
            throw new WorkflowDocumentSecurityException(
                    "Principal name cannot be null");
        }

        ACP docACP;
        try {
            docACP = getACP(docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentSecurityException(ce);
        }

        if (docACP != null) {
            ACL acl = getACL(docRef, pid);
            if (acl != null) {
                boolean updated = false;
                for (ACE ace : acl.getACEs()) {
                    if (ace.getUsername().equals(principalName)
                            && ace.getPermission().equals(
                                    SecurityConstants.WRITE_LIFE_CYCLE)) {
                        log.debug("ACE removal.......");
                        acl.remove(ace);
                        updated = true;
                    }
                }
                if (updated) {
                    // override existing ACLs
                    docACP.addACL(0, acl);
                    try {
                        CoreSession docManager = getDocumentManager();
                        docManager.setACP(docRef, docACP, true);
                        docManager.save();
                        log.debug("participantName=" + principalName);
                    } catch (SecurityException se) {
                        throw new WorkflowDocumentSecurityException(se);
                    } catch (ClientException ce) {
                        throw new WorkflowDocumentSecurityException(ce);
                    }
                }
            }
        }
    }

    public void removeACL(DocumentRef docRef, String pid)
            throws WorkflowDocumentSecurityException {
        ACL acl = getACL(docRef, pid);
        if (acl != null) {
            try {
                ACP docACP = getACP(docRef);
                docACP.removeACL(acl.getName());
                CoreSession docManager = getDocumentManager();
                docManager.setACP(docRef, docACP, true);
                docManager.save();
                log.debug("Removing wf acp.");
            } catch (ClientException ce) {
                throw new WorkflowDocumentSecurityException(ce);
            }
        }
    }

    public void setRules(DocumentRef docRef, List<UserEntry> userEntries,
            String pid) throws WorkflowDocumentSecurityException {

        // Remove the ACL first
        removeACL(docRef, pid);

        ACP docACP;
        try {
            docACP = getACP(docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentSecurityException(ce);
        }

        if (docACP == null) {
            docACP = new ACPImpl();
        }

        ACL acl = docACP.getACL(getACLNameFor(pid));
        if (acl == null) {
            acl = new ACLImpl(getACLNameFor(pid));
            docACP.addACL(0, acl);
        }
        for (UserEntry entry : userEntries) {
            for (String permission : entry.getPermissions()) {
                UserAccess userAccess = entry.getAccess(permission);
                if (userAccess.isReadOnly()) {
                    continue; // avoid setting read only rules
                }
                ACE ace = new ACE(entry.getUserName(), permission,
                        userAccess.isGranted());
                acl.add(ace);
            }
        }

        try {
            CoreSession docManager = getDocumentManager();
            docManager.setACP(docRef, docACP, true);
            docManager.save();
            log.debug("Saving wf acp.");
        } catch (SecurityException se) {
            throw new WorkflowDocumentSecurityException(se);
        } catch (ClientException ce) {
            throw new WorkflowDocumentSecurityException(ce);
        }
    }

}
