/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.storage.binary.Binary;

/**
 * Helpers for deep copy and copy-on-write.
 */
public class CopyHelper {

    private CopyHelper() {
        // utility class
    }

    /**
     * Makes a deep copy of a {@link Map}.
     */
    public static State deepCopy(State state) {
        state = new State(state);
        for (Entry<String, Serializable> en : state.entrySet()) {
            en.setValue(deepCopy(en.getValue()));
        }
        return state;
    }

    /**
     * Makes a deep copy of a {@link List}.
     */
    public static List<Serializable> deepCopy(List<Serializable> list) {
        list = new ArrayList<Serializable>(list);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, deepCopy(list.get(i)));
        }
        return list;
    }

    /**
     * Makes a deep copy of a value.
     */
    public static Serializable deepCopy(Serializable value) {
        if (value instanceof State) {
            State state = (State) value;
            value = deepCopy(state);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) value;
            value = (Serializable) deepCopy(list);
        } else if (value instanceof Object[]) {
            // array values are supposed to be scalars
            value = ((Object[]) value).clone();
        }
        // else scalar value
        // check anyway (debug)
        else if (!(value == null //
                || value instanceof String //
                || value instanceof Boolean //
                || value instanceof Long //
                || value instanceof Double //
                || value instanceof Calendar //
        || value instanceof Binary)) {
            throw new UnsupportedOperationException("Cannot deep copy: "
                    + value.getClass().getName());
        }
        return value;
    }

}
