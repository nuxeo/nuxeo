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

package org.nuxeo.platform.cache.web;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.Serializable;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.cache.CacheListener;
import org.nuxeo.ecm.platform.cache.client.ClientCacheServiceFactory;

/**
 * Seam component created for each Seam session context. Each instance of this
 * class creates a cache listener that tries to invalidate/update
 * changed objects into the associated Seam context.
 * <p>
 * Bridge from Cache to Seam.
 *
 * @author DM
 */
@Stateful
@Name(CacheUpdateNotifier.SEAM_NAME_CACHE_NOTIFIER)
@Scope(ScopeType.SESSION)
@SerializedConcurrentAccess
public class CacheUpdateNotifierBean implements CacheUpdateNotifier, Serializable {

    private static final long serialVersionUID = -4658013073616597630L;

    private static final Log log = LogFactory.getLog(CacheUpdateNotifierBean.class);

    static int c = 0;

    class CacheListenerImpl implements CacheListener {

        public final int instance;

        private final Context sessionCtx;

        CacheListenerImpl(Context sessionCtx) {
            assert sessionCtx != null;
            log.debug("CacheListenerImpl instantiated");
            instance = c++;
            this.sessionCtx = sessionCtx;
        }

        public void documentUpdate(DocumentModel docModel, boolean pre) {
            assert docModel != null;
            final String logPrefix = "<documentUpdate> ";
            log.info(logPrefix + " docModel path: "
                    + docModel.getPathAsString() + ", pre: " + pre);
            log.info(logPrefix + "doc title: "
                    + docModel.getProperty("dublincore", "title"));
        }

        public void documentRemove(DocumentModel docModel) {
            assert docModel != null;
            final String logPrefix = "<documentRemove> ";
            log.debug(logPrefix + " docModel path: "
                    + docModel.getPathAsString());

            log.debug("documentRemove instance: " + instance);
            log.debug("documentRemove: sessionContext " + sessionContext);
            log.debug("documentRemove: sessionCtx     " + sessionCtx);

            //docModelOnNotification = docModel;

            //printVarsOnContext(sessionContext);
            //printVarsOnContext(sessionCtx);
            removeDocumentFromContextStructures(sessionCtx, docModel);
        }

        public void documentRemoved(String fqn) {
            final String logPrefix = "<documentRemoved> ";

            log.debug(logPrefix + "fqn: " + fqn);
        }

    }

    //private DocumentModel docModelOnNotification;

    /**
     * This will be a valid variable if the thread is coming from
     * a Seam context (the thread is started by an event = web request).
     */
    @In
    private transient Context sessionContext;

    /**
     * List of listeners registered by other Seam components which are taken
     * care of by this business delegate so their lifespan won't go beyond
     * session context.
     */
    private transient Set<CacheListener> listeners;

    private transient CacheListener cacheListener;

    @Create
    @PostActivate
    public void init() {
        log.debug("@Create a CacheListener for Seam session context");

        log.debug("sessionContext  " + sessionContext);
        log.debug("sessionContext2  " + Contexts.getSessionContext());

        if (sessionContext == null) {
            sessionContext = Contexts.getSessionContext();
        }

        assert sessionContext != null;

        listeners = new HashSet<CacheListener>();

        // this is the specialized listener
        cacheListener = new CacheListenerImpl(sessionContext);

        ClientCacheServiceFactory.getCacheService().addCacheListener(
                cacheListener);
    }

    /**
     * Registers a cache listener with the cache service.
     *
     * @param
     */
    public void addCacheListener(CacheListener listener) {
        assert null != listeners;
        log.debug("<addCacheListener> " + listener);
        listeners.add(listener);
        ClientCacheServiceFactory.getCacheService().addCacheListener(
                listener);
    }

    /**
     * Removes the given listener.
     *
     * @param listener
     */
    public void removeCacheListener(CacheListener listener) {
        assert null != listeners;
        log.debug("<removeCacheListener> " + listener);
        ClientCacheServiceFactory.getCacheService().removeCacheListener(
                listener);
        listeners.remove(listener);
    }

