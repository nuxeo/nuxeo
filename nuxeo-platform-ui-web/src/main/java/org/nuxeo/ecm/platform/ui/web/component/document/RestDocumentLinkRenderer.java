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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.document;

import javax.faces.component.UIComponent;

import com.sun.faces.renderkit.html_basic.OutputLinkRenderer;

/**
 * Overrides default output link renderer so that URL parameters passed through
 * f:param tags are not added twice, since the component already takes them
 * into account when building the URL.
 *
 * @see RestDocumentLink
 * @since 5.4.2
 */
public class RestDocumentLinkRenderer extends OutputLinkRenderer {

    /**
     * Returns an empty parameters list because parameters are already taken
     * care of in the computed URL.
     */
    protected Param[] getParamList(UIComponent command) {
        return new Param[0];
    }

}
