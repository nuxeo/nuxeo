package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;

/**
 * @author Stéphane Fourrier
 */
public abstract class YUIAbstractComponent implements Serializable,
        YUIComponent {
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private static final long serialVersionUID = 1L;

    public abstract void setCSS(String CSSClass);

    public abstract String getCSS();

    public abstract List<YUIComponent> getComponents();

}
