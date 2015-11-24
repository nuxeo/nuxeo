package org.nuxeo.opensocial.container.shared.layout.api;

import java.util.List;

/**
 * @author Stéphane Fourrier
 */
public interface YUIComponent {
    public void setCSS(String CSSClass);

    public String getCSS();

    public List<YUIComponent> getComponents();

    /**
     * Returns the unique name of the unit in the layout
     *
     * @return
     */
    public String getId();

    public YUIComponent getACopyFor();

    public void setId(String id);
}
