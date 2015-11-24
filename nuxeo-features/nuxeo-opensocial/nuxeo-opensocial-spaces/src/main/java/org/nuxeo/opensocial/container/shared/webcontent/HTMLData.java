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

    public static final String CENTER_TEMPLATE = "center";

    public static final String RIGHT_TEMPLATE = "right";

    public static final String LEFT_TEMPLATE = "left";

    public static final String TYPE = "wchtml";

    private String html;

    private String htmlTitle;

    private String htmlPictureLink;

    private String htmlPictureLegend;

    private boolean hasPicture;

    private String template;

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

    public void setHtmlPictureLink(String htmlPictureLink) {
        this.htmlPictureLink = htmlPictureLink;
    }

    public String getHtmlPictureLink() {
        return htmlPictureLink;
    }

    public void setHtmlPictureLegend(String htmlPictureLegend) {
        this.htmlPictureLegend = htmlPictureLegend;
    }

    public String getHtmlPictureLegend() {
        return htmlPictureLegend;
    }

    public void setHasPicture(boolean hasPicture) {
        this.hasPicture = hasPicture;
    }

    public boolean hasPicture() {
        return hasPicture;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
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

    public boolean hasFiles() {
        return true;
    }
}
