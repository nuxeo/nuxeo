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

package org.nuxeo.ecm.webapp.pagination;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.event.PhaseId;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.EmptyResultsProvider;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.api.SortNotSupportedException;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentChildrenStdFarm;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component named 'resultsProvidersCache'. Implements actions available in
 * UI for navigating forward and backward through the pages of a result
 * containing Document(models).
 * <p>
 * This compononent maintains a cache of {@link PagedDocumentsProvider}
 * instances.
 * <p>
 * It's also capable of instantiating the cache by calling provider farms: other
 * Seam components that also implement the ResultsProviderFarm interface.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 * @deprecated use {@link ContentView} instances in conjunction with
 *             {@link PageProvider} instead.
 */
@Deprecated
@Name("resultsProvidersCache")
@Scope(ScopeType.CONVERSATION)
public class ResultsProvidersCacheBean implements ResultsProvidersCache, Serializable {

    private static final long serialVersionUID = 8632024396770685542L;

    private static final Log log = LogFactory.getLog(ResultsProvidersCacheBean.class);

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    private transient Map<String, PagedDocumentsProvider> resultsProvidersCache;

    /**
     * Used to indicate that providers have already been refreshed within this
     * scope.
     */
    @In(required = false)
    @Out(scope = ScopeType.EVENT, required = false)
    private transient Set<String> cleanProviders;

    // ----- lifecycle methods ------
    /**
     * Init method needed in Seam managed lifecycle.
     */
    @Create
    public void init() {
        log.debug("Initializing...");
        initCache();
    }


    public void destroy() {
        log.debug("Destroy...");
    }


    private void initCache() {
        if (resultsProvidersCache == null) {
            log.debug("Constructing a new, empty cache");
            resultsProvidersCache = new HashMap<String, PagedDocumentsProvider>();
        }
    }

    /*
     * API
     */

    public PagedDocumentsProvider get(String name) throws ClientException {
        try {
            return get(name, null);
        } catch (SortNotSupportedException e) {
            throw new ClientException("unexpected sortNotSupported", e);
        }
    }

