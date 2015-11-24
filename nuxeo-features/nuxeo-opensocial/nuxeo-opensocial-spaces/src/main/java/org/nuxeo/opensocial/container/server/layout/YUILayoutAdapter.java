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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.container.server.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;

/**
 * @author Stéphane Fourrier
 */
public interface YUILayoutAdapter {
    YUILayout getLayout() throws ClientException;

    void setBodySize(YUIBodySize size) throws ClientException;

    YUIUnit setSideBar(YUISideBarStyle sideBar) throws ClientException;

    YUIUnit setHeader(YUIUnit hasHeader) throws ClientException;

    YUIUnit setFooter(YUIUnit hasFooter) throws ClientException;

    YUIComponentZone createZone(YUIComponentZone zone, int zoneIndex)
            throws ClientException;

    void deleteZone(int zoneIndex) throws ClientException;

    YUIComponentZone updateZone(YUIComponentZone zone, int zoneIndex,
            YUITemplate template) throws ClientException;

    void initLayout(YUILayout layout) throws ClientException;

    void save() throws ClientException;
}
