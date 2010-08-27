/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Eugen Ionica
 */

package org.nuxeo.ecm.platform.relations.api.util;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

/**
 * Constants for relations management.
 *
 * @author Anahide Tchertchian
 * @author Eugen Ionica
 */
public class RelationConstants {

    public static final String GRAPH_NAME = "default";

    public static final String METADATA_NAMESPACE = "http://www.nuxeo.org/metadata/";

    public static final String DOCUMENT_NAMESPACE = "http://www.nuxeo.org/document/uid/";

    // statement metadata

    public static final Resource TITLE = new ResourceImpl(METADATA_NAMESPACE
            + "title");

    public static final Resource UUID = new ResourceImpl(METADATA_NAMESPACE
            + "uuid");

    public static final Resource CREATION_DATE = new ResourceImpl(
            METADATA_NAMESPACE + "CreationDate");

    public static final Resource MODIFICATION_DATE = new ResourceImpl(
            METADATA_NAMESPACE + "ModificationDate");

    public static final Resource AUTHOR = new ResourceImpl(METADATA_NAMESPACE
            + "Author");

    // XXX AT: for BBB, use a different namespace for comment
    public static final Resource COMMENT = new ResourceImpl(
            "http://www.nuxeo.org/comment");

    public static final Resource COPY_FROM_WORK_VERSION = new ResourceImpl(
            METADATA_NAMESPACE + "copy-from-work-version");

    // Constant utility class
    private RelationConstants() {
    }

}
