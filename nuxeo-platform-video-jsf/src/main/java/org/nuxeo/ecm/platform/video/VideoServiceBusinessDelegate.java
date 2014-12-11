/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@Name("videoService")
@Scope(SESSION)
public class VideoServiceBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoServiceBusinessDelegate.class);

    protected VideoService videoService;

    /**
     * Acquires a new {@link VideoService} reference. The related service may be deployed on a local or remote
     * AppServer.
     */
    @Unwrap
    public VideoService getService() throws ClientException {
        if (videoService == null) {
            try {
                videoService = Framework.getService(VideoService.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to VideoService. " + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (videoService == null) {
                throw new ClientException("VideoService service not bound");
            }
        }
        return videoService;
    }

    @Destroy
    public void destroy() {
        if (videoService != null) {
            videoService = null;
        }
        log.debug("Destroyed the seam component");
    }

}
