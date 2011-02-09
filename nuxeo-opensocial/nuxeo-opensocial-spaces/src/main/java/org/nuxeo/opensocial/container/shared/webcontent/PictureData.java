package org.nuxeo.opensocial.container.shared.webcontent;

import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.abs.AbstractWebContentData;

/**
 * @author Stéphane Fourrier
 */
public class PictureData extends AbstractWebContentData {
    private static final long serialVersionUID = 1L;

    public static final String TITLE_PREFERENCE = "WC_PICTURE_TITLE";
    public static final String URL_PREFERENCE = "WC_PICTURE_URL";

    private static final String ICONE_NAME = "photo-icon";

    public static String TYPE = new String("wcpicture");

    private String url;
    private String pictureTitle;

    public PictureData() {
        super();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPictureTitle(String pictureTitle) {
        this.pictureTitle = pictureTitle;
    }

    public String getPictureTitle() {
        return pictureTitle;
    }

    @Override
    public boolean initPrefs(Map<String, String> params) {
        if (params.get(TITLE_PREFERENCE) != null) {
            setPictureTitle(params.get(TITLE_PREFERENCE));
        }

        if (params.get(URL_PREFERENCE) != null) {
            setUrl(params.get(URL_PREFERENCE));
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
