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
 * $Id$
 */

package org.nuxeo.ecm.core.api.ejb;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.ejb.local.DocumentManagerLocal;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;

/**
 * The implementation for {@link CoreSession} interface. This class and its
 * parents mostly delegate the calls to the {@link AbstractSession} and its
 * children.
 * <p>
 * This class and its children are also the points where security is
 * attached/checked.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Stateful
@Local(DocumentManagerLocal.class)
@Remote(CoreSession.class)
// @Interceptors(DocumentParameterInterceptor.class)
@SerializedConcurrentAccess
public class DocumentManagerBean extends AbstractSession implements SessionSynchronization {

    private static final long serialVersionUID = 6781675353273516393L;

    private static final Log log = LogFactory.getLog(DocumentManagerBean.class);

    private static final String CONTEXT_PRINCIPAL_KEY = "principal";

    // also need to be transient (not annot only) - see error with
    // org.jaxon.VariableContext
    @Transient
    protected transient Session session;

    @Resource
    transient EJBContext context;

    @Override
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("@Remove");
        //super.destroy();
    }

    @PreDestroy
    /**
     * This method is called before the stateful bean instance is destroyed.
     * <p>
     * When a client is explicitly destroying a bean using the @Remove method this method will be automatically
     * called before the instance is destroyed
     */
    public void preDestroy() {
        log.debug("@PreDestroy");
        super.destroy();
    }

    // @PostConstruct
    // protected void create() {
    // log.debug("@PostConstructor");
    // }

    @PostActivate
    public void readState() {
        log.debug("@PostActivate");
        CoreInstance.getInstance().registerSession(getSessionId(), this);
    }

    @PrePassivate
    public void prePassivate() {
        log.debug("@Prepassivate");
        try {
            // close the session?
            if (session != null && session.isLive()) {
                session.close();
            }
            session = null;
        } catch (Exception e) {
            log.error("Failed to close session", e);
        }
    }

    @Override
    public Principal getPrincipal() {
        Principal principal;
        try {
            if (sessionContext == null) {
                sessionContext = getSession().getSessionContext();
            }
            // The connection might have been initialized with another principal
            // than the caller principal. If this is the case, then the
            // principal
            // id will be within the session context properties.
            principal = (Principal) sessionContext.get(CONTEXT_PRINCIPAL_KEY);
            if (principal == null) {
                principal = context.getCallerPrincipal();
                sessionContext.put(CONTEXT_PRINCIPAL_KEY, (Serializable) principal);
            }
        } catch (Throwable t) {
            // TODO: don't throw the exception for the moment, as we need to
            // change the API
            // exceptionHandler.wrapException(t);
            return null;
        }

        assert principal != null;
        return principal;
    }

    @Override
    public String connect(String repositoryName,
            Map<String, Serializable> sessionContext) throws ClientException {
        try {
            log.debug("URI: " + repositoryName);

            // store the principal in the core session context so that other
            // core tools may retrieve it
            if (sessionContext == null) {
                sessionContext = new HashMap<String, Serializable>();
            }

            if (!sessionContext.containsKey(CONTEXT_PRINCIPAL_KEY)) {
                log.debug("Add caller principal to the session context....");
                sessionContext.put(CONTEXT_PRINCIPAL_KEY,
                        (Serializable) context.getCallerPrincipal());
            } else {
                log.debug("Principal already within the session context...");
            }
            return super.connect(repositoryName, sessionContext);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    // session handling code

    @Override
    protected synchronized Session getSession() throws ClientException {
        assert repositoryName != null;
        // make sure we don't reuse closed sessions
        if (session == null || !session.isLive()) {
            log.debug("Initializing session for repository: " + repositoryName);
            try {
                NXCore.getRepository(repositoryName);
            } catch (Exception e) {
                throw ClientException.wrap(e);
            }
            try {
                session = createSession(repositoryName, "default",
                        sessionContext);
            } catch (Exception e) {
                throw ClientException.wrap(e);
            }
        }
        return session;
    }

    protected Session createSession(String repoName, String ws,
            Map<String, Serializable> context) throws DocumentException,
            NoSuchRepositoryException {
        Repository repo = NXCore.getRepository(repoName);
        return repo.getSession(context);
    }

    // ----------------------- cache interceptors ------------

    @Override
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    public DocumentModel getDocument(DocumentRef docRef) throws ClientException {
        try {
            return super.getDocument(docRef);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    public DocumentModel getDocument(DocumentRef docRef, String[] schemas)
            throws ClientException {
        try {
            return super.getDocument(docRef, schemas);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    public DocumentModel getChild(DocumentRef parent, String name)
            throws ClientException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("UUID: " + parent.toString() + ", name: " + name);
            }
            return super.getChild(parent, name);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    public DocumentModelList getChildren(DocumentRef parent)
            throws ClientException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("UUID: " + parent.toString());
            }
            return super.getChildren(parent);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    public DocumentModelList getChildren(DocumentRef parent, String type)
            throws ClientException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("UUID: " + parent.toString() + " type: " + type);
            }
            return super.getChildren(parent, type);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    public DocumentModelList getChildren(DocumentRef parent, String type,
            String perm) throws ClientException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("UUID: " + parent.toString() + " type: " + type);
            }
            return super.getChildren(parent, type, perm);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public boolean isSessionAlive() {
        return session != null && session.isLive();
    }

    @Override
    public List<SecuritySummaryEntry> getSecuritySummary(
            DocumentModel docModel, Boolean includeParents)
            throws ClientException {
        try {
            return super.getSecuritySummary(docModel, includeParents);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }


    public void afterBegin() throws EJBException, RemoteException {
//        System.out.println("# "+Thread.currentThread().getId()+ " #### TRANSACTION STARTED: ");
//        if (log.isDebugEnabled()) {
//            log.debug("Transaction started");
//        }
//        CoreEventListenerService service = NXCore.getCoreEventListenerService();
//        if (service != null) {
//            service.transactionStarted();
//        }
    }

    public void beforeCompletion() throws EJBException, RemoteException {
//        System.out.println("# "+Thread.currentThread().getId()+ " #### TRANSACTION ABOUT TO COMMIT");
//        if (log.isDebugEnabled()) {
//            log.debug("Transaction about to commit");
//        }
//        CoreEventListenerService service = NXCore.getCoreEventListenerService();
//        if (service != null) {
//            service.transactionAboutToCommit();
//        }
    }

    public void afterCompletion(boolean committed) throws EJBException,
            RemoteException {
//        System.out.println("# "+Thread.currentThread().getId()+ " #### TRANSACTION COMMITTED: "+committed);
//        if (log.isDebugEnabled()) {
//            log.debug("Transaction "+(committed ? "committed" : "rollbacked"));
//        }
//        CoreEventListenerService service = NXCore.getCoreEventListenerService();
//        if (service != null) {
//            if (committed) {
//                service.transactionCommited();
//            } else {
//                service.transactionRollbacked();
//            }
//        }
    }

}
