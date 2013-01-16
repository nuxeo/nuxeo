package org.nuxeo.opensocial.container.shared.layout.api;

import java.util.List;

/**
 * @author Stéphane Fourrier
 */
public interface YUIContent {
    public String getId();

    public void setId(String id);

    public List<YUIComponent> getComponents();

    public void addComponent(YUIComponent component);

    public void removeComponent(YUIComponent component);
}
