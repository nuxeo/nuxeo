package org.nuxeo.opensocial.container.shared.webcontent;

import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.abs.AbstractWebContentData;

/**
 * @author Stéphane Fourrier
 */
public class PictureData extends AbstractWebContentData {
    private static final long serialVersionUID = 1L;

    public static final String TITLE_PREFERENCE = "WC_PICTURE_TITLE";

    private static final String ICONE_NAME = "photo-icon";

    public static String TYPE = "wcpicture";

    private String pictureTitle;
    private String pictureLegend;
    private String pictureLink;

    public PictureData() {
        super();
    }

    public void setPictureTitle(String pictureTitle) {
        this.pictureTitle = pictureTitle;
    }

    public String getPictureTitle() {
        return pictureTitle;
    }

    public void setPictureLegend(String pictureLegend) {
        this.pictureLegend = pictureLegend;
    }

    public String getPictureLegend() {
        return pictureLegend;
    }

    public void setPictureLink(String pictureLink) {
        this.pictureLink = pictureLink;
    }

    public String getPictureLink() {
        return pictureLink;
    }

    @Override
    public boolean initPrefs(Map<String, String> params) {
        if (params.get(TITLE_PREFERENCE) != null) {
            setPictureTitle(params.get(TITLE_PREFERENCE));
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
