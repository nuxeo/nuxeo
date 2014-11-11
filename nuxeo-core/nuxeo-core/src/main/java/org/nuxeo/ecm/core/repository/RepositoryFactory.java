/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.repository;

import java.util.concurrent.Callable;

/**
 * Low-level Repository factory.
 * <p>
 * The repository factory to use is usually specified in the MBean configuration file.
 */
public interface RepositoryFactory extends Callable<Object> {

    void init(String repositoryName);

    /**
     * Constructs the low-level repository with the name previously passed to {@link #init}.
     *
     * @return a low-level Repository
     */
    @Override
    public Object call();

}
