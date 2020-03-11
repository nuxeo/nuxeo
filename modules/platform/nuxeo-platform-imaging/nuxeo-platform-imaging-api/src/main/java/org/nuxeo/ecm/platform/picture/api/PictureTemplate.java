/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.api;

import static org.apache.commons.logging.LogFactory.getLog;

import org.apache.commons.logging.Log;

/**
 * Object to store the definition of a picture template, to be used when computing views for a given image.
 *
 * @deprecated since 7.1. Use {@link org.nuxeo.ecm.platform.picture.api.PictureConversion}.
 */
@Deprecated
public class PictureTemplate extends PictureConversion {

    private static final Log log = getLog(PictureTemplate.class);

    public PictureTemplate(String title, String description, String tag, Integer maxSize) {
        super(title, description, tag, maxSize);
        log.warn("PictureTemplate is deprecated since 7.1, please use PictureConversion instead");
    }
}
