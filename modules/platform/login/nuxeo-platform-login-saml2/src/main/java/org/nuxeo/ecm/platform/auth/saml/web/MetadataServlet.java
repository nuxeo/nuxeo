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

import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.getStartPageURL;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
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

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String baseURL = getStartPageURL(request);

        EntityDescriptor descriptor = SAMLConfiguration.getEntityDescriptor(baseURL);

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
