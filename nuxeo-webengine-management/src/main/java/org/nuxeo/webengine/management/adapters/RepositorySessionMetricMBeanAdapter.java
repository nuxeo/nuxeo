package org.nuxeo.webengine.management.adapters;

/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *    Stephane Lacoin (Nuxeo EP Software Engineer)
 */

import org.nuxeo.ecm.core.model.Repository;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class RepositorySessionMetricMBeanAdapter implements RepositorySessionMetricMBean {

    public RepositorySessionMetricMBeanAdapter(Repository repository) {
        this.repository = repository;
    }

    protected final Repository repository;
    

    public Integer getActiveSessionsCount() {
        return repository.getActiveSessionsCount();
    }

    public Integer getClosedSessionsCount() {
        return repository.getClosedSessionsCount();
    }

    public Integer getStartedSessionsCount() {
        return repository.getStartedSessionsCount();
    }

}
