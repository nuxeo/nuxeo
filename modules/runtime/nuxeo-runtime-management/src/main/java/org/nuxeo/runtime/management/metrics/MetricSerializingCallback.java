/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
