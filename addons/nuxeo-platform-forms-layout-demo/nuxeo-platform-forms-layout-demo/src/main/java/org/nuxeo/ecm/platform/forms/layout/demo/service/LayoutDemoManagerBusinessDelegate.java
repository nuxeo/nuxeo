/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.demo.service;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
@Name("layoutDemoManager")
@Scope(SESSION)
public class LayoutDemoManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -4778456059717447736L;

    protected LayoutDemoManager layoutDemoManager;

    @Unwrap
    public LayoutDemoManager getLayoutDemoManager() throws ClientException {
        if (layoutDemoManager == null) {
            try {
                layoutDemoManager = Framework.getService(LayoutDemoManager.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to LayoutDemoManager. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (layoutDemoManager == null) {
                throw new ClientException("LayoutDemoManager service not bound");
            }

        }
        return layoutDemoManager;
    }

    @Destroy
    public void destroy() {
        if (layoutDemoManager != null) {
            layoutDemoManager = null;
        }
    }

}
