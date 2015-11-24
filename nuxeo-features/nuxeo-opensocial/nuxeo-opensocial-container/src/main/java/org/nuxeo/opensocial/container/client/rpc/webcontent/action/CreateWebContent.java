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

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.CreateWebContentResult;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContent extends AbstractAction<CreateWebContentResult> {
    private static final long serialVersionUID = 1L;

    private WebContentData data;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialization purpose only
    private CreateWebContent() {
        super();
    }

    public CreateWebContent(ContainerContext containerContext,
            final WebContentData data) {
        super(containerContext);
        this.data = data;
    }

    public WebContentData getData() {
        return data;
    }

}
