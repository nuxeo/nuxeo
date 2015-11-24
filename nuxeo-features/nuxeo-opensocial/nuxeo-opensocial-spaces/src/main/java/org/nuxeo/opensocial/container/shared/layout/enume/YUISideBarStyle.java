package org.nuxeo.opensocial.container.shared.layout.enume;

/**
 * @author Stéphane Fourrier
 */
public enum YUISideBarStyle {
    YUI_SB_NO_COLUMN(0, "yui-t7", "Aucune"), YUI_SB_LEFT_160PX(160, "yui-t1",
            "A gauche (160px)"), YUI_SB_LEFT_180PX(180, "yui-t2",
            "A gauche (180px)"), YUI_SB_LEFT_300PX(300, "yui-t3",
            "A gauche (300px)"), YUI_SB_RIGHT_180PX(180, "yui-t4",
            "A droite (180px)"), YUI_SB_RIGHT_240PX(240, "yui-t5",
            "A droite (240px)"), YUI_SB_RIGHT_300PX(300, "yui-t6",
            "A droite (300px)");

    private int size;

    private String CSS;

    private String description;

    private YUISideBarStyle(int size, String CSS, String description) {
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
