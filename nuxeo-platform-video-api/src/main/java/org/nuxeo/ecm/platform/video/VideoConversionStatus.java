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

/**
 * Simple data transfer object to report on the state of a video conversion.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class VideoConversionStatus {

    public static final String STATUS_CONVERSION_QUEUED = "status.video.conversionQueued";

    public static final String STATUS_CONVERSION_PENDING = "status.video.conversionPending";

    public final String message;

    public final int positionInQueue;

    public final int queueSize;

    public VideoConversionStatus(String message) {
        this.message = message;
        positionInQueue = 0;
        queueSize = 0;
    }

    public VideoConversionStatus(String message, int positionInQueue,
            int queueSize) {
        this.message = message;
        this.positionInQueue = positionInQueue;
        this.queueSize = queueSize;
    }

    public String getMessage() {
        return message;
    }

    public int getPositionInQueue() {
        return positionInQueue;
    }

    public int getQueueSize() {
        return queueSize;
    }

}
