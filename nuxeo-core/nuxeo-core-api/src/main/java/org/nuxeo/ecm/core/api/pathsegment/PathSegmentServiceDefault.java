/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.regex.Pattern;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Service generating a path segment from the title by just removing slashes and limiting size.
 */
public class PathSegmentServiceDefault implements PathSegmentService {

    public Pattern stupidRegexp = Pattern.compile("^[- .,;?!:/\\\\'\"]*$");

    /**
     * @deprecated since 7.4, use {@link PathSegmentService#NUXEO_MAX_SEGMENT_SIZE_PROPERTY} instead
     */
    @Deprecated
    public static final String NUXEO_MAX_SEGMENT_SIZE_PROPERTY = PathSegmentService.NUXEO_MAX_SEGMENT_SIZE_PROPERTY;

    @Override
    public String generatePathSegment(DocumentModel doc) {
        return generatePathSegment(doc.getTitle());
    }

    @Override
    public String generatePathSegment(String s) {
        if (s == null) {
            s = "";
        }
        s = s.trim();
        if (s.length() > getMaxSize()) {
            s = s.substring(0, getMaxSize()).trim();
        }
        s = s.replace('/', '-');
        s = s.replace('\\', '-');
        if (stupidRegexp.matcher(s).matches()) {
            return IdUtils.generateStringId();
        }
        return s;
    }

    @Override
    public int getMaxSize() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return Integer.parseInt(cs.getProperty(PathSegmentService.NUXEO_MAX_SEGMENT_SIZE_PROPERTY, "24"));
    }

}