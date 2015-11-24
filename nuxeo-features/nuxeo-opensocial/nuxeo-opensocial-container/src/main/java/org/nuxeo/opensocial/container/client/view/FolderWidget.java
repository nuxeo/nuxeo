package org.nuxeo.opensocial.container.client.view;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;

/**
 * @author Stéphane Fourrier
 */
public interface FolderWidget extends HasClickHandlers, HasMouseOverHandlers,
        HasMouseOutHandlers, HasDoubleClickHandlers {
    void unHighLight();

    void highLight();

    boolean isSelected();

    String getId();

    void select();

    void unSelect();
}
