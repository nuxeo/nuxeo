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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.web.resources.wro.processor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.web.resources.wro.provider.NuxeoUriLocator;

import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.ReadOnlyContext;
import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;

/**
 *
 * Extends this class to implement a flavor-based processor.
 *
 * @since 7.4
 */
public abstract class AbstractFlavorProcessor implements ResourcePreProcessor {

    @Inject
    protected ReadOnlyContext context;

    public abstract String getAlias();

    @Override
    public void process(Resource resource, Reader reader, Writer writer) throws IOException {
        String flavor = getFlavor();
        if (!StringUtils.isBlank(flavor) && isEnabled(resource)) {
            process(resource, reader, writer, flavor);
        } else {
            process(resource, reader, writer, null);
        }
    }

    protected abstract void process(final Resource resource, final Reader reader, final Writer writer,
            String flavorName) throws IOException;

    public String getEncoding() {
        return Context.isContextSet() ? context.getConfig().getEncoding() : WroConfiguration.DEFAULT_ENCODING;
    }

    protected String getFlavor() {
        String queryString = context.getRequest().getQueryString();
        if (queryString != null) {
            Map<String, String> params = URIUtils.getRequestParameters(queryString);
            if (params != null && params.containsKey("flavor")) {
                return params.get("flavor");
            }
        }
        return null;
    }

    protected boolean isEnabled(final Resource resource) {
        return NuxeoUriLocator.isProcessorEnabled(getAlias(), resource.getUri());
    }
}
