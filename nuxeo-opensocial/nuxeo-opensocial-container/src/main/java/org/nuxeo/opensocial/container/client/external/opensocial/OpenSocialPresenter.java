package org.nuxeo.opensocial.container.client.external.opensocial;

import org.nuxeo.opensocial.container.client.ui.api.HasId;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialPresenter extends
        WidgetPresenter<OpenSocialPresenter.Display> {

    public interface Display extends WidgetDisplay, HasId {
        void setUrl(String url);

        void setHeight(int height);

        void setName(String name);

        void enableFacets();
    }

    public static final Place PLACE = null;

    private OpenSocialModel model;

    public OpenSocialPresenter(Display display, EventBus eventBus,
            OpenSocialModel model) {
        super(display, eventBus);

        this.model = model;
        fetchContent();
    }

    private void fetchContent() {
        display.setId("open-social-" + model.getData().getId());
        display.setName("open-social-" + model.getData().getId());
        display.setUrl(model.getData().getFrameUrl());

        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            display.enableFacets();
        }
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
        display.setUrl(model.getData().getFrameUrl());
    }

    public void revealDisplay() {
    }
}
