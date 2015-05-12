/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */

package org.nuxeo.ecm.platform.auth.saml.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet that returns local SP metadata for configuring IdPs.
 *
 * @since 6.0
 */
public class MetadataServlet extends HttpServlet {

    protected static final Log log = LogFactory.getLog(MetadataServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String baseURL = VirtualHostHelper.getBaseURL(request);
        baseURL += (baseURL.endsWith("/") ? "" : "/") + NuxeoAuthenticationFilter.DEFAULT_START_PAGE;

        EntityDescriptor descriptor = SAMLConfiguration.getEntityDescriptor(baseURL);

        try {
            Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(
                    descriptor);
            if (marshaller == null) {
                log.error(
                        "Unable to marshall message, no marshaller registered for message object: "
                                + descriptor.getElementQName());
            }
            Element dom = marshaller.marshall(descriptor);
            XMLHelper.writeNode(dom, response.getWriter());
        } catch (MarshallingException e) {
            log.error("Unable to write metadata.");
        }
    }
}
