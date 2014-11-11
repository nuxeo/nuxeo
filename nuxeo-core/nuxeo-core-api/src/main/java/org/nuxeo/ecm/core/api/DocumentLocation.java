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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DocumentLocation.java 25074 2007-09-18 14:23:08Z atchertchian $
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;

/**
 * Document server name with its unique identifier within this server.
 */
public interface DocumentLocation extends Serializable {

    /**
     * Returns the document server name.
     */
    String getServerName();

    /**
     * Returns the document reference.
     */
    DocumentRef getDocRef();

}
