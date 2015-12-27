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

    protected static final String BASE_PATH = "${basePath}";

    @Inject
    private ReadOnlyContext context;

    @Override
    protected String replaceImageUrl(String cssUri, String imageUrl) {
        // Can be null when using standalone context.
        final String contextPath = context.getRequest() != null ? context.getRequest().getContextPath() : null;
        String finalImageUrl = imageUrl;
        if (imageUrl != null) {
            // some resources begin with ../ instead of BASE_PATH (e.g: ../img/fancybox.png)
            if (!imageUrl.contains(BASE_PATH) && imageUrl.startsWith("../")) {
                finalImageUrl = imageUrl.replace("../", contextPath + "/");
            } else {
                finalImageUrl = imageUrl.replaceAll(BASE_PATH_REGEXP, contextPath);
            }
        }
        return finalImageUrl;
    }

}
