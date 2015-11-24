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

package org.nuxeo.opensocial.container.client.event.priv.app;

import org.nuxeo.opensocial.container.client.utils.Severity;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class SendMessageEvent extends GwtEvent<SendMessageEventHandler> {
    public static Type<SendMessageEventHandler> TYPE = new Type<SendMessageEventHandler>();

    private String message;

    private Severity severity;

    private boolean keepVisible;

    public SendMessageEvent(String message, Severity severity) {
        this.message = message;
        this.severity = severity;
        this.keepVisible = false;
    }

    public SendMessageEvent(String message, Severity severity,
            boolean keepVisible) {
        this.message = message;
        this.severity = severity;
        this.keepVisible = keepVisible;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public boolean hasToBeKeptVisible() {
        return keepVisible;
    }

    @Override
    public Type<SendMessageEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SendMessageEventHandler handler) {
        handler.onMessageSent(this);
    }
}
