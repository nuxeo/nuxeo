/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stéphane Fourrier
 */

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
