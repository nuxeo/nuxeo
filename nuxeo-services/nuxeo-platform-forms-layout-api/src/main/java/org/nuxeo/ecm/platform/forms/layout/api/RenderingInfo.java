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
package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;

/**
 * @since 5.5
 */
public interface RenderingInfo extends Serializable {

    public static enum LEVEL {
        error, warn, info
    }

    String getLevel();

    String getMessage();

    boolean isTranslated();

    /**
     * Returns a clone instance of this widget definition.
     * <p>
     * Useful for conversion of widget definition during export.
     */
    RenderingInfo clone();

}
