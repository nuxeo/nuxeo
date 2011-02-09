package org.nuxeo.opensocial.container.client.external.html;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.opensocial.container.client.event.publ.UpdateWebContentEvent;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasText;

/**
 * @author Stéphane Fourrier
 */
public class HTMLPresenter extends WidgetPresenter<HTMLPresenter.Display> {
    public interface Display extends WidgetDisplay {
        String getHtmlFromView();

        void setHtmlView(String html);

        String getHtmlFromEditor();

        void setHtmlEditor(String html);

        HasClickHandlers getSaveButton();

        HasClickHandlers getModifyButton();

        void enableModifPanel();

        HasText getTitleTextBox();

        HasText getHtmlTitle();

        void switchToModifyPanel();

        void switchToMainPanel();

        void enableFacets();
    }

    public static final Place PLACE = null;

    private HTMLModel model;

    public HTMLPresenter(Display display, EventBus eventBus, HTMLModel model) {
        super(display, eventBus);

        this.model = model;
        fetchContent();
    }

    private void fetchContent() {
        display.setHtmlView(model.getData()
                .getHtml());
        display.getHtmlTitle()
                .setText(model.getData()
                        .getHtmlTitle());

        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            display.enableFacets();
            display.enableModifPanel();
        }
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            registerSaveEvent();
            registerModifyEvent();
        }
    }

    private void registerModifyEvent() {
        registerHandler(display.getModifyButton()
                .addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.getTitleTextBox()
                                .setText(model.getData()
                                        .getHtmlTitle());
                        display.setHtmlEditor(model.getData()
                                .getHtml());
                        display.switchToModifyPanel();
                    }
                }));
    }

    private void registerSaveEvent() {
        registerHandler(display.getSaveButton()
                .addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        model.getData()
                                .setHtmlTitle(display.getTitleTextBox()
                                        .getText());

                        model.getData()
                                .setHtml(display.getHtmlFromEditor());

                        eventBus.fireEvent(new UpdateWebContentEvent(
                                model.getData()
                                        .getId()));

                        display.getHtmlTitle()
                                .setText(display.getTitleTextBox()
                                        .getText());

                        display.setHtmlView(model.getData()
                                .getHtml());

                        display.switchToMainPanel();
                    }
                }));
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
    }
}
