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
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

/**
 * @author bstefanescu
 *
 */
public interface DocumentVersionProxy extends Document {

    /**
     * Gets the version to which this proxy points.
     */
    DocumentVersion getTargetVersion();

    /**
     * Gets the source document (not the version) to which this proxy points.
     */
    Document getTargetDocument() throws DocumentException;

    /**
     * Updates this proxy to point to the base version.
     * <p>
     * If the node is already pointing to the base version does nothing.
     */
    void updateToBaseVersion() throws DocumentException;

}
