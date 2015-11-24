package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIContent;

/**
 * @author Stéphane Fourrier
 */
public class YUIContentImpl implements YUIContent, Serializable {
    private static final long serialVersionUID = 1L;

    private static String CONTENT_ID = "bd";

    private List<YUIComponent> listComponents;

    private String id;

    public YUIContentImpl() {
        setId(CONTENT_ID);
        listComponents = new ArrayList<YUIComponent>();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<YUIComponent> getComponents() {
        return listComponents;
    }

    public void addComponent(YUIComponent component) {
        listComponents.add(component);
    }

    public void removeComponent(YUIComponent component) {
        listComponents.remove(component);
    }
}
