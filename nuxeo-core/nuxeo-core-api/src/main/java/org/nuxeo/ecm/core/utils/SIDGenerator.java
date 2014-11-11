/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.utils;

/**
 * Generate session IDs.
 * <p>
 * Session IDs are long values that must be unique on the same JVM.
 * Each call of the {@link SIDGenerator#next()} method returns an
 * unique ID (unique relative to the current running JVM).
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class SIDGenerator {

    private static int count = 0;

    private static final int COUNT_OFFSET = 32;


    private SIDGenerator() {
    }

    /**
     * The long unique id is generated as follow:
     * <p>
     * On the first 32 bits we put an integer value incremented at each call
     * and that is reset to 0 when the it reaches the max integer range.
     * <p>
     * On the last 32 bits the most significant part of the current timestamp
     * in milliseconds.
     *
     * @return the next unique id in this JVM
     */
    public static synchronized long next() {
        if (count == Integer.MAX_VALUE) {
            count = 0;
        }
        long ms = System.currentTimeMillis();
        long id = (int) ms;
        id = Long.rotateLeft(id, COUNT_OFFSET);
        return id + count++;
    }

}
