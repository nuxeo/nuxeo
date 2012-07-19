package org.nuxeo.apidoc.documentation;

import org.apache.commons.lang.StringEscapeUtils;

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
        sb.append(StringEscapeUtils.escapeHtml(xml));
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

    public String getDocumentation() {
        return DocumentationHelper.getHtml(documentation);
    }

    public String getXml() {
        return StringEscapeUtils.escapeHtml(xml);
    }

}
