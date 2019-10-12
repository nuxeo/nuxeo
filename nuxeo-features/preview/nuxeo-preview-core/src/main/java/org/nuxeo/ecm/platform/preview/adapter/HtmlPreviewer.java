/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.preview.adapter;

import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.runtime.api.Framework;

/**
 * Previewer for HTML content.
 * <p>
 * It escape things differently than the plain text previewer.
 */
public class HtmlPreviewer extends PlainTextPreviewer implements MimeTypePreviewer {

    @Override
    public String htmlContent(String content) {
        HtmlSanitizerService sanitizer = Framework.getService(HtmlSanitizerService.class);
        if (sanitizer == null) {
            throw new RuntimeException("Cannot find HtmlSanitizerService");
        }
        return sanitizer.sanitizeString(content, null);
    }

}
