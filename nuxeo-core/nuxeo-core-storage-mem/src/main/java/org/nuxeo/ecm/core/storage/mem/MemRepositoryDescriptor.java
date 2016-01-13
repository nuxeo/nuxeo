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

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

/**
 * Memory Repository Descriptor.
 *
 * @since 8.1
 */
@XObject(value = "repository")
public class MemRepositoryDescriptor extends DBSRepositoryDescriptor {

    public MemRepositoryDescriptor() {
    }

    @Override
    public MemRepositoryDescriptor clone() {
        return (MemRepositoryDescriptor) super.clone();
    }

    public void merge(MemRepositoryDescriptor other) {
        super.merge(other);
    }

}
