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

package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentMovedEvent extends GwtEvent<WebContentMovedEventHandler> {
    public static Type<WebContentMovedEventHandler> TYPE = new Type<WebContentMovedEventHandler>();

    private final String fromUnitName;

    private final int fromWebContentPosition;

    private final String toUnitName;

    private final int toWebContentPosition;

    public WebContentMovedEvent(String fromUnitName,
            int fromWebContentPosition, String toUnitName,
            int toWebContentPosition) {
        this.fromUnitName = fromUnitName;
        this.fromWebContentPosition = fromWebContentPosition;
        this.toUnitName = toUnitName;
        this.toWebContentPosition = toWebContentPosition;
    }

    public String getFromUnitName() {
        return fromUnitName;
    }

    public int getFromWebContentPosition() {
        return fromWebContentPosition;
    }

    public String getToUnitName() {
        return toUnitName;
    }

    public int getToWebContentPosition() {
        return toWebContentPosition;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<WebContentMovedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentMovedEventHandler handler) {
        handler.onWebContentHasMoved(this);
    }
}
