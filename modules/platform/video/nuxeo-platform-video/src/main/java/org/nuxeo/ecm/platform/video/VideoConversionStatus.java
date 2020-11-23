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

    public final long positionInQueue;

    public final long queueSize;

    public VideoConversionStatus(String message) {
        this.message = message;
        positionInQueue = 0;
        queueSize = 0;
    }

    public VideoConversionStatus(String message, long positionInQueue, long queueSize) {
        this.message = message;
        this.positionInQueue = positionInQueue;
        this.queueSize = queueSize;
    }

    public String getMessage() {
        return message;
    }

    public long getPositionInQueue() {
        return positionInQueue;
    }

    public long getQueueSize() {
        return queueSize;
    }

}
