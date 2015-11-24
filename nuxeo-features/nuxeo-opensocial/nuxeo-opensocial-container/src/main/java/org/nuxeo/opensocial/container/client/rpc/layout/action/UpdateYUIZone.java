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
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUIZoneResult;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUIZone extends AbstractAction<UpdateYUIZoneResult> {

    private static final long serialVersionUID = 1L;

    private YUIComponentZone zone;

    private YUITemplate template;

    private int zoneIndex;

    @SuppressWarnings("unused")
    private UpdateYUIZone() {
        super();
    }

    public UpdateYUIZone(ContainerContext containerContext,
            final YUIComponentZone zone, int zoneIndex,
            final YUITemplate template) {
        super(containerContext);
        this.zone = zone;
        this.template = template;
        this.zoneIndex = zoneIndex;
    }

    public YUIComponentZone getZone() {
        return zone;
    }

    public YUITemplate getTemplate() {
        return template;
    }

    public int getZoneIndex() {
        return zoneIndex;
    }

}
