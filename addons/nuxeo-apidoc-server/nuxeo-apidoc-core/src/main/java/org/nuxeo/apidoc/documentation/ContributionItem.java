/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.documentation;

import org.apache.commons.text.StringEscapeUtils;

public class ContributionItem {

    protected String tagName;

    protected String nameOrId;

    protected String documentation;

    protected String xml;

    public void write(StringBuffer sb) {
        sb.append("\n\n<div>");
        sb.append("\n<div>");
        sb.append(tagName);
        if (nameOrId != null) {
            sb.append(" ");
            sb.append(nameOrId);
        }
        sb.append("</div>");

        sb.append("\n<p>");
        sb.append(DocumentationHelper.getHtml(documentation));
        sb.append("</p>");

        sb.append("\n<code>");
        sb.append(StringEscapeUtils.escapeHtml4(xml));
        sb.append("</code>");

        sb.append("</div>");
    }

    public String getLabel() {
        StringBuffer sb = new StringBuffer();
        sb.append(tagName);
        if (nameOrId != null) {
            sb.append(" ");
            sb.append(nameOrId);
        }
        return sb.toString();
    }

    public String getId() {
        return nameOrId;
    }

    public String getDocumentation() {
        return DocumentationHelper.getHtml(documentation);
    }

    public String getXml() {
        return StringEscapeUtils.escapeHtml4(xml);
    }

    public String getRawXml() {
        return xml;
    }

}
