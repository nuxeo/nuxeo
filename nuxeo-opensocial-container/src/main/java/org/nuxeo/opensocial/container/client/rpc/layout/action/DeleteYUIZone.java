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

package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.DeleteYUIZoneResult;

/**
 * @author Stéphane Fourrier
 */
public class DeleteYUIZone extends AbstractAction<DeleteYUIZoneResult> {

    private static final long serialVersionUID = 1L;

    private int zoneIndex;

    @SuppressWarnings("unused")
    private DeleteYUIZone() {
        super();
    }

    public DeleteYUIZone(ContainerContext containerContext, final int zoneIndex) {
        super(containerContext);
        this.zoneIndex = zoneIndex;
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

}
