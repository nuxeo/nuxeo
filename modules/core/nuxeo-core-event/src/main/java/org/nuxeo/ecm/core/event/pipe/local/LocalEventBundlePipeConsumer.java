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
package org.nuxeo.ecm.core.event.pipe.local;

import java.util.List;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.AbstractListenerPipeConsumer;

/**
 * In memory implementation that does not handle any marshaling and directly feeds the {@link WorkManager}
 *
 * @since 8.4
 */
@Experimental
public class LocalEventBundlePipeConsumer extends AbstractListenerPipeConsumer<EventBundle> {

    @Override
    protected List<EventBundle> unmarshallEventBundle(List<EventBundle> messages) {
        // xxx reconnect them all using the same session ?
        // we may not want to reconnect at this stage and let the workers do it
        return messages;
    }

}
