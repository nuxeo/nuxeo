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

package org.nuxeo.opensocial.container.client.event.publ;

import java.util.List;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContentEvent extends
        GwtEvent<UpdateWebContentEventHandler> {
    public static Type<UpdateWebContentEventHandler> TYPE = new Type<UpdateWebContentEventHandler>();

    private String webContentId;

    private List<String> files;

    public UpdateWebContentEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public UpdateWebContentEvent(String webContentId, List<String> files) {
        this(webContentId);
        this.files = files;
    }

    public String getWebContentId() {
        return webContentId;
    }

    public List<String> getFiles() {
        return files;
    }

    @Override
    protected void dispatch(UpdateWebContentEventHandler handler) {
        handler.onUpdateWebContent(this);
    }

    @Override
    public Type<UpdateWebContentEventHandler> getAssociatedType() {
        return TYPE;
    }
}
