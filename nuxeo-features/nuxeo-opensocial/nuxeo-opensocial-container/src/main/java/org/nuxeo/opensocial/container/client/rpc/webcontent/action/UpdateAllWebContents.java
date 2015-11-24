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

package org.nuxeo.opensocial.container.client.rpc.webcontent.action;

import java.util.List;
import java.util.Map;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateAllWebContentsResult;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public class UpdateAllWebContents extends
        AbstractAction<UpdateAllWebContentsResult> {

    private static final long serialVersionUID = 1L;

    private Map<String, List<WebContentData>> webContents;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialisation purpose only
    private UpdateAllWebContents() {
        super();
    }

    public UpdateAllWebContents(ContainerContext containerContext,
            final Map<String, List<WebContentData>> map) {
        super(containerContext);
        this.webContents = map;
    }

    public Map<String, List<WebContentData>> getWebContents() {
        return webContents;
    }

}
