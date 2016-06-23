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
package org.nuxeo.ecm.core.event.pipe;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.EventBundle;

/**
 * @since TODO
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
