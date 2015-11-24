package org.nuxeo.opensocial.container.shared.layout.enume;

/**
 * @author Stéphane Fourrier
 */
public enum YUITemplate {
    YUI_ZT_100(1, "", "1 Col. (100)"), YUI_ZT_50_50(2, "yui-g",
            "2 Col. (50/50)"), YUI_ZT_66_33(2, "yui-gc", "2 Col. (66/33)"), YUI_ZT_33_66(
            2, "yui-gd", "2 Col. (33/66)"), YUI_ZT_75_25(2, "yui-ge",
            "2 Col. (75/25)"), YUI_ZT_25_75(2, "yui-gf", "2 Col. (25/75)"), YUI_ZT_33_33_33(
            3, "yui-gb", "3 Col. (33/33/33)");

    private final int numberOfComponents;

    private final String CSS;

    private final String description;

    private YUITemplate(int numberOfComponents, String CSS, String description) {
        this.numberOfComponents = numberOfComponents;
        this.CSS = CSS;
        this.description = description;
    }

    public String getCSS() {
        return this.CSS;
    }

    public int getNumberOfComponents() {
        return this.numberOfComponents;
    }

    public String getDescription() {
        return this.description;
    }
}
