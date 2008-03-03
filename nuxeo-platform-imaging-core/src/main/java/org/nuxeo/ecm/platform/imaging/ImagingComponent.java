/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.imaging;

import java.io.InputStream;
import java.io.File;
import java.util.Map;

import org.nuxeo.ecm.platform.imaging.api.ImagingService;
import org.nuxeo.ecm.platform.imaging.core.ImageUtils;
import org.nuxeo.ecm.platform.imaging.core.MimeUtils;
import org.nuxeo.ecm.platform.imaging.core.MetadataUtils;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Max Stepanov
 *
 */
public class ImagingComponent extends DefaultComponent implements ImagingService {

    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        return ImageUtils.crop(in, x, y, width, height);
    }

    public InputStream resize(InputStream in, int width, int height) {
        return ImageUtils.resize(in, width, height);
    }

    public InputStream rotate(InputStream in, int angle) {
        return ImageUtils.rotate(in, angle);
    }

    public Map<String, Object> getImageMetadata(InputStream in) {
        return MetadataUtils.getImageMetadata(in);
    }

    public String getImageMimeType(File file) {
        return MimeUtils.getImageMimeType(file);
    }

}
