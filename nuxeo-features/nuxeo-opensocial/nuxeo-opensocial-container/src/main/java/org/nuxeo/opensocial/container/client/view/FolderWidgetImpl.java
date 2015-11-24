package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.presenter.AppPresenter;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

/**
 * @author Stéphane Fourrier
 */
class FolderWidgetImpl extends Composite implements FolderWidget {
    private FlowPanel layout;

    private boolean isSelected = false;

    private String id;

    public FolderWidgetImpl(String name, String id) {
        layout = new FlowPanel();
        layout.setStyleName("folder");
        this.id = id;

        Image folder = new Image(AppPresenter.images.folder());
        layout.add(folder);

        Label title = new Label(name);
        title.setStyleName("name");
        layout.add(title);

        initWidget(layout);
    }

    public String getId() {
        return id;
    }

    public void unHighLight() {
        layout.removeStyleName("highLight");
    }

    public void highLight() {
        layout.addStyleName("highLight");
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void select() {
        layout.addStyleName("selected");
        isSelected = true;
    }

    public void unSelect() {
        layout.removeStyleName("selected");
        isSelected = false;
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
        return addDomHandler(handler, MouseOverEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
        return addDomHandler(handler, MouseOutEvent.getType());
    }

    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
        return addDomHandler(handler, DoubleClickEvent.getType());
    }
}
