/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mem;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Memory Repository Descriptor.
 *
 * @since 8.1
 */
@XObject(value = "repository")
public class MemRepositoryDescriptor {

    public MemRepositoryDescriptor() {
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    @XNode("@name")
    public String name;

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
    }

    @XNode("fulltext@disabled")
    private Boolean fulltextDisabled;

    public boolean getFulltextDisabled() {
        return defaultFalse(fulltextDisabled);
    }

    /** Copy constructor. */
    public MemRepositoryDescriptor(MemRepositoryDescriptor other) {
        name = other.name;
        label = other.label;
        isDefault = other.isDefault;
        fulltextDisabled = other.fulltextDisabled;
    }

    public void merge(MemRepositoryDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.label != null) {
            label = other.label;
        }
        if (other.isDefault != null) {
            isDefault = other.isDefault;
        }
        if (other.fulltextDisabled != null) {
            fulltextDisabled = other.fulltextDisabled;
        }
    }

}
