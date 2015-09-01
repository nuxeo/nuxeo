/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
            if (Framework.isTestModeSet()) {
                return content;
            }
            throw new RuntimeException("Cannot find HtmlSanitizerService");
        }
        return sanitizer.sanitizeString(content, null);
    }

}
