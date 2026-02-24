/*
 * (C) Copyright 2006-2025 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.ArrayList;

import java.util.Collection;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;
import org.nuxeo.ecm.platform.web.common.session.NuxeoHttpSessionMonitor;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class PluggableAuthenticationService extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(PluggableAuthenticationService.class);

    public static final String NAME = "org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService";

    public static final String EP_AUTHENTICATOR = "authenticators";

    public static final String EP_SESSIONMANAGER = "sessionManager";

    public static final String EP_CHAIN = "chain";

    public static final String EP_SPECIFIC_CHAINS = "specificChains";

    public static final String EP_STARTURL = "startURL";

    public static final String EP_OPENURL = "openUrl";

    public static final String EP_LOGINSCREEN = "loginScreen";

    private Map<String, NuxeoAuthenticationPlugin> authenticators;

    private Map<String, NuxeoAuthenticationSessionManager> sessionManagers;

    private List<String> authChain;

    private final Map<String, SpecificAuthChainDescriptor> specificAuthChains = new HashMap<>();

    private final List<OpenUrlDescriptor> openUrls = new ArrayList<>();

    private final List<String> startupURLs = new ArrayList<>();

    private LoginScreenConfigRegistry loginScreenConfigRegistry;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        authChain = new ArrayList<>();
        sessionManagers = new HashMap<>();
        loginScreenConfigRegistry = new LoginScreenConfigRegistry();
    }

    @Override
    public void start(ComponentContext context) {
        authenticators = new HashMap<>();
        for (var descriptor : this.<AuthenticationPluginDescriptor> getDescriptors(EP_AUTHENTICATOR)) {
            if (descriptor.isEnabled()) {
                try {
                    NuxeoAuthenticationPlugin authPlugin = descriptor.getClassName()
                                                                     .getDeclaredConstructor()
                                                                     .newInstance();
                    authPlugin.initPlugin(descriptor.getParameters());
                    authenticators.put(descriptor.getName(), authPlugin);
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to create AuthPlugin: {} Error : {}", descriptor.getName(), e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        authenticators = null;
    }

    @Override
    public void deactivate(ComponentContext context) {
        authChain = null;
        sessionManagers = null;
        loginScreenConfigRegistry = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_AUTHENTICATOR)) {
            register(EP_AUTHENTICATOR, (AuthenticationPluginDescriptor) contribution);
        } else if (extensionPoint.equals(EP_CHAIN)) {
            AuthenticationChainDescriptor chainContrib = (AuthenticationChainDescriptor) contribution;
            log.debug("New authentication chain powered by: {}", contributor::getName);
            authChain.clear();
            authChain.addAll(chainContrib.getPluginsNames());
        } else if (extensionPoint.equals(EP_OPENURL)) {
            OpenUrlDescriptor openUrlContrib = (OpenUrlDescriptor) contribution;
            openUrls.add(openUrlContrib);
        } else if (extensionPoint.equals(EP_STARTURL)) {
            StartURLPatternDescriptor startupURLContrib = (StartURLPatternDescriptor) contribution;
            startupURLs.addAll(startupURLContrib.getStartURLPatterns());
        } else if (extensionPoint.equals(EP_SESSIONMANAGER)) {
            SessionManagerDescriptor smContrib = (SessionManagerDescriptor) contribution;
            if (smContrib.enabled) {
                try {
                    NuxeoAuthenticationSessionManager sm = smContrib.getClassName()
                                                                    .getDeclaredConstructor()
                                                                    .newInstance();
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
        } else if (extensionPoint.equals(EP_LOGINSCREEN)) {
            LoginScreenConfig newConfig = (LoginScreenConfig) contribution;
            registerLoginScreenConfig(newConfig);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(EP_AUTHENTICATOR)) {
            unregister(EP_AUTHENTICATOR, (AuthenticationPluginDescriptor) contribution);
        } else if (extensionPoint.equals(EP_LOGINSCREEN)) {
            LoginScreenConfig newConfig = (LoginScreenConfig) contribution;
            unregisterLoginScreenConfig(newConfig);
        }
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

    /**
     * @return all plugins contributed to the authenticators extension point
     * @since 2023.44
     */
    public Collection<NuxeoAuthenticationPlugin> getAuthenticatorPlugins() {
        return Collections.unmodifiableCollection(authenticators.values());
    }

    public List<NuxeoAuthenticationPlugin> getPluginChain() {
        return authChain.stream().filter(authenticators::containsKey).map(authenticators::get).toList();
    }

    public NuxeoAuthenticationPlugin getPlugin(String pluginName) {
        return authenticators.get(pluginName);
    }

    public AuthenticationPluginDescriptor getDescriptor(String pluginName) {
        AuthenticationPluginDescriptor descriptor = getDescriptor(EP_AUTHENTICATOR, pluginName);
        if (descriptor == null) {
            log.error("Plugin: {} not registered or not created", pluginName);
        }
        return descriptor;
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

    public LoginScreenConfig getLoginScreenConfig() {
        return loginScreenConfigRegistry.getConfig();
    }

    /**
     * @since 10.10
     */
    public void registerLoginScreenConfig(LoginScreenConfig config) {
        loginScreenConfigRegistry.addContribution(config);
    }

    /**
     * @since 10.10
     */
    public void unregisterLoginScreenConfig(LoginScreenConfig config) {
        loginScreenConfigRegistry.removeContribution(config);
    }

}
