/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */

package org.nuxeo.ecm.platform.ui.web.application;

import com.sun.faces.renderkit.RenderKitFactoryImpl;

/**
 * @since 5.9.6
 */
public class NuxeoRenderKitFactoryImpl extends RenderKitFactoryImpl {

    public NuxeoRenderKitFactoryImpl() {
        super();
        // Erase default faces RederKitImpl with Nuxeo custom one to use our own
        // ServerSideStateHelper
        addRenderKit(HTML_BASIC_RENDER_KIT, new NuxeoRenderKitImpl());
    }

}
