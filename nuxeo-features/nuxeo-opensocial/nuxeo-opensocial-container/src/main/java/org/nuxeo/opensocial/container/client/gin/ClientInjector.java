package org.nuxeo.opensocial.container.client.gin;

import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.presenter.MessagePresenter;
import org.nuxeo.opensocial.container.client.utils.WebContentFactory;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

import net.customware.gwt.dispatch.client.gin.ClientDispatchModule;
import net.customware.gwt.presenter.client.EventBus;

/**
 * @author Stéphane Fourrier
 */
@GinModules({ ClientDispatchModule.class, ClientModule.class })
public interface ClientInjector extends Ginjector {
    AppPresenter getAppPresenter();

    EventBus getEventBus();

    AppModel getModel();

    MessagePresenter getMessagePresenter();

    WebContentFactory getGadgetFactory();
}
