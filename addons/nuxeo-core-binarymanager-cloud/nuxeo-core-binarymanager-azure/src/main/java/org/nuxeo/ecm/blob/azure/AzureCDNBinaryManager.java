/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.blob.azure;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.blob.ManagedBlob;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureCDNBinaryManager extends AzureBinaryManager {

    // Resolved using AzureBinaryManager#PROPERTIES_PREFIX as prefix
    public static final String AZURE_CDN_PROPERTY = "cdn.host";

    protected String host;

    @Override
    protected void setupCloudClient() throws IOException {
        super.setupCloudClient();
        host = getProperty(AZURE_CDN_PROPERTY);
    }

    @Override
    protected URI getRemoteUri(String digest, ManagedBlob blob, HttpServletRequest servletRequest) throws IOException {
        URI azure = super.getRemoteUri(digest, blob, servletRequest);

        // Just replace default Azure storage host with the CDN one
        String cdn = azure.toString().replace(azure.getHost(), host);
        return URI.create(cdn);
    }
}
