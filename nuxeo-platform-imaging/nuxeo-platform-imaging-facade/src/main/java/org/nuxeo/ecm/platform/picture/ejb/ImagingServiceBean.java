/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.ejb;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Session bean wrapper for the local ImagingService
 * <p>
 * Enable EJB remoting for the local nuxeo-runtime service
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Stateless
@Remote(ImagingService.class)
@Local(ImagingService.class)
public class ImagingServiceBean implements ImagingService {

    private final ImagingService service;

    public ImagingServiceBean() {
        try {
            service = Framework.getLocalService(ImagingService.class);
        } catch (Exception e) {
            throw new Error("Failed to lookup ImagingService service", e);
        }
    }

    @Deprecated
    public Map<String, Object> getImageMetadata(InputStream in) {
        return service.getImageMetadata(in);
    }

    @Deprecated
    public Map<String, Object> getImageMetadata(File file) {
        return service.getImageMetadata(file);
    }

    public Map<String, Object> getImageMetadata(Blob blob) {
        return service.getImageMetadata(blob);
    }

    public InputStream resize(InputStream in, int width, int height) {
        return service.resize(in, width, height);
    }

    public InputStream rotate(InputStream in, int angle) {
        return service.rotate(in, angle);
    }

    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        return service.crop(in, x, y, width, height);
    }
    public String getImageMimeType(File file){
        return service.getImageMimeType(file);
    }

    public String getImageMimeType(InputStream in) {
        return service.getImageMimeType(in);
    }

}
