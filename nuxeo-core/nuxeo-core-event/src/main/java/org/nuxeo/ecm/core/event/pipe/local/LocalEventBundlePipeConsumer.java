/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe.local;

import java.util.List;

import javax.resource.spi.work.WorkManager;

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
