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

package org.nuxeo.ecm.core.api.security.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;

/**
 * An ACL implementation.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ACLImpl extends ArrayList<ACE> implements ACL {

    private static final long serialVersionUID = 5332101749929771434L;

    private final String name;
    private final boolean isReadOnly;


    public ACLImpl(String name, boolean isReadOnly) {
        this.name = name;
        this.isReadOnly = isReadOnly;
    }

    public ACLImpl() {
        this(LOCAL_ACL, false);
    }

    public ACLImpl(String name) {
        this(name, false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ACE[] getACEs() {
        return toArray(new ACE[size()]);
    }

    @Override
    public void setACEs(ACE[] aces) {
        clear();
        addAll(Arrays.asList(aces));
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public Object clone() {
        ACLImpl copy = new ACLImpl(name, isReadOnly);
        ACE[] aces = new ACE[size()];
        for (int i=0; i<size(); i++) {
            aces[i] = (ACE) get(i).clone();
        }
        copy.setACEs(aces);
        return copy;
    }

}
