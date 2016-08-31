/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

/**
 * Servlet that returns local SP metadata for configuring IdPs.
 *
 * @since 6.0
 */
public class MetadataServlet extends HttpServlet {

    protected static final Log log = LogFactory.getLog(MetadataServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String baseURL = VirtualHostHelper.getBaseURL(request);
        baseURL += (baseURL.endsWith("/") ? "" : "/") + LoginScreenHelper.getStartupPagePath();

        EntityDescriptor descriptor = SAMLConfiguration.getEntityDescriptor(baseURL);

        try {
            Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(descriptor);
            if (marshaller == null) {
                log.error("Unable to marshall message, no marshaller registered for message object: "
                        + descriptor.getElementQName());
            }
            Element dom = marshaller.marshall(descriptor);
            XMLHelper.writeNode(dom, response.getWriter());
        } catch (MarshallingException e) {
            log.error("Unable to write metadata.");
        }
    }
}
