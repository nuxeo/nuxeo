/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.core.blob.apps;

import org.nuxeo.ecm.core.blob.ManagedBlob;

import java.io.IOException;
import java.util.List;

/**
 * Interface for a provider with linked/third-party apps.
 *
 * @since 7.3
 */
public interface LinkedAppsProvider {

    public static final int PREFERRED_ICON_SIZE = 16;

    /**
     * Returns a list of application links for the given blob.
     */
    List<AppLink> getAppLinks(String user, ManagedBlob blob) throws IOException;
}
