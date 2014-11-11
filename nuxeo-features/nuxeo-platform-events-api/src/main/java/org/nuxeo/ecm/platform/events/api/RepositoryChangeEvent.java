/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.events.api;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author Max Stepanov
 *
 */
public interface RepositoryChangeEvent extends NXCoreEvent {

    int ADDED = 1;
    int REMOVED = 2;
    int UPDATED = 3;
    int PERMISSIONS = 4;
    int LOCK = 5;
    int LIFECYCLE = 6;
    int MOVED = 7;

    int getType();

    String getOriginSessionId();

    String getRepositoryName();

    DocumentRef getTargetDocumentRef();

    Object getDetails();

}
