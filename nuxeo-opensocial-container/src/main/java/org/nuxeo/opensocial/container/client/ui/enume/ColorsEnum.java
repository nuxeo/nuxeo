package org.nuxeo.opensocial.container.client.ui.enume;

/**
 * @author Stéphane Fourrier
 */
public enum ColorsEnum {
    RED("FF0000"), PINK("FF0066"), ORANGE("FF6600"), GREEN("99CC00"), BLUE(
            "66CCFF"), DARK_GREY("999999"), GREY("CCCCCC"), WHITE("FFFFFF"), NONE(
            "none");

    private String cssColor;

    private ColorsEnum(String cssColor) {
        this.cssColor = cssColor;
    }

    public String getCssColor() {
        return this.cssColor;
    }
}
