/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@XObject("imageToConvert")
public class ImageToConvertDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@extension")
    private String extension;

    @XNode("@mimeType")
    private String mimeType;

    /**
     * @return the extension.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return the mimeType.
     */
    public String getMimeType() {
        return mimeType;
    }

}
