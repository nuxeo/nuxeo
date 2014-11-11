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
 *     rdarlea
 */
package org.nuxeo.ecm.webengine.webcomments.utils;

/**
 * Utility class used for registering constants.
 */
public final class WebCommentsConstants {

    private WebCommentsConstants() {
    }

    /**
     * Nuxeo document type names
     */
    public static final String WORKSPACE = "Workspace";


    /**
     * Constants used for Comments
     */
    /**
     * Constants used for Comments
     */
    public static final String PERMISSION_COMMENT = "Comment";

    public static final String PERMISSION_MODERATE = "Moderate";

    public static final String PERMISSION_WRITE = "Write";

    public static final String PERMISSION_MANAGE_EVERYTHING = "Everything";
    
    public static final String MODERATION_APRIORI = "apriori";
    
    public static final String MODERATION_APOSTERIORI = "aposteriori";
}
