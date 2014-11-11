/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.runtime.management.metrics;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.CallbackSkeleton;
import org.javasimon.Counter;
import org.javasimon.Sample;
import org.javasimon.Split;

public class MetricSerializingCallback extends CallbackSkeleton {

    protected static final Log log = LogFactory.getLog(MetricSerializingCallback.class);

    protected final MetricSerializer serializer;

    public MetricSerializingCallback(MetricSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void stopwatchStop(Split split) {
       toStream(split.getStopwatch().sample());
    }

    @Override
    public void counterSet(Counter counter, long val) {
      toStream(counter.sample());
    }

    protected void toStream(Sample sample) {
        try {
            serializer.toStream(sample);
        } catch (IOException e) {
            log.info("not streamed " + sample, e);
        }
    }

}
