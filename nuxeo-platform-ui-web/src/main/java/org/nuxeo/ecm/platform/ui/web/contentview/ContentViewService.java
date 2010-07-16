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
package org.nuxeo.ecm.platform.ui.web.contentview;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PageProvider;

/**
 * Service handling content views and associated page providers.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface ContentViewService extends Serializable {

    /**
     * Returns the content view with given name, or null if not found.
     *
     * @throws ClientException
     */
    ContentView getContentView(String name) throws ClientException;

    /**
     * Returns the page provider computed from the content view with given
     * name. Its properties are resolved using current {@link FacesContext}
     * instance if they are EL Expressions.
     *
     * @throws ClientException
     */
    PageProvider<?> getPageProvider(String contentViewName,
            Object... parameters) throws ClientException;

}
