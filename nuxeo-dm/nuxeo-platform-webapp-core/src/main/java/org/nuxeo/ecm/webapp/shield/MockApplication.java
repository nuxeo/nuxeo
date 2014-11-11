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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.shield;

import java.util.ResourceBundle;

import javax.el.ELContextListener;
import javax.faces.context.FacesContext;

/**
 * Override of the default mock application to allow lookup of EL context and
 * benefit from translation and escaping features on the error page.
 *
 * @since 5.9.3
 */
public class MockApplication extends org.jboss.seam.mock.MockApplication {

    @Override
    public ELContextListener[] getELContextListeners() {
        return new ELContextListener[0];
    }

    @Override
    public ResourceBundle getResourceBundle(FacesContext context, String string) {
        return null;
    }

}
