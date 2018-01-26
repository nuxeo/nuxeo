/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *
 */
package org.nuxeo.ecm.core.uidgen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.core.uidgen.AbstractUIDSequencer;

public class DummyUIDSequencerImpl extends AbstractUIDSequencer {

    protected ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<String, AtomicInteger>();

    protected int delta = 1;

    @Override
    public void init() {
    }

    @Override
    public int getNext(String key) {
        counters.putIfAbsent(key, new AtomicInteger());
        return counters.get(key).addAndGet(delta);
    }

    @Override
    public void dispose() {
    }

}
