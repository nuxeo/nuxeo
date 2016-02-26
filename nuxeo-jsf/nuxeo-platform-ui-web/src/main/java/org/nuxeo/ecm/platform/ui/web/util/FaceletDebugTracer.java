/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.util;

import javax.faces.view.facelets.Tag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 8.2
 */
public class FaceletDebugTracer {

    private static final Log log = LogFactory.getLog(FaceletDebugTracer.class);

    public static final String TRACE_PROP = "nuxeo.jsf.debug.log_min_duration_ms";

    public static long start() {
        if (getMaxTraceLag() >= 0) {
            return System.currentTimeMillis();
        }
        return 0;
    }

    public static void trace(long start, Tag tag, String id) {
        if (start > 0) {
            trace(start, tag, id, getMaxTraceLag());
        }
    }

    public static void traceMillis(long start, Tag tag, String id) {
        trace(start, tag, id, 0);
    }

    public static void trace(long start, Tag tag, String id, long maxLag) {
        if (start > 0 && maxLag >= 0) {
            long end = System.currentTimeMillis();
            long lag = end - start;
            if (lag >= maxLag) {
                log.info(String.format("'%s' at '%s' took: %s ms.", id, tag, lag));
            }
        }
    }

    protected static long getMaxTraceLag() {
        if (log.isInfoEnabled()) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            return Long.valueOf(cs.getProperty(TRACE_PROP, "-1"));
        }
        return -1;
    }

}
