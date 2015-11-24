package org.nuxeo.opensocial.container.shared.layout.enume;

/**
 * @author Stéphane Fourrier
 */
public enum YUISize {
    YUI_BS_750_PX(750, "doc", "750px"), YUI_BS_950_PX(950, "doc2", "950px"), YUI_BS_974_PX(
            974, "doc4", "974px"), YUI_BS_FULL_PAGE(-1, "doc3", "100%");

    private int size;

    private String CSS;

    private String description;

    private YUISize(int size, String CSS, String description) {
        this.size = size;
        this.CSS = CSS;
        this.description = description;
    }

    public int getSize() {
        return this.size;
    }

    public String getCSS() {
        return this.CSS;
    }

    public String getDescription() {
        return this.description;
    }
}
