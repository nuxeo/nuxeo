package org.nuxeo.theme.editor;

public class StyleLayer {

    private final String name;

    private final Integer uid;

    private final boolean selected;

    public StyleLayer(final String name, final Integer uid,
            final boolean selected) {
        this.name = name;
        this.uid = uid;
        this.selected = selected;
    }

    public String getRendered() {
        final String className = selected ? "selected" : "";
        return String.format(
                "<a href=\"javascript:void(0)\" class=\"%s\" onclick=\"NXThemesStyleEditor.setCurrentStyleLayer(%s)\" >%s</a>",
                className, uid, name);
    }
}
