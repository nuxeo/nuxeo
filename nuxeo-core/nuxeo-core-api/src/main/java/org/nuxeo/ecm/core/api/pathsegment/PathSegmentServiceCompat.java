/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Service generating a path segment from the title by simplifying it to lowercase and dash-separated words.
 */
public class PathSegmentServiceCompat implements PathSegmentService {

    protected int maxSize = Integer.parseInt(Framework.getProperty(NUXEO_MAX_SEGMENT_SIZE_PROPERTY, "24"));

    @Override
    public String generatePathSegment(DocumentModel doc) {
        return generatePathSegment(doc.getTitle());
    }

    @Override
    public String generatePathSegment(String s) {
        return IdUtils.generateId(s, "-", true, getMaxSize());
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }
}
