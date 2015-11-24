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

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.opensocial.container.client.event.priv.app.HideMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.HideMessageEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEventHandler;

import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;

/**
 * @author Stéphane Fourrier
 */
public class MessagePresenter extends WidgetPresenter<MessagePresenter.Display> {

    public interface Display extends WidgetDisplay {
        HasText getMessageBox();

        void showMessage();

        void hideMessage();

        void setPriorityColor(String color);
    }

    public static final Place PLACE = new Place("Message");

    public static final float TIMEOUT = 2;

    @Inject
    public MessagePresenter(final Display display, EventBus eventBus) {
        super(display, eventBus);
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        registerMessageSending();
        registerMessageHide();
    }

    @Override
    protected void onUnbind() {
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
    }

    private void registerMessageSending() {
        registerHandler(eventBus.addHandler(SendMessageEvent.TYPE,
                new SendMessageEventHandler() {
                    public void onMessageSent(SendMessageEvent event) {
                        float timeout = event.hasToBeKeptVisible() ? 0
                                : TIMEOUT;
                        showMessage(event.getMessage(),
                                event.getSeverity().getAssociatedClassName(),
                                timeout);
                    }
                }));
    }

    private void registerMessageHide() {
        registerHandler(eventBus.addHandler(HideMessageEvent.TYPE,
                new HideMessageEventHandler() {
                    public void onMessageHidden(HideMessageEvent event) {
                        hideMessage();
                    }
                }));
    }

    public static native void showMessage(String message, String className,
            float timeout) /*-{
                           $wnd.jQuery.ambiance({
                           title: message,
                           className: className,
                           timeout: timeout
                           });
                           }-*/;

    public static native void hideMessage() /*-{
                                            $wnd.jQuery(".ambiance").remove();
                                            }-*/;
}