    @PrePassivate
    public void ejbPassivate() {
        log.debug("PrePassivate");
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Destroy");
        ClientCacheServiceFactory.getCacheService().removeCacheListener(
                cacheListener);

        for (CacheListener listener : listeners) {
            ClientCacheServiceFactory.getCacheService().removeCacheListener(
                    listener);
        }
    }

    private void removeDocumentFromContextStructures(Context ctx,
            DocumentModel docModelOnNotification) {
        //final String contextVarName = "ListOfDocuments";
        for (String varName : ctx.getNames()) {
            removeDocumentFromContextStructure(ctx, varName, docModelOnNotification);
        }
    }

    /**
     * Checks if the structure identified by the given name on the sessionContext
     * is a manageable structure which might contain the current
     * docModelOnNotification and removes it.
     *
     */
    private void removeDocumentFromContextStructure(Context ctx,
            final String contextVarName, DocumentModel docModelOnNotification) {
        final String logPrefix = "<removeDocumentFromContextList> "
                + "contextVarName=" + contextVarName;

        final Object objOnCtx = ctx.get(contextVarName);
        //log.info(logPrefix + " check context var[" + contextVarName + "] = "
        //        + objOnCtx.getClass());
        if (objOnCtx != null) {
            if (objOnCtx instanceof List) {

                final List list = (List) objOnCtx;

                removeDocumnentFromList(contextVarName, docModelOnNotification,
                        list);

            //} else if (objOnCtx instanceof DocModelTableModel) {
            //    final DocModelTableModel tableModel = (DocModelTableModel) objOnCtx;

            //    final boolean removed = removeDocumnentFromTableModel(docModelOnNotification, tableModel);

            //    log.info(logPrefix + "tableModel name: " + contextVarName + ", removed: " + removed);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(logPrefix + "structure not handled: "
                            + contextVarName);
                }
            }
        }
    }

    private void removeDocumnentFromList(final String contextVarName,
            DocumentModel docModelOnNotification, final List list) {
        final String logPrefix = "<removeDocumnentFromList> ";

        boolean found = false;
        boolean removed = false;
        try {
            for (Object object : list) {
                if (object instanceof DocumentModel) {
                    final DocumentModel model = (DocumentModel) object;
                    log.debug("Compare : "
                                    + docModelOnNotification.getRef());
                    log.debug("        : " + model.getRef());
                    if (docModelOnNotification.getRef().equals(
                            model.getRef())) {
                        try {
                            removed = list.remove(model);
                        } catch (UnsupportedOperationException e) {
                            log.error("Cannot remove document from list (UnsupportedOperationException)");
                        }
                        found = true;
                        break;
                    }
                } else {
                    log.debug(logPrefix
                            + "The list does not contain DocumentModel objects. Skipping...");
                    break;
                }
            }
        } catch (ConcurrentModificationException e) {
            // probably is our list...
            log.debug(logPrefix
                    + " :: ConcurrentModificationException occured. "
                    + "Ignoring...");
        }
        // final boolean opResult = list.remove(docModelOnNotification);
        log.debug(logPrefix + "list name: " + contextVarName + ", found: "
                + found + ", removed: " + removed);
    }

    /*
    private static boolean removeDocumnentFromTableModel(
            final DocumentModel docModelOnNotification,
            final DocModelTableModel tableModel) {
        //final String logPrefix = "<removeDocumnentFromTableModel> ";

        try {
            return tableModel.removeRow(docModelOnNotification);
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    */

    /**
     * Debug utility method.
     *
     * @param ctx
     */
    private void printVarsOnContext(Context ctx) {
        final String logPrefix = "VarOnCtx: ";
        log.info(logPrefix + ctx);
        for (String varName : ctx.getNames()) {
            final Object objOnCtx = ctx.get(varName);
            log.info(logPrefix + "var: " + varName + " = " + objOnCtx);
        }
    }

    /**
     * Method is called to trigger the activation of the bean.
     * It does nothing on its own.
     */
    // TODO : find a less intrusive solution. Avoid calling too many times
    // without a scope
    @ExcludeClassInterceptors
    @ExcludeDefaultInterceptors
    public void doNothing() {
        // log.info("<doNothing>" + sessionContext);
    }

}
