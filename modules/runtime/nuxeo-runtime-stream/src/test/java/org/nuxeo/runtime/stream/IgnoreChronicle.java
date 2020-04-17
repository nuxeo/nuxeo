/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.stream;

import static org.nuxeo.runtime.stream.RuntimeStreamFeature.STREAM_CHRONICLE;
import static org.nuxeo.runtime.stream.RuntimeStreamFeature.STREAM_KAFKA;
import static org.nuxeo.runtime.stream.RuntimeStreamFeature.STREAM_PROPERTY;

import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;

public class IgnoreChronicle implements ConditionalIgnoreRule.Condition {

    @Override
    public boolean shouldIgnore() {
        if (STREAM_CHRONICLE.equals(System.getProperty(STREAM_PROPERTY))) {
            return true;
        }
        // not kafka also means chronicle
        if (!((STREAM_KAFKA.equals(System.getProperty(STREAM_PROPERTY))
                || "true".equals(System.getProperty("kafka"))))) {
            return true;
        }
        return false;
    }
}
