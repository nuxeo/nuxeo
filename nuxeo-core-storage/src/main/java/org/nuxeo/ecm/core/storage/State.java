/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstraction for a Map<String, Serializable> that is Serializable.
 *
 * @since 5.9.5
 */
public class State extends HashMap<String, Serializable> {

    private static final long serialVersionUID = 1L;

    /** Empty constructor. */
    public State() {
        super();
    }

    /** Copy constructor. */
    public State(State state) {
        super(state);
    }

    private State(Map<String, Serializable> map) {
        super(map);
    }

    public static State singleton(String key, Serializable value) {
        return new State(Collections.singletonMap(key, value));
    }
}
