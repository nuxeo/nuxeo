/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.PropertyContainer;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * Base interface for SQL documents.
 */
public interface SQLDocument extends Document, PropertyContainer, Property {

    final String BINARY_TEXT_SYS_PROP = "binaryText";
    final String FULLTEXT_JOBID_SYS_PROP = "fulltextJobId";

    /**
     * Returns the node with info about the hierarchy location.
     */
    Node getNode();

    /**
     * Returns the property holding the ACL.
     */
    org.nuxeo.ecm.core.model.Property getACLProperty() throws DocumentException;

    /**
     * Raises an exception if the document is read-only.
     */
    void checkWritable() throws DocumentException;

}
