package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.presenter.AppPresenter;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import net.customware.gwt.presenter.client.widget.WidgetDisplay;

/**
 * @author Stéphane Fourrier
 */
public class AppWidget extends Composite implements AppPresenter.Display {
    private final AbsolutePanel panel;

    public AppWidget() {
        panel = new AbsolutePanel();
        panel.setWidth("100%");
        initWidget(panel);
    }

    public Widget asWidget() {
        return this;
    }

    public void addContent(WidgetDisplay display) {
        panel.add(display.asWidget());
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
