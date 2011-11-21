/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     stan
 */

package org.nuxeo.ecm.webapp.webcontainer;

import java.io.Serializable;

import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.Logo;

public interface ThemeActions extends Serializable {

    /**
     * Returns negotiated default theme to handle print action
     */
    String getDefaultTheme();

    /**
     * Returns the logo configured for negotiated flavor
     *
     * @since 5.5.
     */
    Logo getLogo();

    /**
     * Returns the logo configured for given flavor
     *
     * @since 5.5.
     */
    Logo getLogo(String flavorName);

    Flavor getFlavor(String flavorName);

}
