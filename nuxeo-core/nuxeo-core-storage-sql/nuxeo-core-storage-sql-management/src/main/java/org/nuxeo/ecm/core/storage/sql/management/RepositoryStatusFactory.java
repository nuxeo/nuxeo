/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;

/**
 * @author Florent Guillaume
 */
public class RepositoryStatusFactory extends AbstractResourceFactory {

    @Override
    public void registerResources() {
        RepositoryStatus instance = new RepositoryStatus();
        service.registerResource("SQLRepositoryStatus",
                ObjectNameFactory.formatQualifiedName("SQLStorage"),
                RepositoryStatusMBean.class, instance);
    }

}
