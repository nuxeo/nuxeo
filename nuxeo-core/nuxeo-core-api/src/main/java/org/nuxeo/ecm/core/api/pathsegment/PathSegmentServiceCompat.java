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

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Service generating a path segment from the title by simplifying it to lowercase and dash-separated words.
 */
public class PathSegmentServiceCompat implements PathSegmentService {

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
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return cs.getInteger(PathSegmentService.NUXEO_MAX_SEGMENT_SIZE_PROPERTY, 24);
    }

}
