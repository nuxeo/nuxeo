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

package org.nuxeo.opensocial.container.client.presenter;

import org.nuxeo.opensocial.container.client.ui.api.HasId;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.Presenter;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

/**
 * @author Stéphane Fourrier
 */
public class CustomWebContentPresenter extends
        WidgetPresenter<CustomWebContentPresenter.Display> {
    public interface Display extends WidgetDisplay, HasId {
        String getParentId();

        void addContent(Widget widget);

        void clean();
    }

    private WebContentData webContent;

    private Presenter contentPresenter;

    @Inject
    public CustomWebContentPresenter(final Display display,
            final EventBus eventBus, WebContentData data,
            Presenter contentPresenter) {
        super(display, eventBus);

        this.webContent = data;
        this.contentPresenter = contentPresenter;

        fetchLayoutContent();
    }

    private void fetchLayoutContent() {
        display.addContent(((WidgetDisplay) contentPresenter.getDisplay()).asWidget());
        display.setId(webContent.getId());
    }

    @Override
    public Place getPlace() {
        return null;
    }

    @Override
    protected void onBind() {
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
        display.clean();
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
    }
}
