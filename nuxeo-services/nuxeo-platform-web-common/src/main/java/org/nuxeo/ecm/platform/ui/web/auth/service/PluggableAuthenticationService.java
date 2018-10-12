/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPropagator;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoCallbackHandlerFactory;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.platform.web.common.session.NuxeoHttpSessionMonitor;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class PluggableAuthenticationService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService";

    public static final String EP_AUTHENTICATOR = "authenticators";

    public static final String EP_SESSIONMANAGER = "sessionManager";

    public static final String EP_CHAIN = "chain";

    public static final String EP_SPECIFIC_CHAINS = "specificChains";

    public static final String EP_PROPAGATOR = "propagator";

    public static final String EP_CBFACTORY = "JbossCallbackfactory";

    public static final String EP_STARTURL = "startURL";

    public static final String EP_OPENURL = "openUrl";

    public static final String EP_PREFILTER = "preFilter";

    public static final String EP_LOGINSCREEN = "loginScreen";

    private static final Log log = LogFactory.getLog(PluggableAuthenticationService.class);

    private Map<String, AuthenticationPluginDescriptor> authenticatorsDescriptors;

    private Map<String, NuxeoAuthenticationPlugin> authenticators;

    private Map<String, AuthPreFilterDescriptor> preFiltersDesc;

    private List<NuxeoAuthPreFilter> preFilters;

    private Map<String, NuxeoAuthenticationSessionManager> sessionManagers;

    // NB: not used. Remove?
    private NuxeoAuthenticationSessionManager defaultSessionManager;

    private NuxeoAuthenticationPropagator propagator;

    private NuxeoCallbackHandlerFactory cbhFactory;

    private List<String> authChain;

    private final Map<String, SpecificAuthChainDescriptor> specificAuthChains = new HashMap<String, SpecificAuthChainDescriptor>();

    private final List<OpenUrlDescriptor> openUrls = new ArrayList<OpenUrlDescriptor>();

    private final List<String> startupURLs = new ArrayList<String>();

    private LoginScreenConfigRegistry loginScreenConfigRegistry;

    @Override
    public void activate(ComponentContext context) {
        authenticatorsDescriptors = new HashMap<String, AuthenticationPluginDescriptor>();
        authChain = new ArrayList<String>();
        authenticators = new HashMap<String, NuxeoAuthenticationPlugin>();
        sessionManagers = new HashMap<String, NuxeoAuthenticationSessionManager>();
        defaultSessionManager = new DefaultSessionManager();
        loginScreenConfigRegistry = new LoginScreenConfigRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        authenticatorsDescriptors = null;
        authenticators = null;
        authChain = null;
        sessionManagers = null;
        defaultSessionManager = null;
        loginScreenConfigRegistry = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_AUTHENTICATOR)) {
            AuthenticationPluginDescriptor descriptor = (AuthenticationPluginDescriptor) contribution;
            if (authenticatorsDescriptors.containsKey(descriptor.getName())) {
                mergeDescriptors(descriptor);
                log.debug("merged AuthenticationPluginDescriptor: " + descriptor.getName());
            } else {
                authenticatorsDescriptors.put(descriptor.getName(), descriptor);
                log.debug("registered AuthenticationPluginDescriptor: " + descriptor.getName());
            }

            // create the new instance
            AuthenticationPluginDescriptor actualDescriptor = authenticatorsDescriptors.get(descriptor.getName());
            try {
                NuxeoAuthenticationPlugin authPlugin = actualDescriptor.getClassName().newInstance();
                authPlugin.initPlugin(actualDescriptor.getParameters());
                authenticators.put(actualDescriptor.getName(), authPlugin);
            } catch (InstantiationException e) {
                log.error(
                        "Unable to create AuthPlugin for : " + actualDescriptor.getName() + "Error : " + e.getMessage(),
                        e);
            } catch (IllegalAccessException e) {
                log.error(
                        "Unable to create AuthPlugin for : " + actualDescriptor.getName() + "Error : " + e.getMessage(),
                        e);
            }

        } else if (extensionPoint.equals(EP_CHAIN)) {
            AuthenticationChainDescriptor chainContrib = (AuthenticationChainDescriptor) contribution;
            log.debug("New authentication chain powered by " + contributor.getName());
            authChain.clear();
            authChain.addAll(chainContrib.getPluginsNames());
        } else if (extensionPoint.equals(EP_OPENURL)) {
            OpenUrlDescriptor openUrlContrib = (OpenUrlDescriptor) contribution;
            openUrls.add(openUrlContrib);
        } else if (extensionPoint.equals(EP_STARTURL)) {
            StartURLPatternDescriptor startupURLContrib = (StartURLPatternDescriptor) contribution;
            startupURLs.addAll(startupURLContrib.getStartURLPatterns());
        } else if (extensionPoint.equals(EP_PROPAGATOR)) {
            AuthenticationPropagatorDescriptor propagationContrib = (AuthenticationPropagatorDescriptor) contribution;

            // create the new instance
            try {
                propagator = propagationContrib.getClassName().newInstance();
            } catch (InstantiationException e) {
                log.error("Unable to create propagator", e);
            } catch (IllegalAccessException e) {
                log.error("Unable to create propagator", e);
            }
        } else if (extensionPoint.equals(EP_CBFACTORY)) {
            CallbackHandlerFactoryDescriptor cbhfContrib = (CallbackHandlerFactoryDescriptor) contribution;

            // create the new instance
            try {
                cbhFactory = cbhfContrib.getClassName().newInstance();
            } catch (InstantiationException e) {
                log.error("Unable to create callback handler factory", e);
            } catch (IllegalAccessException e) {
                log.error("Unable to create callback handler factory", e);
            }
        } else if (extensionPoint.equals(EP_SESSIONMANAGER)) {
            SessionManagerDescriptor smContrib = (SessionManagerDescriptor) contribution;
            if (smContrib.enabled) {
                try {
                    NuxeoAuthenticationSessionManager sm = smContrib.getClassName().newInstance();
                    sessionManagers.put(smContrib.getName(), sm);
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to create session manager", e);
                }
            } else {
                sessionManagers.remove(smContrib.getName());
            }
        } else if (extensionPoint.equals(EP_SPECIFIC_CHAINS)) {
            SpecificAuthChainDescriptor desc = (SpecificAuthChainDescriptor) contribution;
            specificAuthChains.put(desc.name, desc);
        } else if (extensionPoint.equals(EP_PREFILTER)) {
            AuthPreFilterDescriptor desc = (AuthPreFilterDescriptor) contribution;
            if (preFiltersDesc == null) {
                preFiltersDesc = new HashMap<String, AuthPreFilterDescriptor>();
            }

            if (desc.enabled) {
                preFiltersDesc.put(desc.getName(), desc);
            } else {
                preFiltersDesc.remove(desc.getName());
            }
        } else if (extensionPoint.equals(EP_LOGINSCREEN)) {
            LoginScreenConfig newConfig = (LoginScreenConfig) contribution;
            loginScreenConfigRegistry.addContribution(newConfig);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_AUTHENTICATOR)) {
            AuthenticationPluginDescriptor descriptor = (AuthenticationPluginDescriptor) contribution;
            authenticatorsDescriptors.remove(descriptor.getName());
            log.debug("unregistered AuthenticationPlugin: " + descriptor.getName());
        } else if (extensionPoint.equals(EP_LOGINSCREEN)) {
            LoginScreenConfig newConfig = (LoginScreenConfig) contribution;
            loginScreenConfigRegistry.removeContribution(newConfig);
        }
    }

    private void mergeDescriptors(AuthenticationPluginDescriptor newContrib) {
        AuthenticationPluginDescriptor oldDescriptor = authenticatorsDescriptors.get(newContrib.getName());

        // Enable/Disable
        oldDescriptor.setEnabled(newContrib.getEnabled());

        // Merge parameters
        Map<String, String> oldParameters = oldDescriptor.getParameters();
        oldParameters.putAll(newContrib.getParameters());
        oldDescriptor.setParameters(oldParameters);

        // override LoginLModule
        if (newContrib.getLoginModulePlugin() != null && newContrib.getLoginModulePlugin().length() > 0) {
            oldDescriptor.setLoginModulePlugin(newContrib.getLoginModulePlugin());
        }

        oldDescriptor.setStateful(newContrib.getStateful());

        if (newContrib.getClassName() != null) {
            oldDescriptor.setClassName(newContrib.getClassName());
        }

        oldDescriptor.setNeedStartingURLSaving(newContrib.getNeedStartingURLSaving());
    }

    // Service API

    public List<String> getStartURLPatterns() {
        return startupURLs;
    }

    public List<String> getAuthChain() {
        return authChain;
    }

    public List<String> getAuthChain(HttpServletRequest request) {

        if (specificAuthChains == null || specificAuthChains.isEmpty()) {
            return authChain;
        }

        SpecificAuthChainDescriptor desc = getAuthChainDescriptor(request);

        if (desc != null) {
            return desc.computeResultingChain(authChain);
        } else {
            return authChain;
        }
    }

    public boolean doHandlePrompt(HttpServletRequest request) {
        if (specificAuthChains == null || specificAuthChains.isEmpty()) {
            return true;
        }

        SpecificAuthChainDescriptor desc = getAuthChainDescriptor(request);

        return desc != null ? desc.doHandlePrompt() : SpecificAuthChainDescriptor.DEFAULT_HANDLE_PROMPT_VALUE;

    }

    private SpecificAuthChainDescriptor getAuthChainDescriptor(HttpServletRequest request) {
        String specificAuthChainName = getSpecificAuthChainName(request);
        SpecificAuthChainDescriptor desc = specificAuthChains.get(specificAuthChainName);
        return desc;
    }

    public String getSpecificAuthChainName(HttpServletRequest request) {
        for (String specificAuthChainName : specificAuthChains.keySet()) {
            SpecificAuthChainDescriptor desc = specificAuthChains.get(specificAuthChainName);

            List<Pattern> urlPatterns = desc.getUrlPatterns();
            if (!urlPatterns.isEmpty()) {
                // test on URI
                String requestUrl = request.getRequestURI();
                for (Pattern pattern : urlPatterns) {
                    Matcher m = pattern.matcher(requestUrl);
                    if (m.matches()) {
                        return specificAuthChainName;
                    }
                }
            }

            Map<String, Pattern> headerPattern = desc.getHeaderPatterns();

            for (String headerName : headerPattern.keySet()) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null) {
                    Matcher m = headerPattern.get(headerName).matcher(headerValue);
                    if (m.matches()) {
                        return specificAuthChainName;
                    }
                }
            }
        }
        return null;
    }

    public UserIdentificationInfoCallbackHandler getCallbackHandler(UserIdentificationInfo userIdent) {
        if (cbhFactory == null) {
            return new UserIdentificationInfoCallbackHandler(userIdent);
        }
        return cbhFactory.createCallbackHandler(userIdent);
    }

    public NuxeoAuthenticationPropagator.CleanupCallback propagateUserIdentificationInformation(
            CachableUserIdentificationInfo cachableUserIdent) {
        if (propagator != null) {
            return propagator.propagateUserIdentificationInformation(cachableUserIdent);
        }
        return null;
    }

    public List<NuxeoAuthenticationPlugin> getPluginChain() {
        List<NuxeoAuthenticationPlugin> result = new ArrayList<NuxeoAuthenticationPlugin>();

        for (String pluginName : authChain) {
            if (authenticatorsDescriptors.containsKey(pluginName)
                    && authenticatorsDescriptors.get(pluginName).getEnabled()) {
                if (authenticators.containsKey(pluginName)) {
                    result.add(authenticators.get(pluginName));
                }
            }
        }
        return result;
    }

    public NuxeoAuthenticationPlugin getPlugin(String pluginName) {
        if (authenticatorsDescriptors.containsKey(pluginName)
                && authenticatorsDescriptors.get(pluginName).getEnabled()) {
            if (authenticators.containsKey(pluginName)) {
                return authenticators.get(pluginName);
            }
        }
        return null;
    }

    public AuthenticationPluginDescriptor getDescriptor(String pluginName) {
        if (authenticatorsDescriptors.containsKey(pluginName)) {
            return authenticatorsDescriptors.get(pluginName);
        } else {
            log.error("Plugin " + pluginName + " not registered or not created");
            return null;
        }
    }

    public void invalidateSession(ServletRequest request) {
        boolean done = false;
        if (!sessionManagers.isEmpty()) {
            Iterator<NuxeoAuthenticationSessionManager> it = sessionManagers.values().iterator();
            while (it.hasNext() && !(done = it.next().invalidateSession(request))) {
            }
        }
        if (!done) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    public HttpSession reinitSession(HttpServletRequest httpRequest) {
        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onBeforeSessionReinit(httpRequest);
            }
        }

        HttpSession session = httpRequest.getSession(true);

        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onAfterSessionReinit(httpRequest);
            }
        }
        return session;
    }

    public boolean canBypassRequest(ServletRequest request) {
        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                if (sm.canBypassRequest(request)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean needResetLogin(ServletRequest request) {
        if (!sessionManagers.isEmpty()) {
            for (NuxeoAuthenticationSessionManager sm : sessionManagers.values()) {
                if (sm.needResetLogin(request)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getBaseURL(ServletRequest request) {
        return VirtualHostHelper.getBaseURL(request);
    }

    public void onAuthenticatedSessionCreated(ServletRequest request, HttpSession session,
            CachableUserIdentificationInfo cachebleUserInfo) {

        NuxeoHttpSessionMonitor.instance().associatedUser(session, cachebleUserInfo.getPrincipal().getName());

        if (!sessionManagers.isEmpty()) {
            for (String smName : sessionManagers.keySet()) {
                NuxeoAuthenticationSessionManager sm = sessionManagers.get(smName);
                sm.onAuthenticatedSessionCreated(request, session, cachebleUserInfo);
            }
        }
    }

    public List<OpenUrlDescriptor> getOpenUrls() {
        return openUrls;
    }

    // preFilter management

    public synchronized void initPreFilters() {

        if (preFiltersDesc != null) {
            List<AuthPreFilterDescriptor> sortableDesc = new ArrayList<AuthPreFilterDescriptor>();

            sortableDesc.addAll(preFiltersDesc.values());

            Collections.sort(sortableDesc);

            preFilters = new ArrayList<NuxeoAuthPreFilter>();

            for (AuthPreFilterDescriptor desc : sortableDesc) {
                try {
                    NuxeoAuthPreFilter preFilter = (NuxeoAuthPreFilter) desc.getClassName().newInstance();
                    preFilters.add(preFilter);
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to create preFilter " + desc.getName() + " and class" + desc.getClassName(), e);
                }
            }
        }
    }

    public List<NuxeoAuthPreFilter> getPreFilters() {
        if (preFilters == null || preFilters.isEmpty()) {
            return null;
        }
        return preFilters;
    }

    public LoginScreenConfig getLoginScreenConfig() {
        return loginScreenConfigRegistry.getConfig();
    }

}
