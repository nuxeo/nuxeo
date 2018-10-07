/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.ecm.webapp.seam;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.service.TimestampedService;

/**
 * Simple Seam bean to control the Reload Action
 *
 * @author tiry
 */
@Name("seamReload")
@Scope(EVENT)
@Install(precedence = FRAMEWORK)
public class NuxeoSeamHotReloader implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NuxeoSeamHotReloader.class);

    @In(required = false, create = true)
    private transient NuxeoPrincipal currentUser;

    /**
     * Returns true if dev mode is set
     *
     * @since 5.6
     * @see Framework#isDevModeSet()
     */
    @Factory(value = "nxDevModeSet", scope = ScopeType.EVENT)
    public boolean isDevModeSet() {
        return Framework.isDevModeSet();
    }

    @Factory(value = "seamHotReloadIsEnabled", scope = ScopeType.APPLICATION)
    public boolean isHotReloadEnabled() {
        return SeamHotReloadHelper.isHotReloadEnabled();
    }

    /**
     * Returns true if dev mode is set and current user is an administrator.
     *
     * @since 5.6
     * @return
     */
    public boolean getCanTriggerFlush() {
        return isDevModeSet() && currentUser != null && currentUser.isAdministrator();
    }

    /**
     * Calls the {@link ReloadService#flush()} method, that should trigger the reset of a bunch of caches shared by all
     * users, and sends a Seam event to propagate this to other Seam components.
     * <p>
     * Does nothing if not in dev mode.
     * <p>
     * The reload service flush method should already be triggerd by install/uninstall of modules. This method makes it
     * possible to force it again, and to propagate it to the Seam layer for current user.
     *
     * @see #resetSeamComponentsCaches()
     * @see #shouldResetCache(Long)
     * @see #shouldResetCache(TimestampedService, Long)
     * @since 5.6
     */
    public String doFlush() {
        if (Framework.isDevModeSet()) {
            FacesContext faces = FacesContext.getCurrentInstance();
            String viewId = faces.getViewRoot().getViewId();
            URLPolicyService service = Framework.getService(URLPolicyService.class);
            String outcome = service.getOutcomeFromViewId(viewId, null);
            ReloadService srv = Framework.getService(ReloadService.class);
            srv.flush();
            Events.instance().raiseEvent(EventNames.FLUSH_EVENT);
            // return the current view id otherwise an error appears in logs
            // because navigation cache needs to be rebuilt after execution
            return outcome;
        }
        return null;
    }

    /**
     * Returns true if reload service has sent a runtime flush event since given timestamp.
     *
     * @since 5.6
     * @param cacheTimestamp
     * @see ReloadService#lastFlushed()
     */
    public boolean shouldResetCache(Long cacheTimestamp) {
        if (cacheTimestamp == null) {
            return true;
        }
        Long serviceTimestamp = getCurrentCacheTimestamp();
        if (serviceTimestamp == null) {
            return false;
        }
        if (cacheTimestamp.compareTo(serviceTimestamp) < 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns the last flush timestamp held by the {@link ReloadService}.
     *
     * @since 5.6
     * @see ReloadService
     * @see TimestampedService
     */
    public Long getCurrentCacheTimestamp() {
        ReloadService service = Framework.getService(ReloadService.class);
        return service.lastFlushed();
    }

    /**
     * Returns true if given service has changed since given timestamp.
     *
     * @since 5.6
     * @param service
     * @param cacheTimestamp
     * @see TimestampedService
     */
    public boolean shouldResetCache(TimestampedService service, Long cacheTimestamp) {
        if (cacheTimestamp == null || service == null) {
            return true;
        }
        Long serviceTimestamp = service.getLastModified();
        if (serviceTimestamp == null) {
            return false;
        }
        if (cacheTimestamp.compareTo(serviceTimestamp) < 0) {
            return true;
        }
        return false;
    }

    /**
     * Resets most caches of the Seam application.
     * <p>
     * This is useful when a change is detected on the reload service.
     * <p>
     * For compatibility and easier upgrade, this method listens to the {@link EventNames#FLUSH_EVENT}, and sends other
     * events to the Seam layer for other components to reset their own cache without needing to change their code.
     * <p>
     * In the future, this behaviour could be removed, so Seam component should reset their cache listening to the
     * {@link EventNames#FLUSH_EVENT} directly.
     *
     * @since 5.6
     */
    @Observer(value = { EventNames.FLUSH_EVENT }, create = false)
    @BypassInterceptors
    public void triggerResetOnSeamComponents() {
        String[] events = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
                EventNames.LOCATION_SELECTION_CHANGED, EventNames.CONTENT_ROOT_SELECTION_CHANGED,
                EventNames.DOMAIN_SELECTION_CHANGED, EventNames.LOCAL_CONFIGURATION_CHANGED, };
        Events seamEvents = Events.instance();
        for (String event : events) {
            seamEvents.raiseEvent(event);
        }
    }

    /**
     * Triggers a full reload of Seam context and components.
     * <p>
     * Needs the Seam debug jar to be present and Seam debug mode to be enabled.
     */
    public String doReload() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }

        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        String url = BaseURL.getBaseURL(request);
        url += "restAPI/seamReload";

        try {
            response.resetBuffer();
            response.sendRedirect(url);
            response.flushBuffer();
            request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
            facesContext.responseComplete();
        } catch (IOException e) {
            log.error("Error during redirect", e);
        }
        return null;
    }

}
