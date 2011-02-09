package org.nuxeo.opensocial.container.shared.webcontent;

import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.abs.AbstractWebContentData;

/**
 * @author Stéphane Fourrier
 */
public class HTMLData extends AbstractWebContentData {
    private static final long serialVersionUID = 1L;

    public static final String HTML_PREFERENCE = "WC_HTML_HTML";
    public static final String TITLE_PREFERENCE = "WC_HTML_TITLE";

    public static final String ICONE_NAME = "richtext-icon";

    public static String TYPE = new String("wchtml");

    private String html;
    private String htmlTitle;

    public HTMLData() {
        super();
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getHtmlTitle() {
        return htmlTitle;
    }

    public void setHtmlTitle(String htmlTitle) {
        this.htmlTitle = htmlTitle;
    }

    @Override
    public boolean initPrefs(Map<String, String> params) {
        if (params.get(HTML_PREFERENCE) != null) {
            setHtml(params.get(HTML_PREFERENCE));
        }

        if (params.get(TITLE_PREFERENCE) != null) {
            setHtmlTitle(params.get(TITLE_PREFERENCE));
        }

        return super.initPrefs(params);
    }

    public void updateFrom(WebContentData data) {
        // TODO
    }

    @Override
    public String getAssociatedType() {
        return TYPE;
    }

    @Override
    public String getIcon() {
        return ICONE_NAME;
    }
}
