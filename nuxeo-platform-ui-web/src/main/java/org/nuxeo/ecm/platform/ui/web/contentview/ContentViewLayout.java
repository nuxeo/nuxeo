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

/**
 * Content view layout definition
 *
 * @author Anahide Tchertchian
 */
public interface ContentViewLayout extends Serializable {

    /**
     * Returns the name of the layout
     */
    String getName();

    /**
     * Returns a title for this content view layout
     */
    String getTitle();

    /**
     * Returns a boolean stating if title has to be translated
     */
    boolean getTranslateTitle();

    /**
     * Returns the icon relative path for this content view layout
     */
    String getIconPath();

}
