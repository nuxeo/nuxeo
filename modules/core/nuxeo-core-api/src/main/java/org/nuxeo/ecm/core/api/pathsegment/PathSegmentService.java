/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service with a method generating a path segment (name) given a {@link DocumentModel} about to be created. Usually the
 * title is used to derive the path segment.
 */
public interface PathSegmentService {

    /**
     * @since 7.4
     */
    String NUXEO_MAX_SEGMENT_SIZE_PROPERTY = "nuxeo.path.segment.maxsize";

    /**
     * Generate the path segment to use for a {@link DocumentModel} that's about to be created.
     *
     * @param doc the document
     * @return the path segment, which must not contain any {@code /} character
     */
    String generatePathSegment(DocumentModel doc);

    /**
     * Generate the path segment to use from a string.
     *
     * @param s the string
     * @return the path segment, which must not contain any {@code /} character
     * @since 5.9.2
     */
    String generatePathSegment(String s);

    /**
     * Return the path segment max size
     *
     * @since 7.4
     */
    int getMaxSize();
}
