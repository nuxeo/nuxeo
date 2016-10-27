/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import org.nuxeo.ecm.core.event.EventBundle;

import java.util.List;
import java.util.Map;

/**
 * Provides partial default implementation for a {@link PipeConsumer}
 *
 * @since 8.4
 */
public abstract class AbstractPipeConsumer<T> implements PipeConsumer<T> {

    protected String name;

    protected Map<String, String> params;

    @Override
    public void initConsumer(String name, Map<String, String> params) {
        this.name = name;
        this.params = params;

    }

    protected String getName() {
        return name;
    }

    protected Map<String, String> getParameters() {
        return params;
    }

    @Override
    public boolean receiveMessage(List<T> messages) {
        List<EventBundle> bundles = unmarshallEventBundle(messages);
        return processEventBundles(bundles);
    }

    protected abstract List<EventBundle> unmarshallEventBundle(List<T> messages);

    protected abstract boolean processEventBundles(List<EventBundle> bundles);

}
