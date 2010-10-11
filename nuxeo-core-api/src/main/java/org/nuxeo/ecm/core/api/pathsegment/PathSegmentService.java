/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.api.pathsegment;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service with a method generating a path segment (name) given a
 * {@link DocumentModel} about to be created. Usually the title is used to
 * derive the path segment.
 */
public interface PathSegmentService {

    /**
     * Generate the path segment to use for a {@link DocumentModel} that's about
     * to be created.
     *
     * @param doc the document
     * @return the path segment, which must not contain any {@code /} character
     */
    String generatePathSegment(DocumentModel doc) throws ClientException;

}
