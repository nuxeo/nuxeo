package org.nuxeo.opensocial.container.client.utils;

import java.util.Map;

import org.nuxeo.opensocial.container.client.external.html.HTMLGadget;
import org.nuxeo.opensocial.container.client.external.html.HTMLModel;
import org.nuxeo.opensocial.container.client.external.html.HTMLPresenter;
import org.nuxeo.opensocial.container.client.external.opensocial.OpenSocialGadget;
import org.nuxeo.opensocial.container.client.external.opensocial.OpenSocialModel;
import org.nuxeo.opensocial.container.client.external.opensocial.OpenSocialPresenter;
import org.nuxeo.opensocial.container.client.external.picture.PictureGadget;
import org.nuxeo.opensocial.container.client.external.picture.PictureModel;
import org.nuxeo.opensocial.container.client.external.picture.PicturePresenter;
import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.inject.Inject;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.Presenter;

/**
 * @author Stéphane Fourrier
 */
public class WebContentFactory {
    private EventBus eventBus;

    private AppModel model;

    @Inject
    public WebContentFactory(EventBus eventBus, AppModel model) {
        this.eventBus = eventBus;
        this.model = model;
    }

    public WebContentData getDataFor(String gadgetType) {
        if (PictureData.TYPE.equals(gadgetType))
            return new PictureData();
        if (HTMLData.TYPE.equals(gadgetType))
            return new HTMLData();
        if (OpenSocialData.TYPE.equals(gadgetType))
            return new OpenSocialData();
        return null;
    }

    public Presenter getPresenterFor(WebContentData webContentData) {
        Map<String, Boolean> permissions = model.getPermissions().get(
                webContentData.getId());

        if (PictureData.TYPE.equals(webContentData.getAssociatedType())) {
            return new PicturePresenter(new PictureGadget(), eventBus,
                    new PictureModel((PictureData) webContentData, permissions));
        }
        if (HTMLData.TYPE.equals(webContentData.getAssociatedType())) {
            return new HTMLPresenter(new HTMLGadget(), eventBus, new HTMLModel(
                    (HTMLData) webContentData, permissions));
        }
        if (OpenSocialData.TYPE.equals(webContentData.getAssociatedType())) {
            return new OpenSocialPresenter(new OpenSocialGadget(), eventBus,
                    new OpenSocialModel((OpenSocialData) webContentData,
                            permissions));
        }
        return null;
    }

}
