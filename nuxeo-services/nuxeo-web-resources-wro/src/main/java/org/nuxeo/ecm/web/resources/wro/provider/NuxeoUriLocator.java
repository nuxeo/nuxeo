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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.wro.provider;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.resource.locator.UriLocator;
import ro.isdc.wro.model.resource.locator.factory.UriLocatorFactory;

/**
 * Nuxeo URI locator, made available to all wro resources thanks to {@link NuxeoConfigurableProvider}.
 *
 * @since 7.3
 */
public class NuxeoUriLocator implements UriLocator {

    private static final Log log = LogFactory.getLog(NuxeoUriLocator.class);

    public static final String ALIAS = "nuxeoUri";

    @Inject
    UriLocatorFactory uriLocatorFactory;

    @Override
    public boolean accept(String uri) {
        return uri != null && uri.startsWith(Resource.PREFIX);
    }

    @Override
    public InputStream locate(String uri) throws IOException {
        Resource resource = getResource(uri);
        if (resource != null) {
            String ruri = resource.getURI();
            if (ruri == null) {
                log.error("Cannot handle resource '" + resource.getName() + "': no resolved uri");
                return null;
            }
            final UriLocator uriLocator = uriLocatorFactory.getInstance(ruri);
            if (uriLocator != null) {
                return uriLocator.locate(ruri);
            }
        }
        return null;
    }

    public static Resource getResource(String uri) {
        // resolve resource from Nuxeo service
        String name = uri.substring(Resource.PREFIX.length());
        WebResourceManager service = Framework.getService(WebResourceManager.class);
        return service.getResource(name);
    }

    public static String getUri(Resource resource) {
        return Resource.PREFIX + resource.getName();
    }

    public static boolean isProcessorEnabled(String alias, String uri) {
        Resource res = getResource(uri);
        if (res != null) {
            return res.getProcessors().contains(alias);
        }
        return false;
    }

}
