/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;

/**
 * Property diff display interface.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface PropertyDiffDisplay extends Serializable {

    static final String DEFAULT_STYLE_CLASS = "noBackgroundColor";

    static final String RED_BACKGROUND_STYLE_CLASS = "redBackgroundColor";

    static final String GREEN_BACKGROUND_STYLE_CLASS = "greenBackgroundColor";

    Serializable getValue();

    String getStyleClass();
}
