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

package org.nuxeo.opensocial.container.client;

/**
 * @author Stéphane Fourrier
 */
public class ContainerBuilderConfiguration {
    private ContainerBuilderConfiguration() {
    }

    public static native boolean isContainerSizeConfigurable() /*-{
        try { return $wnd.nuxeo.openSocial.container.builder.width; } catch(e) { return false; }
    }-*/;

    public static native boolean isSideBarConfigurable() /*-{
        try { return $wnd.nuxeo.openSocial.container.builder.sidebar; } catch(e) { return false; }
    }-*/;

    public static native boolean isFooterConfigurable() /*-{
        try { return $wnd.nuxeo.openSocial.container.builder.footer; } catch(e) { return false; }
    }-*/;

    public static native boolean isHeaderConfigurable() /*-{
        try { return $wnd.nuxeo.openSocial.container.builder.header; } catch(e) { return false; }
    }-*/;
}
