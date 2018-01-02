/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.adapter;

import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author Alexandre Russel
 */
public class AbstractPreviewer {

    protected String getPreviewTitle(DocumentModel dm) {
        StringBuilder sb = new StringBuilder();

        sb.append(dm.getTitle());
        sb.append(" ");
        String vl = dm.getVersionLabel();
        if (vl != null) {
            sb.append(vl);
        }
        sb.append(" (preview)");

        String title = sb.toString();
        return StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(title));
    }

}
