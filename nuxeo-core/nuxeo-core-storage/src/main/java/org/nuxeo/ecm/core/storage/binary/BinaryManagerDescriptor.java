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
package org.nuxeo.ecm.core.storage.binary;


/**
 * Descriptor for a Binary Manager.
 */
public class BinaryManagerDescriptor {

    public BinaryManagerDescriptor() {
    }

    public String repositoryName;

    public Class<? extends BinaryManager> klass;

    public String key;

    public String storePath;

    /** Copy constructor. */
    public BinaryManagerDescriptor(BinaryManagerDescriptor other) {
        repositoryName = other.repositoryName;
        klass = other.klass;
        key = other.key;
        storePath = other.storePath;
    }

    public void merge(BinaryManagerDescriptor other) {
        if (other.repositoryName != null) {
            repositoryName = other.repositoryName;
        }
        if (other.klass != null) {
            klass = other.klass;
        }
        if (other.key != null) {
            key = other.key;
        }
        if (other.storePath != null) {
            storePath = other.storePath;
        }
    }

}
