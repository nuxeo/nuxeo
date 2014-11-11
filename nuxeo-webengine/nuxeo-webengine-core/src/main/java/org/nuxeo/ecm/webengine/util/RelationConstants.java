/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.util;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

public class RelationConstants {

    public static final String GRAPH_NAME = "default";

    public static final String METADATA_NAMESPACE = "http://www.nuxeo.org/metadata/";

    public static final String DOCUMENT_NAMESPACE = "http://www.nuxeo.org/document/uid/";

    public static final Resource TITLE = new ResourceImpl(METADATA_NAMESPACE + "title");

    public static final Resource UUID = new ResourceImpl(METADATA_NAMESPACE + "uuid");

    // Constant utility class
    private RelationConstants() {
    }

}
