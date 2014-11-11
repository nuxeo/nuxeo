/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: AbstractWorkflowDocumentManager.java 28456 2008-01-03 12:01:11Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionSynchronization;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.document.api.BaseWorkflowDocumentManager;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.CoreDocumentManagerBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;

/**
 * Abstract workflow document manager.
 * <p>
 * This abstract class will deal with JTA session resources synchronization. In
 * this case the core document manager needs to be opened and closed at the
 * beginning and end of every transaction.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractWorkflowDocumentManager implements
        SessionSynchronization, BaseWorkflowDocumentManager {

    private static final Log log = LogFactory.getLog(AbstractWorkflowDocumentManager.class);

    private static final long serialVersionUID = -7670238533651462575L;

    protected String repositoryUri;

    protected final CoreDocumentManagerBusinessDelegate documentManagerBusinessDelegate;

    protected final WorkflowDocumentSecurityPolicyBusinessDelegate wDocRightsPolicyBusinessDelegate;

    protected transient CoreSession documentManager;

    protected AbstractWorkflowDocumentManager() {
        documentManagerBusinessDelegate = new CoreDocumentManagerBusinessDelegate();
        wDocRightsPolicyBusinessDelegate = new WorkflowDocumentSecurityPolicyBusinessDelegate();
    }

    public void afterBegin() throws EJBException {
        try {
            log.trace("Connect workflow document manager");
            documentManager = getDocumentManager();
        } catch (NamingException e) {
            throw new EJBException(e);
        } catch (ClientException e) {
            throw new EJBException(e);
        }
    }

    public void afterCompletion(boolean committed) throws EJBException {
        try {
            if (documentManager != null) {
                log.trace("Disconnect workflow document manager");
                documentManager.disconnect();
                documentManager = null;
            }
        } catch (ClientException e) {
            throw new EJBException(e);
        }
    }

    public void beforeCompletion() throws EJBException, RemoteException {
        // TODO Auto-generated method stub
    }

    protected CoreSession getDocumentManager() throws NamingException,
            ClientException {
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

        try {
            documentManager = getDocumentManager();
        } catch (NamingException e) {
            throw new ClientException(e.getMessage());
        }

        if (docRef != null) {
            documentManager.unlock(docRef);
            documentManager.save();
            log.debug("Document has been unlocked.... docRef=" + docRef);
        }

    }

    public DocumentModel getDocumentModelFor(DocumentRef docRef)
            throws ClientException {
        DocumentModel dm;
        try {
            dm = getDocumentManager().getDocument(docRef);
        } catch (NamingException e) {
            throw new ClientException(e);
        }
        return dm;
    }

    public String getRepositoryUri() {
        return repositoryUri;
    }

    public void setRepositoryUri(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

}
