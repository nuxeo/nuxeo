/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * Methods specific to a proxy.
 */
public interface DocumentProxy extends Document {

    /**
     * Gets the document (version or live document) to which this proxy points.
     */
    Document getTargetDocument();

    /**
     * Sets the source document (not the version) to which this proxy points.
     */
    void setTargetDocument(Document target) throws DocumentException;

}
