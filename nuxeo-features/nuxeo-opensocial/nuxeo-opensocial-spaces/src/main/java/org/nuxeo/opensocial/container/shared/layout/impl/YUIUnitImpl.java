package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

/**
 * @author Stéphane Fourrier
 */
public class YUIUnitImpl extends YUIAbstractComponent implements YUIUnit,
        Serializable {
    private static String UNIT_CSS_CLASS = "yui-u";

    private static final long serialVersionUID = 1L;

    private String CSS;

    /**
     * Constructeur de YUIUnit : permet de creer une unité qui sert de
     * container aux contenu web
     */
    public YUIUnitImpl(String id) {
        this();
        setId(id);
    }

    /**
     * Constructeur de YUIUnit : permet de creer une unité qui sert de
     * container aux contenu web
     */
    public YUIUnitImpl() {
        setCSS(UNIT_CSS_CLASS);
    }

    public String getCSS() {
        return this.CSS;
    }

    public void setCSS(String CSS) {
        this.CSS = CSS;
    }

    @Override
    public List<YUIComponent> getComponents() {
        return null;
    }

    public YUIComponent getACopyFor() {
        YUIUnitImpl unit = new YUIUnitImpl();

        unit.setCSS(getCSS());
        return unit;
    }
}
