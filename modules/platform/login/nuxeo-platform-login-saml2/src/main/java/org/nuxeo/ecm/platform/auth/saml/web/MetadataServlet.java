/*
 * (C) Copyright 2014-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.web;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.getStartPageURL;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.auth.saml.SAMLAuthenticationProvider;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * Servlet that returns local SP metadata for configuring IdPs.
 *
 * @since 6.0
 */
public class MetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(MetadataServlet.class);

    protected static final String PARAMETER_PLUGIN_NAME = "pluginName";

    protected static final String PARAMETER_ENTITY_ID = "entityId";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String baseURL = getStartPageURL(request);

        // retrieve the requested SAML authenticator
        // - one asked with the pluginName query parameter
        // - one asked with the SAML SP entityId query parameter
        // - the default one if none parameter is provided
        String pluginNameParameter = request.getParameter(PARAMETER_PLUGIN_NAME);
        String entityIdParameter = request.getParameter(PARAMETER_ENTITY_ID);
        SAMLConfiguration configuration;
        if (isNotBlank(pluginNameParameter)) {
            var plugin = Framework.getService(PluggableAuthenticationService.class).getPlugin(pluginNameParameter);
            if (!(plugin instanceof SAMLAuthenticationProvider samlPlugin)) {
                throw new IllegalArgumentException("Plugin: " + pluginNameParameter + " not found or not a SAML one");
            }
            configuration = samlPlugin.getConfiguration();
        } else if (isNotBlank(entityIdParameter)) {
            configuration = Framework.getService(PluggableAuthenticationService.class)
                                     .getAuthenticatorPlugins()
                                     .stream()
                                     .<SAMLAuthenticationProvider> mapMulti((plugin, downstream) -> {
                                         if (plugin instanceof SAMLAuthenticationProvider samlPlugin) {
                                             downstream.accept(samlPlugin);
                                         }
                                     })
                                     .map(SAMLAuthenticationProvider::getConfiguration)
                                     .findFirst()
                                     .orElseThrow(() -> new IllegalArgumentException(
                                             "SAML Plugin with entityId: " + entityIdParameter + " not found"));
        } else {
            configuration = SAMLConfiguration.retrieveDefaultPluginConfiguration();
        }
        EntityDescriptor descriptor = configuration.createSPEntityDescriptor(baseURL);

        try {
            var marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(descriptor);
            if (marshaller == null) {
                log.error("Unable to marshall message, no marshaller registered for message object: {}",
                        descriptor::getElementQName);
                return;
            }
            Element dom = marshaller.marshall(descriptor);
            SerializeSupport.writeNode(dom, response.getOutputStream());
        } catch (MarshallingException e) {
            log.error("Unable to write metadata.");
        }
    }
}
