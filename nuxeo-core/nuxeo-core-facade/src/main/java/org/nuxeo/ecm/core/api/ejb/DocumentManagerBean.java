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
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJBContext;
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
import org.jboss.ejb3.annotation.SecurityDomain;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CallerPrincipalProvider;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.RollbackClientException;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.ACP;
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
 */
@Stateful
@Local(DocumentManagerLocal.class)
@Remote(CoreSession.class)
@SecurityDomain("nuxeo-ecm")
@Interceptors({ ReentrantCallsShieldInterceptor.class })
public class DocumentManagerBean extends AbstractSession implements
        DocumentManagerLocal, SessionSynchronization {

    private static final long serialVersionUID = 6781675353273516393L;

    private static final Log log = LogFactory.getLog(DocumentManagerBean.class);

    private static final String CONTEXT_PRINCIPAL_KEY = "principal";

    private Boolean supportsTags = null;

    // also need to be transient (not annot only) - see error with
    // org.jaxon.VariableContext
    @Transient
    protected transient Session session;

    // Because of NXP-5782 this field can not be made transient
    // otherwise it will break after a passivation
    // => we will fetch the caller Principal only the first time
    @Resource
    protected EJBContext context;

    protected Principal callerPrincipal = null;

    @Override
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("@Remove");
        // super.destroy();
    }

    @PreDestroy
    /*
     * This method is called before the stateful bean instance is destroyed. <p>
     * When a client is explicitly destroying a bean using the @Remove method
     * this method will be automatically called before the instance is destroyed
     */
    public void preDestroy() {
        log.debug("@PreDestroy");
        super.destroy();
    }

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
            // why not unregister the session?
            session = null;
        } catch (Exception e) {
            log.error("Failed to close session", e);
        }
    }

    /**
     * Allays use this method to retrieve the caller principal from the context.
     * This allows fixing a problem on JBoss5.
     *
     * @see CallerPrincipalProvider
     * @return
     */
    protected Principal getCallerPrincipal() {
        if (callerPrincipal == null) {
            callerPrincipal = context.getCallerPrincipal();
        }
        if (!(callerPrincipal instanceof NuxeoPrincipal)) {
            NuxeoPrincipal np = CallerPrincipalProvider.getInstance().getCallerPrincipal();
            if (np != null) {
                callerPrincipal = np;
            }
        }
        return callerPrincipal;
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
                principal = getCallerPrincipal();
                sessionContext.put(CONTEXT_PRINCIPAL_KEY,
                        (Serializable) principal);
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
                        (Serializable) getCallerPrincipal());
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
    public synchronized Session getSession() throws ClientException {
        assert repositoryName != null;
        // make sure we don't reuse closed sessions
        if (session == null || !session.isLive()) {
            log.debug("Initializing session for repository: " + repositoryName);
            try {
                session = createSession(repositoryName, "default",
                        sessionContext);
            } catch (Exception e) {
                throw ClientException.wrap(e);
            }
        }
        return session;
    }

    @Override
    public boolean supportsTags(String repositoryName) throws ClientException {
        try {
            Repository repo = NXCore.getRepository(repositoryName);
            return repo.supportsTags();
        } catch (Exception e) {
            throw new ClientException("Failed to load repository "
                    + repositoryName, e);
        }
    }

    @Override
    public boolean supportsTags() throws ClientException {
        if (supportsTags != null) {
            return supportsTags.booleanValue();
        }
        throw new ClientException("Can not query on a closed repository");
    }

    protected Session createSession(String repoName, String ws,
            Map<String, Serializable> context) throws DocumentException,
            NoSuchRepositoryException {
        Repository repo = NXCore.getRepository(repoName);
        supportsTags = repo.supportsTags();
        return repo.getSession(context);
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        // we have an underlying JCA pool that shares state for all connections
        // in the same thread
        return true;
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

    // SessionSynchronization: AbstractSession already implements afterBegin,
    // beforeCompletion and afterCompletion

    // Methods that throws a rolling back application exception

    @Override
    public DocumentModel createDocument(DocumentModel docModel)
            throws ClientException {

        try {
            return super.createDocument(docModel);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel[] createDocument(DocumentModel[] docModels)
            throws ClientException {

        try {
            return super.createDocument(docModels);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void save() throws ClientException {
        try {
            super.save();
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel saveDocument(DocumentModel docModel)
            throws ClientException {
        try {
            return super.saveDocument(docModel);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void saveDocuments(DocumentModel[] docModels) throws ClientException {
        try {
            super.saveDocuments(docModels);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void removeDocuments(DocumentRef[] docRefs) throws ClientException {
        try {
            super.removeDocuments(docRefs);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void removeChildren(DocumentRef docRef) throws ClientException {
        try {
            super.removeChildren(docRef);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel copy(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        try {
            return super.copy(src, dst, name);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public List<DocumentModel> copy(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        try {
            return super.copy(src, dst);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel copyProxyAsDocument(DocumentRef src, DocumentRef dst,
            String name) throws ClientException {
        try {
            return super.copyProxyAsDocument(src, dst, name);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public List<DocumentModel> copyProxyAsDocument(List<DocumentRef> src,
            DocumentRef dst) throws ClientException {
        try {
            return super.copyProxyAsDocument(src, dst);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel move(DocumentRef src, DocumentRef dst, String name)
            throws ClientException {
        try {
            return super.move(src, dst, name);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }

    }

    @Override
    public void move(List<DocumentRef> src, DocumentRef dst)
            throws ClientException {
        try {
            super.move(src, dst);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void setACP(DocumentRef docRef, ACP newAcp, boolean overwrite)
            throws ClientException {
        try {
            super.setACP(docRef, newAcp, overwrite);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version) throws ClientException {
        try {
            return super.restoreToVersion(docRef, version);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void checkOut(DocumentRef docRef) throws ClientException {
        try {
            super.checkOut(docRef);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentRef checkIn(DocumentRef docRef, VersioningOption option,
            String checkinComment) throws ClientException {
        try {
            return super.checkIn(docRef, option, checkinComment);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    @Deprecated
    public DocumentModel checkIn(DocumentRef docRef, VersionModel version)
            throws ClientException {
        try {
            return super.checkIn(docRef, version);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel createProxy(DocumentRef docRef, DocumentRef folderRef)
            throws ClientException {
        try {
            return super.createProxy(docRef, folderRef);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel createProxy(DocumentRef parentRef, DocumentRef docRef,
            VersionModel version, boolean overwriteExistingProxy)
            throws ClientException {
        try {
            return super.createProxy(parentRef, docRef, version,
                    overwriteExistingProxy);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public boolean followTransition(DocumentRef docRef, String transition)
            throws ClientException {
        try {
            return super.followTransition(docRef, transition);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }

    }

    @Override
    public void setLock(DocumentRef docRef, String key) throws ClientException {
        try {
            super.setLock(docRef, key);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public String unlock(DocumentRef docRef) throws ClientException {
        try {
            return super.unlock(docRef);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException {
        try {
            return super.publishDocument(docToPublish, section);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section, boolean overwriteExistingProxy)
            throws ClientException {
        try {
            return super.publishDocument(docToPublish, section,
                    overwriteExistingProxy);
        } catch (Throwable e) {
            throw new RollbackClientException(e);
        }
    }

    @Override
    public void importDocuments(List<DocumentModel> docModels)
            throws ClientException {
        try {
            super.importDocuments(docModels);
        } catch (Throwable t) {
            throw new RollbackClientException("Error during document import", t);
        }
    }

    @Override
    public DocumentModel getSourceDocument(DocumentRef docRef)
            throws ClientException {
        try {
            return super.getSourceDocument(docRef);
        } catch (Throwable t) {
            throw new RollbackClientException(t);
        }
    }

    @Override
    public List<VersionModel> getVersionsForDocument(DocumentRef docRef)
            throws ClientException {
        try {
            return super.getVersionsForDocument(docRef);
        } catch (Throwable t) {
            throw new RollbackClientException(t);
        }
    }

    @Override
    public DocumentModel getLastDocumentVersion(DocumentRef docRef)
            throws ClientException {
        try {
            return super.getLastDocumentVersion(docRef);
        } catch (Throwable t) {
            throw new RollbackClientException(t);
        }
    }

    @Override
    public DocumentRef getLastDocumentVersionRef(DocumentRef docRef)
            throws ClientException {
        try {
            return super.getLastDocumentVersionRef(docRef);
        } catch (Throwable t) {
            throw new RollbackClientException(t);
        }
    }

    @Override
    public List<DocumentModel> getVersions(DocumentRef docRef)
            throws ClientException {
        try {
            return super.getVersions(docRef);
        } catch (Throwable t) {
            throw new RollbackClientException(t);
        }
    }
}
