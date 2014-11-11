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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache.server.bd;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.ejb.DocumentManagerBean;
import org.nuxeo.ecm.core.api.ejb.DocumentManagerCacheStatInterceptor;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.platform.cache.CacheService;
import org.nuxeo.ecm.platform.cache.CacheServiceException;
import org.nuxeo.ecm.platform.cache.CacheServiceFactory;
import org.nuxeo.ecm.platform.cache.CacheableObjectKeys;
import org.nuxeo.ecm.platform.cache.data.DocumentModelGhost;

/**
 * This EJB session bean will be referred and used by client code instead of its
 * super class DocumentManagerBean. In case of retrieving business objects like
 * DataModel or DocumentModel this class overrides the methods to cache the
 * returned objects by the superclass calls. The cached objects will be
 * replicated to client cache(s) avoiding subsequent calls to the
 * DocumentManager session for the same object.
 * <p>
 * In case of objects being updated the Manager should intercept any change and
 * update the cached objects also... TBD!! Also in DocumentModel - remote
 * interface should have a cacheable variant.
 *
 * @author DM
 */
@Stateful
// next annotation is to prevent
// javax.ejb.EJBException: Application Error: no concurrent calls on stateful beans
// this might happen when the requested DocumentModel is not replicated in time
// for the next subsequent calls to find it in the cache
// -- happens on linux
// TODO how can we insure the obj replication
@SerializedConcurrentAccess
@Remote(CacheableDocumentManager.class)
public class CacheableDocumentManagerBean extends DocumentManagerBean implements CacheableDocumentManager {

    private static final long serialVersionUID = 1711569527351638572L;

    private static final Log log = LogFactory.getLog(CacheableDocumentManagerBean.class);

    private transient CacheService cacheService;
    private transient EventListener listener;

    @PostConstruct
    public void ejbCreate() {
        log.info("ejbCreate");
        initServerCache();
        registerDocumentChangeListener();
    }

    private void initServerCache() {
        log.info("Initialize cache system");

        //cacheService = new CacheService();
        try {
            cacheService = CacheServiceFactory.getCacheService(this.getClass().getName());
            // cacheService.init();
            //cacheServer.startService();
        } catch (Exception e) {
            final String errMsg = "In " + this.getClass().getSimpleName()
                    + ": " + e.getMessage();
            log.error(errMsg, e);
            // this method being a @PostConstruct callback cannot throw a
            // checked exception....
            // throw new ClientException(errMsg);
            throw new EJBException(errMsg, e);
        }
    }

    @Remove
    public void ejbRemove() {
        log.info("ejbRemove");
        disposeServerCache();
    }

    private void disposeServerCache() {
        log.info("Dispose cache system");

        if (cacheService != null) {
            cacheService.stopService();
            cacheService = null;
        }
    }

    @PostActivate
    public void ejbActivate() {
        log.info("ejbActivate");
        initServerCache();
        registerDocumentChangeListener();
    }

    private void registerDocumentChangeListener() {
        log.info("Register Document change listener");

        CoreEventListenerService repListenerService = Framework.getLocalService(CoreEventListenerService.class);
        assert null == listener;
        assert null != cacheService;
        listener = new CachedObjectsEventListener(cacheService);
        repListenerService.addEventListener(listener);
    }

    @PrePassivate
    public void ejbPassivate() {
        log.info("ejbPassivate");
        disposeServerCache();
        deregisterDocumentChangeListener();
    }

    private void deregisterDocumentChangeListener() {
        log.info("De-register Document change listener");

        CoreEventListenerService repListenerService = Framework.getLocalService(CoreEventListenerService.class);
        assert null != listener;
        repListenerService.removeEventListener(listener);
        listener = null;
    }

    /**
     * Caches the root document.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getRootDocument()
     */
    @Override
    public DocumentModel getRootDocument() throws ClientException {
        log.info("Caching root DocumentModel ");
        final DocumentModel dm = super.getRootDocument();

        final String dmPath = CacheableObjectKeys.getCacheKey(dm.getRef());
        putDocumentInCache(dmPath, dm);

        final DocumentModelGhost ghost = createDocGhost(dm);

        log.debug("Returning: " + ghost);
        return ghost;
    }

