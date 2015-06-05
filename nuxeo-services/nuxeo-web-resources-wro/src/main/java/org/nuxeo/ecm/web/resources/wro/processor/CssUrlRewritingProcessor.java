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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.wro.processor;

import ro.isdc.wro.config.ReadOnlyContext;
import ro.isdc.wro.model.group.Inject;

/**
 * CSS URL rewriting processor, handling basePath variable replacement, and avoiding crash when one given URL cannot be
 * rewritten.
 *
 * @since 7.4
 */
public class CssUrlRewritingProcessor extends ro.isdc.wro.model.resource.processor.impl.css.CssUrlRewritingProcessor {

    protected static final String BASE_PATH_REGEXP = "\\$\\{basePath}";

    @Inject
    private ReadOnlyContext context;

    @Override
    protected String replaceImageUrl(String cssUri, String imageUrl) {
        // Can be null when using standalone context.
        final String contextPath = context.getRequest() != null ? context.getRequest().getContextPath() : null;
        String finalImageUrl = imageUrl;
        if (imageUrl != null) {
            finalImageUrl = imageUrl.replaceAll(BASE_PATH_REGEXP, contextPath);
        }
        return finalImageUrl;
    }

}
