package org.nuxeo.opensocial.container.shared.layout.api;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;

/**
 * @author Stéphane Fourrier
 */
public interface YUIComponentZone extends Serializable {
    void addComponent(YUIComponent component);

    void setTemplate(YUITemplate template);

    YUITemplate getTemplate();
}