    /**
     * Caches the retrieved document with its path as the cache key.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getDocument(org.nuxeo.ecm.core.api.DocumentRef)
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel getDocument(DocumentRef docRef) throws ClientException {
        log.debug("Caching DocumentModel for docRef : " + docRef);
        final DocumentModel dm = super.getDocument(docRef);

        final String dmPath = CacheableObjectKeys.getCacheKey(docRef);
        putDocumentInCache(dmPath, dm);

        // return a ghost implementation instead
        // that will lazily load real data from cache
        final DocumentModelGhost ghost = new DocumentModelGhost(docRef,
                dm.getSessionId(), dm.getType(), dm.getDeclaredSchemas(),
                dm.getId(), dm.getPath(), dm.getParentRef());

        log.debug("Returning: " + ghost);
        //return dm;
        return ghost;
    }

    /**
     * Caches the document with the same key as for the one param getDocument(...)
     * method because the Document will get other schemas in a lazy mode.
     *
     * @param schemas may be null
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getDocument(org.nuxeo.ecm.core.api.DocumentRef, java.lang.String[])
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel getDocument(DocumentRef docRef, String[] schemas)
            throws ClientException {
        log.debug("Caching DocumentModel for docRef : " + docRef
                + " and schemas: " + schemas);
        final DocumentModel dm = super.getDocument(docRef, schemas);

        final String dmPath = CacheableObjectKeys.getCacheKey(docRef);
        putDocumentInCache(dmPath, dm);

        // return a ghost implementation instead
        // that will lazily load real data from cache
        final DocumentModelGhost ghost = new DocumentModelGhost(docRef,
                dm.getSessionId(), dm.getType(), dm.getDeclaredSchemas(),
                dm.getId(), dm.getPath(), dm.getParentRef());

        log.debug("Returning: " + ghost);
        //return dm;
        return ghost;
    }

    /**
     * Caches the document which is the child with the given name of the document
     * having given DocumentRef.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getDocument(org.nuxeo.ecm.core.api.DocumentRef, java.lang.String[])
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel getChild(DocumentRef parent, String name)
            throws ClientException {
        log.debug("Caching child DocumentModel " + name
                + " for parent docRef : " + parent);
        final DocumentModel dm = super.getChild(parent, name);

        final String dmPath = CacheableObjectKeys.getCacheKey(parent, name);
        putDocumentInCache(dmPath, dm);

        final DocumentModelGhost ghost = createDocGhost(dm);

        log.debug("Returning: " + ghost);
        return ghost;
    }

    /**
     * Caches the requested version of document identified by the given docRef.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getDocument(org.nuxeo.ecm.core.api.DocumentRef, java.lang.String[])
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel getDocumentWithVersion(final DocumentRef docRef,
            VersionModel version) throws ClientException {
        log.debug("Caching version " + version + " of DocumentModel " + docRef);
        final DocumentModel dm = super.getDocumentWithVersion(docRef, version);

        final String dmPath = CacheableObjectKeys.getCacheKey(docRef, version);
        putDocumentInCache(dmPath, dm);

        final DocumentModelGhost ghost = createDocGhost(dm);

        log.debug("Returning: " + ghost);
        return ghost;
    }

    /**
     * Caches the newly created DocumentModel.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#createDocument(org.nuxeo.ecm.core.api.DocumentModel)
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel createDocument(final DocumentModel model) throws ClientException {
        log.debug("Caching new Document " + model);
        final DocumentModel dm =  super.createDocument(model);

        final String dmPath = CacheableObjectKeys.getCacheKey(dm.getRef());

        assert !cacheService.exists(dmPath);
        putDocumentInCache(dmPath, dm);

        final DocumentModelGhost ghost = createDocGhost(dm);

        log.debug("Returning: " + ghost);
        return ghost;
    }

    /**
     * Caches the parent (it should be already in the cache), but the main role
     * is to return a Ghost implementation of the parent DocumentModel.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getParentDocument(org.nuxeo.ecm.core.api.DocumentRef)
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel getParentDocument(final DocumentRef docRef)
            throws ClientException {
        log.debug("Caching parent for " + docRef);
        final DocumentModel dm = super.getParentDocument(docRef);

        if (null == dm) {
            log.warn("Null parent for doc ref: " + docRef);
            return null;
        }

        final String dmPath = CacheableObjectKeys.getCacheKey(dm.getRef());
        putDocumentInCache(dmPath, dm);

        final DocumentModelGhost ghost = createDocGhost(dm);

        log.debug("Returning : " + ghost);
        return ghost;
    }

    //
    // TODO - We should change to a DocumentModelList interface...
    //
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModel[] createDocument(final DocumentModel[] docModels)
            throws ClientException {
        final DocumentModel[] newDocModels = super.createDocument(docModels);

        final DocumentModelGhost[] ghosts = new DocumentModelGhost[newDocModels.length];
        int i = 0;
        for (DocumentModel dm : newDocModels) {
            final String dmPath = CacheableObjectKeys.getCacheKey(dm.getRef());
            putDocumentInCache(dmPath, dm);
            final DocumentModelGhost ghost = createDocGhost(dm);
            ghosts[i] = ghost;
        }

        log.debug("Returning: " + ghosts);
        return ghosts;
    }

    /**
     * Caches the objects returned by parent class and return a new list with
     * DocumentModelGhost objects that will refer the real cached objects.
     * <p>
     * Won't cache the retrieved lists for the moment as these might be changed
     * between requests and replicating a potentially large number of lists that
     * are constructed based on different params is not desired.
     *
     * @see CoreSession#getChildren(DocumentRef)
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList getChildren(DocumentRef parent)
            throws ClientException {
        log.debug("Caching children for parent Ref : " + parent);
        final List<DocumentModel> documents = super.getChildren(parent);

        // obtain the parent to be able to build a path
        // DONOT ....final DocumentModel parentDocModel =
        // super.getDocument(parent);

        // TODO needed in case we cache list
        // final String dmPath = CacheableObjectKeys.getCacheKey(parent);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);


        // FIXME: Cache the list (after the code can invalidate it)
        /*
        final String fqn = CacheableObjectKeys.getCacheKey(parent) + "/children";
        try {
            cacheService.putObject(fqn, ghostDocuments);
        } catch (CacheServiceException e) {
            //e.printStackTrace();
            final String errMsg = "In " + this.getClass().getSimpleName()
                    + ": " + e.getMessage();
            log.error(errMsg, e);
            throw new ClientException(errMsg);
        }
        */

