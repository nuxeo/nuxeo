package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.presenter.MessagePresenter;

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class MessageWidget extends Composite implements
        MessagePresenter.Display {

    private final AbsolutePanel panel;

    private Label message;

    private AbsolutePanel innerPanel;

    public MessageWidget() {
        panel = new AbsolutePanel();
        panel.setStyleName("messageWidget");
        panel.getElement().getStyle().setPosition(Position.FIXED);
        panel.setWidth(Window.getClientWidth() + "px");

        innerPanel = new AbsolutePanel();
        innerPanel.setStyleName("messenger");
        panel.add(innerPanel);

        message = new Label("");
        innerPanel.add(message);

        initWidget(panel);
    }

    public HasText getMessageBox() {
        return (HasText) message;
    }

    public void showMessage() {
        panel.getElement().getStyle().setTop(0, Unit.PX);
        panel.setVisible(true);
    }

    public void hideMessage() {
        panel.setVisible(false);
    }

    public void setPriorityColor(String color) {
        innerPanel.getElement().getStyle().setBackgroundColor(color);
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
