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

package org.nuxeo.opensocial.container.client;

import org.nuxeo.opensocial.container.client.gin.ClientInjector;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.presenter.MessagePresenter;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Stéphane Fourrier Entry point classes define
 */
public class Container implements EntryPoint {

    private static final String GWT_CONTAINER_ID = "gwtContainerDiv";

    public static ClientInjector injector = GWT.create(ClientInjector.class);

    public static AbsolutePanel rootPanel = RootPanel.get(GWT_CONTAINER_ID);

    public void onModuleLoad() {
        MessagePresenter messagePresenter = injector.getMessagePresenter();
        messagePresenter.bind();
        messagePresenter.revealDisplay();

        AppPresenter appPresenter = injector.getAppPresenter();
        appPresenter.bind();
        appPresenter.revealDisplay();

        rootPanel.add(appPresenter.getDisplay().asWidget());
        RootPanel.get().add(messagePresenter.getDisplay().asWidget());
    }
}
