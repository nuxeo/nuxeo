/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
