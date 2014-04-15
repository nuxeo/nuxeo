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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.favorites.api;

import org.nuxeo.ecm.collections.api.CollectionConstants;

/**
 * @since 5.9.4
 */
public interface FavoritesConstants {

    public static final String DEFAULT_FAVORITES_NAME = "Favorites";
    public static final String FAVORITES_TYPE = "Favorites";
    public static final Object DEFAULT_FAVORITES_TITLE = CollectionConstants.I18N_PREFIX + "label.myFavorites.title";

}
