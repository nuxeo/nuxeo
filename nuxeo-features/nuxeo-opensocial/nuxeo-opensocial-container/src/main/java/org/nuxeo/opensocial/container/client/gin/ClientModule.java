package org.nuxeo.opensocial.container.client.gin;

import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.presenter.ContainerBuilderPresenter;
import org.nuxeo.opensocial.container.client.presenter.ContainerPresenter;
import org.nuxeo.opensocial.container.client.presenter.MessagePresenter;
import org.nuxeo.opensocial.container.client.utils.WebContentFactory;
import org.nuxeo.opensocial.container.client.view.AppWidget;
import org.nuxeo.opensocial.container.client.view.ContainerBuilderWidget;
import org.nuxeo.opensocial.container.client.view.ContainerWidget;
import org.nuxeo.opensocial.container.client.view.MessageWidget;

import com.google.inject.Singleton;

import net.customware.gwt.presenter.client.DefaultEventBus;
import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.gin.AbstractPresenterModule;
import net.customware.gwt.presenter.client.place.PlaceManager;

/**
 * @author Stéphane Fourrier
 */
public class ClientModule extends AbstractPresenterModule {
    @Override
    protected void configure() {
        bind(AppModel.class).in(Singleton.class);
        bind(EventBus.class).to(DefaultEventBus.class).in(Singleton.class);
        bind(PlaceManager.class).in(Singleton.class);
        bind(WebContentFactory.class).in(Singleton.class);

        bind(AppPresenter.class).in(Singleton.class);
        bind(AppPresenter.Display.class).to(AppWidget.class).in(Singleton.class);

        bind(MessagePresenter.class).in(Singleton.class);
        bind(MessagePresenter.Display.class).to(MessageWidget.class).in(
                Singleton.class);

        bind(ContainerPresenter.class).in(Singleton.class);
        bind(ContainerPresenter.Display.class).to(ContainerWidget.class).in(
                Singleton.class);

        bind(ContainerBuilderPresenter.class).in(Singleton.class);
        bind(ContainerBuilderPresenter.Display.class).to(
                ContainerBuilderWidget.class).in(Singleton.class);
    }
}
