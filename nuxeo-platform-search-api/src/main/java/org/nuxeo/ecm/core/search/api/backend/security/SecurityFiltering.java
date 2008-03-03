/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.backend.security;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Shared info about security filtering.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public final class SecurityFiltering {

    /**
     * This is the list of all permissions that grant access to some
     * indexed document. This is used in place of groups of permissions
     * resolution that aren't accessible in async mode for now.
     */
    // TODO This should at least become more flexible.
    public static final List<String> GRANT = Arrays.asList(
            SecurityConstants.BROWSE,
            SecurityConstants.EVERYTHING,
            SecurityConstants.READ,
            SecurityConstants.READ_WRITE
            );

    public static final String SEPARATOR = "#";

    public static final String ESCAPE = "[#]";

    // Constant utility class.
    private SecurityFiltering() {
    }

}
