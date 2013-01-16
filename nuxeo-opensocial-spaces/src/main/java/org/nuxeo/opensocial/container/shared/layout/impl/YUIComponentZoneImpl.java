package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;

/**
 * @author Stéphane Fourrier
 */
public class YUIComponentZoneImpl extends YUIAbstractComponent implements
        Serializable, YUIComponentZone {
    private static String FIRST_COMPONENT_CSS_CLASS = " first";

    private static final long serialVersionUID = 1L;

    private List<YUIComponent> listComponents;

    private YUITemplate template;

    @SuppressWarnings("unused")
    private YUIComponentZoneImpl() {
    }

    public YUIComponentZoneImpl(YUITemplate template) {
        this.template = template;
        listComponents = new ArrayList<YUIComponent>();
    }

    @Override
    public List<YUIComponent> getComponents() {
        return listComponents;
    }

    public void clearComponents() {
        listComponents = new ArrayList<YUIComponent>(
                template.getNumberOfComponents());
    }

    public void removeComponent(int index) {
        listComponents.remove(index);
    }

    public void addComponent(YUIComponent component) {
        if (listComponents.size() == 0) {
            ((YUIComponent) component).setCSS(component.getCSS()
                    + FIRST_COMPONENT_CSS_CLASS);
        }

        if (listComponents.size() < this.template.getNumberOfComponents()) {
            listComponents.add(component);
        }
    }

    @Override
    public String getCSS() {
        return this.template.getCSS();
    }

    @Override
    public void setCSS(String CSS) {
    }

    public void setTemplate(YUITemplate template) {
        this.template = template;
    }

    public YUITemplate getTemplate() {
        return this.template;
    }

    public YUIComponent getACopyFor() {
        YUIComponentZoneImpl zone = new YUIComponentZoneImpl(getTemplate());

        for (YUIComponent comp : getComponents()) {
            YUIComponent componentToCopy = comp.getACopyFor();
            zone.addComponent(componentToCopy);
        }

        return zone;
    }
}
