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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Object representing a registered video conversion on the {@link VideoService} .
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("videoConversion")
@XRegistry(enable = false)
public class VideoConversion {

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode("@converter")
    private String converter;

    @XNode("@height")
    private long height;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    private boolean enabled;

    /**
     * @since 7.2
     */
    @XNode(value = "@rendition", defaultAssignment = "true")
    private boolean rendition;

    /**
     * @since 7.2
     */
    @XNode(value = "@renditionVisible", defaultAssignment = "true")
    private boolean renditionVisible;

    public String getName() {
        return name;
    }

    public String getConverter() {
        return converter;
    }

    public long getHeight() {
        return height;
    }

    public boolean isRenditionVisible() {
        return renditionVisible;
    }

    public boolean isRendition() {
        return rendition;
    }

}