        log.debug("Returning: " + ghostDocuments);
        //return documents;
        return ghostDocuments;
    }

    /**
     * Caches the objects returned by parent class and return a new list with
     * DocumentModelGhost objects that will refer the real cached objects.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getChildren(org.nuxeo.ecm.core.api.DocumentRef, java.lang.String)
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type)
            throws ClientException {
        log.debug("Caching children for parent Ref : " + parent + " and type: "
                + type);
        final List<DocumentModel> documents = super.getChildren(parent, type);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);

        log.debug("Returning: " + ghostDocuments);
        return ghostDocuments;
    }

    /**
     * Caches the objects returned by parent class and return a new list with
     * DocumentModelGhost objects that will refer the real cached objects.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getChildren(org.nuxeo.ecm.core.api.DocumentRef, java.lang.String, org.nuxeo.ecm.core.api.Filter, org.nuxeo.ecm.core.api.Sorter)
     */
    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type, String perm,
            Filter filter, Sorter sorter) throws ClientException {
        log.debug("Caching children for parent Ref : " + parent + " and type: "
                + type + ", with filter: " + filter + " and sorter: " + sorter);
        final List<DocumentModel> documents = super.getChildren(parent, type, perm,
                filter, sorter);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);

        log.debug("Returning: " + ghostDocuments);
        return ghostDocuments;
    }

    /**
     * Caches the objects returned by parent class and return a new list with
     * DocumentModelGhost objects that will refer the real cached objects.
     *
     * @see org.nuxeo.ecm.core.api.ejb.DocumentManagerBean#getChildren(org.nuxeo.ecm.core.api.DocumentRef, java.lang.String, org.nuxeo.ecm.core.api.Filter, org.nuxeo.ecm.core.api.Sorter)
     */
    @Override
    public DocumentModelList getChildren(DocumentRef parent, String type,
            Filter filter, Sorter sorter) throws ClientException {
        return getChildren(parent, type, null, filter, sorter);
    }

    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList query(String query) throws ClientException {
        log.debug("Caching documents from query : " + query);
        final List<DocumentModel> documents = super.query(query);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);

        log.debug("Returning: " + ghostDocuments);
        return ghostDocuments;
    }

    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList query(String query, Filter filter) throws ClientException {
        log.debug("Caching filtered documents from query : " + query);
        final List<DocumentModel> documents = super.query(query, filter);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);

        log.debug("Returning: " + ghostDocuments);
        return ghostDocuments;
    }

    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList querySimpleFts(String keywords) throws ClientException {
        log.debug("Caching documents from querySimpleFts, keywords : " + keywords);
        final List<DocumentModel> documents = super.querySimpleFts(keywords);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);

        log.debug("Returning: " + ghostDocuments);
        return ghostDocuments;
    }

    @Interceptors(DocumentManagerCacheStatInterceptor.class)
    @Override
    public DocumentModelList querySimpleFts(String keywords, Filter filter) throws ClientException {
        log.debug("Caching filtered documents from querySimpleFts, keywords : " + keywords);
        final List<DocumentModel> documents = super.querySimpleFts(keywords, filter);

        final DocumentModelList ghostDocuments = prepareGhostDocumentsList(documents);

        log.debug("Returning: " + ghostDocuments);
        return ghostDocuments;
    }

    /**
     * Utility method that creates a DocumentModelGhost given an existing DocumentModel.
     *
     * @param dm
     * @return
     */
    private DocumentModelGhost createDocGhost(DocumentModel dm) {
        assert null != dm;

        return new DocumentModelGhost(dm.getRef(),
                dm.getSessionId(), dm.getType(), dm.getDeclaredSchemas(),
                dm.getId(), dm.getPath(), dm.getParentRef());
    }

    /**
     * Utility method that wraps exception thrown by Caching system.
     *
     * @param dmPath
     * @param documentModel
     * @throws ClientException
     */
    private void putDocumentInCache(final String dmPath,
            final DocumentModel documentModel) throws ClientException {

        // if document is already in the cache we won't put it again since
        // this will trigger a nodeRemove event
        if (cacheService.exists(dmPath)) {
            return;
        }
        log.debug("<putDocumentInCache> " + documentModel);
        try {
            cacheService.putObject(dmPath, documentModel);
        } catch (CacheServiceException e) {
            // e.printStackTrace();
            final String errMsg = "In " + this.getClass().getSimpleName();
            log.error(errMsg, e);
            // TODO : analyse the possibility not to throw an exception here
            // but to let the call return as it is without caching capability
            throw new ClientException(errMsg, e);
        }
    }

    /**
     * Utility method that builds a list of DocumentModelGhost objects from a
     * given list of real DocumentMode objects.
     *
     * @param documents
     * @return the ghost list
     * @throws ClientException
     */
    private DocumentModelList prepareGhostDocumentsList(
            final List<DocumentModel> documents) throws ClientException {

        final DocumentModelList ghostDocuments = new GhostDocumentsList();

        try {
            /*
             cacheService.putObject(dmPath, documents);

             // as for the list the cache is making a copy...
             // but for this case it won't be probably necessary
             // to retrieve the cached list because it is used
             // as immutable object on the client side
             List<DocumentModel> cachedDocuments = (List<DocumentModel>) cacheService
             .getObject(dmPath);
             */

            // put in the cache each of the DocumentModel in the list
            // and return a list of ghost DocumentModels instead
            // We might overwrite the objects alread in the cache, maybe
            // is better to check for their existence
            // FIXME: probably the object has been changed and need updated
            for (DocumentModel dm : documents) {

                final String fqn = CacheableObjectKeys.getCacheKey(dm);
                if (!cacheService.exists(fqn)) {
                    cacheService.putObject(fqn, dm);
                }

                // return a ghost implementation instead
                // that will lazily load real data from cache
                final DocumentModelGhost ghost = new DocumentModelGhost(
                        dm.getRef(), dm.getSessionId(), dm.getType(),
                        dm.getDeclaredSchemas(), dm.getId(), dm.getPath(),
                        dm.getParentRef());

                ghostDocuments.add(ghost);
            }

        } catch (CacheServiceException e) {
            // e.printStackTrace();
            final String errMsg = "In " + this.getClass().getSimpleName();
            log.error(errMsg, e);
            // TODO : analyse the possibility not to throw an exception here
            // but to let the call return as it is without caching capability
            throw new ClientException(errMsg, e);
        }

        return ghostDocuments;
    }

    //@Override
    public DataModel __getDataModel(DocumentRef docRef, String schema)
            throws ClientException {
        log.debug("Caching DataModel for docRef: " + docRef + ", schema: "
                + schema);
        final DataModel dm = super.getDataModel(docRef, schema);

        final String dmPath = CacheableObjectKeys.getCacheKey(dm);
        try {
            cacheService.putObject(dmPath, dm);
        } catch (CacheServiceException e) {
            final String errMsg = "In " + this.getClass().getSimpleName();
            log.error(errMsg, e);
            // TODO : analyse the possibility not to throw an exception here
            // but to let the call return as it is without caching capability
            throw new ClientException(errMsg, e);
        }

        return dm;
    }

    /**
     * @see org.nuxeo.ecm.platform.cache.server.bd.CacheableDocumentManager#getDocumentImpl(org.nuxeo.ecm.core.api.DocumentRef)
     */
    public DocumentModel getDocumentImpl(DocumentRef ref) throws ClientException {
        // just get the object from parent impl.
        return super.getDocument(ref);
    }

}
