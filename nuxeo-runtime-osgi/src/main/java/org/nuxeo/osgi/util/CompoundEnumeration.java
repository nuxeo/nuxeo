/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.osgi.util;

import java.util.Enumeration;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompoundEnumeration<E> implements Enumeration<E> {

    protected final Enumeration<E>[] enums;
    protected int index = 0;

    @SuppressWarnings("unchecked")
    public CompoundEnumeration(Enumeration<?>[] enums) {
        this.enums = (Enumeration<E>[])enums;
    }

    protected boolean next() {
        while (index < enums.length) {
            if (enums[index] != null && enums[index].hasMoreElements()) {
                return true;
            }
            index++;
        }
        return false;
    }

    @Override
    public boolean hasMoreElements() {
        return next();
    }

    @Override
    public E nextElement() {
        return enums[index].nextElement();
    }

}