    public PagedDocumentsProvider get(String name, SortInfo sortInfo)
            throws ClientException, SortNotSupportedException {
        PhaseId lifeCycleId = FacesLifecycle.getPhaseId();
        PagedDocumentsProvider provider = resultsProvidersCache.get(name);
        if (cleanProviders == null) {
            cleanProviders = new HashSet<String>();
        }
        if (provider == null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("(Re)building provider '" + name + "'");
                }
                provider = getProviderFarmFor(name).getResultsProvider(name,
                        sortInfo);
            } catch (ResultsProviderFarmUserException e) {
                if (lifeCycleId != PhaseId.RENDER_RESPONSE) {
                    // don't send message during render phase
                    // otherwise they will be displayed in next page !
                    facesMessages.add(FacesMessage.SEVERITY_WARN,
                            resourcesAccessor.getMessages().get(e.getMessage()));
                }
                return getEmptyResultsProvider(name);
            } catch (Exception e) {
                log.error("failed to obtain sorted resultProvider", e);
                try {
                    log.debug("retrying search without sort parameters");
                    provider = getProviderFarmFor(name).getResultsProvider(
                            name, null);
                } catch (Exception e2) {
                    if (lifeCycleId != PhaseId.RENDER_RESPONSE) {
                        // don't send message during render phase
                        // otherwise they will be displayed in next page !

                        facesMessages.add(FacesMessage.SEVERITY_WARN,
                                resourcesAccessor.getMessages().get("feedback.search.invalid"));
                    }
                    resultsProvidersCache.put(name, new EmptyResultsProvider());
                    return resultsProvidersCache.get(name);
                }
            }
            resultsProvidersCache.put(name, provider);
        } else if (!cleanProviders.contains(name)) {
            // avoid refreshing twice if someone calls provider changing from
            // jsf: used to be refreshed in APPLY_REQUEST_VALUES for nothing
            if (lifeCycleId == PhaseId.RENDER_RESPONSE ||
                    lifeCycleId == PhaseId.INVOKE_APPLICATION) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Refreshing dirty provider %s " +
                            "(jsf phase='%s')", name, lifeCycleId));
                }
                provider.refresh();
            }
        }
        cleanProviders.add(name);
        return provider;
    }

    /**
     * Returns an empty provider with the expected actual type for given name.
     *
     * @param name
     * @return the empty provider
     * @throws ClientException
     */
    protected PagedDocumentsProvider getEmptyResultsProvider(String name) throws ClientException {
        ResultsProviderFarm farm = getProviderFarmFor(name);

        // Using reflection to maintain BBB.
        // In 5.2 branch, this will be on ResultsProviderFarm interface
        Method method;

        try {
            method = farm.getClass().getMethod("getEmptyResultsProvider", String.class);
        } catch (SecurityException e) {
            return new EmptyResultsProvider();
        } catch (NoSuchMethodException e) {
            log.warn(farm.getClass().getName() + " will have to " +
                    "implement getEmptyResultsProvider() for Nuxeo 5.2");
            return new EmptyResultsProvider();
        }
        try {
            return (PagedDocumentsProvider) method.invoke(farm, name);
        } catch (Exception e) {
            return new EmptyResultsProvider();
        }
    }

    public void invalidate(String name) {
        if (log.isDebugEnabled()) {
            log.debug("Invalidating provider '" + name + "'");
        }
        resultsProvidersCache.remove(name);
    }

    @Observer(value={ EventNames.DOCUMENT_CHILDREN_CHANGED, EventNames.LOCATION_SELECTION_CHANGED },
            create=false)
    @BypassInterceptors
    public void invalidateChildrenProvider() {
        invalidate(DocumentChildrenStdFarm.CHILDREN_BY_COREAPI);
    }

    public DocumentModelList getCurrentPageOf(String name)
            throws ClientException {
        return get(name).getCurrentPage();
    }

    /**
     * @deprecated use normal messaging in JSF with pageIndex and numberOfPages
     *             as params
     */
    @Deprecated
    public String getRecordStatus() {
        log.error("getRecordStatus has been called");
        String numberOfPagesStr;
        // GR TODO just there so that it stops breaking te stuff
        int numberOfPages = 10;
        if (numberOfPages == PagedDocumentsProvider.UNKNOWN_SIZE) {
            numberOfPagesStr = "unknown";
        } else {
            numberOfPagesStr = Integer.toString(numberOfPages);
        }
        /*
         * return MessageFormat.format("{0} of {1}", new Object []{
         * Integer.valueOf(currentPage), numberOfPagesStr });
         */
        return " of " + numberOfPagesStr;
    }

    /*
     * INTERNALS
     */

    /**
     * Gets a ResultsProviderFarm that can instantiate the given named
     * PagedDocumentsProvider.
     *
     * @param name
     * @return
     * @throws ClientException If no results provider has been registered under
     *             the required name
     */
    private ResultsProviderFarm getProviderFarmFor(String name)
            throws ClientException {

        ResultsProviderService service = (ResultsProviderService) Framework.getRuntime().getComponent(
                ResultsProviderService.NAME);
        String farmName = service.getFarmNameFor(name);
        if (farmName == null) {
            throw new ClientException("Unknown results provider: " + name);
        }

        Object ob = Contexts.lookupInStatefulContexts(farmName);
        if (ob == null) {
            // seam component has not yet been created
            SeamComponentCallHelper.getSeamComponentByName(farmName);
            ob = Contexts.lookupInStatefulContexts(farmName);
            if (ob == null) {
                throw new ClientException(farmName
                        + " provider farm is not a registered seam component");
            }
        }
        return (ResultsProviderFarm) ob;
    }

    // TEMPORARY COMPATIBILITY
    public int getNumberOfPages() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getPageIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

}
