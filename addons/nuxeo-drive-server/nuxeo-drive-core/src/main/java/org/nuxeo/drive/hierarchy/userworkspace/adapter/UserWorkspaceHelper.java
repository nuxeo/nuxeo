/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.hierarchy.userworkspace.adapter;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author Antoine Taillefer
 */
public final class UserWorkspaceHelper {

    private static final String USER_WORKSPACE_ROOT = "UserWorkspaces";

    private UserWorkspaceHelper() {
        // Helper class
    }

    public static boolean isUserWorkspace(DocumentModel doc) {
        // TODO: find a better way than checking the path?
        Path path = doc.getPath();
        int pathLength = path.segmentCount();
        return pathLength > 1 && USER_WORKSPACE_ROOT.equals(path.segment(pathLength - 2));
    }

}
