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
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
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

    private List<NuxeoAuthenticationPlugin> authPluginChain;

    @Override
    public void start(ComponentContext context) {
        authenticators = new HashMap<>();
        this.<AuthenticationPluginDescriptor> getRegistryContributions(EP_AUTHENTICATOR).forEach(desc -> {
            String name = desc.getName();
            try {
                NuxeoAuthenticationPlugin authPlugin = desc.getClassName().getDeclaredConstructor().newInstance();
                authPlugin.initPlugin(desc.getParameters());
                authenticators.put(name, authPlugin);
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Unable to create AuthPlugin with name '%s' (%s)", name, e.getMessage());
                addRuntimeMessage(Level.ERROR, msg);
                log.error(msg, e);
            }
        });
        authChain = this.<AuthenticationChainDescriptor> getRegistryContribution(EP_CHAIN)
                        .map(AuthenticationChainDescriptor::getPluginsNames)
                        .orElse(Collections.emptyList());
        authPluginChain = authChain.stream()
                                   .filter(authenticators::containsKey)
                                   .map(authenticators::get)
                                   .collect(Collectors.toList());
        sessionManagers = new HashMap<>();
        this.<SessionManagerDescriptor> getRegistryContributions(EP_SESSIONMANAGER).forEach(desc -> {
            String name = desc.getName();
            try {
                NuxeoAuthenticationSessionManager sm = desc.getClassName().getDeclaredConstructor().newInstance();
                sessionManagers.put(name, sm);
            } catch (ReflectiveOperationException e) {
                log.error("Unable to create session manager", e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        authenticators = null;
        authChain = null;
        authPluginChain = null;
        sessionManagers = null;
    }

    // Service API

    public List<String> getStartURLPatterns() {
        return this.<StartURLPatternDescriptor> getRegistryContribution(EP_STARTURL)
                   .map(StartURLPatternDescriptor::getStartURLPatterns)
                   .orElse(Collections.emptyList());
    }

    public List<String> getAuthChain() {
        return Collections.unmodifiableList(authChain);
    }

    public List<String> getAuthChain(HttpServletRequest request) {
        List<SpecificAuthChainDescriptor> specificAuthChains = getRegistryContributions(EP_SPECIFIC_CHAINS);
        // if there are no contributions, skip the logic to find a descriptor applying to the request
        if (specificAuthChains.isEmpty()) {
            return authChain;
        }
        return getAuthChainDescriptor(request).map(desc -> desc.computeResultingChain(authChain)).orElse(authChain);
    }

    public boolean doHandlePrompt(HttpServletRequest request) {
        List<SpecificAuthChainDescriptor> specificAuthChains = getRegistryContributions(EP_SPECIFIC_CHAINS);
        // if there are no contributions, skip the logic to find a descriptor applying to the request
        if (specificAuthChains.isEmpty()) {
            return true;
        }
        return getAuthChainDescriptor(request).map(SpecificAuthChainDescriptor::doHandlePrompt)
                                              .orElse(SpecificAuthChainDescriptor.DEFAULT_HANDLE_PROMPT_VALUE);

    }

    private Optional<SpecificAuthChainDescriptor> getAuthChainDescriptor(HttpServletRequest request) {
        String specificAuthChainName = getSpecificAuthChainName(request);
        return getRegistryContribution(EP_SPECIFIC_CHAINS, specificAuthChainName);
    }

    public String getSpecificAuthChainName(HttpServletRequest request) {
        List<SpecificAuthChainDescriptor> specificAuthChains = getRegistryContributions(EP_SPECIFIC_CHAINS);
        for (SpecificAuthChainDescriptor desc : specificAuthChains) {
            String name = desc.getName();
            String requestUrl = request.getRequestURI();
            // test on URI
            for (Pattern pattern : desc.getUrlPatterns()) {
                Matcher m = pattern.matcher(requestUrl);
                if (m.matches()) {
                    return name;
                }
            }
            for (Map.Entry<String, Pattern> entry : desc.getHeaderPatterns().entrySet()) {
                String headerValue = request.getHeader(entry.getKey());
                if (headerValue != null) {
                    Matcher m = entry.getValue().matcher(headerValue);
                    if (m.matches()) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    public List<NuxeoAuthenticationPlugin> getPluginChain() {
        return Collections.unmodifiableList(authPluginChain);
    }

    public NuxeoAuthenticationPlugin getPlugin(String pluginName) {
        return authenticators.get(pluginName);
    }

    public AuthenticationPluginDescriptor getDescriptor(String pluginName) {
        return this.<AuthenticationPluginDescriptor> getRegistryContribution(EP_AUTHENTICATOR, pluginName)
                   .orElseGet(() -> {
                       log.error("Plugin '{}' not registered or not created", pluginName);
                       return null;
                   });
    }

    public void invalidateSession(ServletRequest request) {
        boolean done = false;
        Iterator<NuxeoAuthenticationSessionManager> it = sessionManagers.values().iterator();
        while (it.hasNext() && !(done = it.next().invalidateSession(request))) {
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
        sessionManagers.values().forEach(sm -> sm.onBeforeSessionReinit(httpRequest));
        HttpSession session = httpRequest.getSession(true);
        sessionManagers.values().forEach(sm -> sm.onAfterSessionReinit(httpRequest));
        return session;
    }

    public boolean canBypassRequest(ServletRequest request) {
        return sessionManagers.values().stream().anyMatch(sm -> sm.canBypassRequest(request));
    }

    public boolean needResetLogin(ServletRequest request) {
        return sessionManagers.values().stream().anyMatch(sm -> sm.needResetLogin(request));
    }

    public String getBaseURL(ServletRequest request) {
        return VirtualHostHelper.getBaseURL(request);
    }

    public void onAuthenticatedSessionCreated(ServletRequest request, HttpSession session,
            CachableUserIdentificationInfo cachebleUserInfo) {
        NuxeoHttpSessionMonitor.instance().associatedUser(session, cachebleUserInfo.getPrincipal().getName());
        sessionManagers.values().forEach(sm -> sm.onAuthenticatedSessionCreated(request, session, cachebleUserInfo));
    }

    public List<OpenUrlDescriptor> getOpenUrls() {
        return getRegistryContributions(EP_OPENURL);
    }

    public LoginScreenConfig getLoginScreenConfig() {
        return this.<LoginScreenConfig> getRegistryContribution(EP_LOGINSCREEN).orElse(null);
    }

    protected LoginScreenConfigRegistry getLoginScreenConfigRegistry() {
        return getExtensionPointRegistry(EP_LOGINSCREEN);
    }

    /**
     * @since 10.10
     */
    public void registerLoginScreenConfig(LoginScreenConfig config) {
        getLoginScreenConfigRegistry().addContribution(config);
    }

    /**
     * @since 10.10
     */
    public void unregisterLoginScreenConfig(LoginScreenConfig config) {
        getLoginScreenConfigRegistry().removeContribution(config);
    }

}
