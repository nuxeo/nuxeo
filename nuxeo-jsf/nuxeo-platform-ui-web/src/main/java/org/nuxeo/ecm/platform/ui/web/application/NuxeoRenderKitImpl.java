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

import javax.faces.render.ResponseStateManager;

import com.sun.faces.renderkit.RenderKitImpl;

/**
 * @since 6.0
 */
public class NuxeoRenderKitImpl extends RenderKitImpl {

    private ResponseStateManager responseStateManager =
            new NuxeoResponseStateManagerImpl();

    @Override
    public synchronized ResponseStateManager getResponseStateManager() {
        if (responseStateManager == null) {
            responseStateManager = new NuxeoResponseStateManagerImpl();
        }
        return responseStateManager;
    }

}
