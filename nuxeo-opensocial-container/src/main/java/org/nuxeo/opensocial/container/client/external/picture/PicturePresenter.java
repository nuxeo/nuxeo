package org.nuxeo.opensocial.container.client.external.picture;

import org.nuxeo.opensocial.container.client.event.publ.UpdateWebContentEvent;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Image;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

/**
 * @author Stéphane Fourrier
 */
public class PicturePresenter extends WidgetPresenter<PicturePresenter.Display> {

    public interface Display extends WidgetDisplay {
        Image getPicture();

        HasClickHandlers getModifyButton();

        HasClickHandlers getSaveButton();

        HasClickHandlers getCancelButton();

        HasText getUrlTextBox();

        HasText getTitleTextBox();

        HasText getPictureTitle();

        void enableModifPanel();

        void switchToModifyPanel();

        void switchToMainPanel();

        void enableFacets();
    }

    public static final Place PLACE = null;

    private PictureModel model;

    public PicturePresenter(Display display, EventBus eventBus,
            PictureModel model) {
        super(display, eventBus);

        this.model = model;

        fetchContent();
    }

    private void fetchContent() {
        display.getPicture().setUrl(model.getData().getUrl());
        display.getPictureTitle().setText(model.getData().getPictureTitle());

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
            registerModifyEvent();
            registerSaveButtonEvent();
            registerCancelButtonEvent();
        }
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

    private void registerModifyEvent() {
        registerHandler(display.getModifyButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.getTitleTextBox().setText(
                                model.getData().getPictureTitle());
                        display.getUrlTextBox().setText(
                                model.getData().getUrl());

                        display.switchToModifyPanel();
                    }
                }));
    }

    private void registerSaveButtonEvent() {
        registerHandler(display.getSaveButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        model.getData().setUrl(
                                display.getUrlTextBox().getText());
                        model.getData().setPictureTitle(
                                display.getTitleTextBox().getText());

                        eventBus.fireEvent(new UpdateWebContentEvent(
                                model.getData().getId()));

                        display.getPicture().setUrl(
                                display.getUrlTextBox().getText());
                        display.getPictureTitle().setText(
                                model.getData().getPictureTitle());

                        display.switchToMainPanel();
                    }
                }));
    }

    private void registerCancelButtonEvent() {
        registerHandler(display.getCancelButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.switchToMainPanel();
                    }
                }));
    }
}
