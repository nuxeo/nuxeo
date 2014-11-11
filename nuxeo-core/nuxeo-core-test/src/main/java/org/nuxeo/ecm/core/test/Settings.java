/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import org.junit.runner.Description;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryBackend;
import org.nuxeo.ecm.core.test.annotations.RepositoryCleanup;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryInitializer;
import org.nuxeo.ecm.core.test.annotations.Session;

@Session
@RepositoryBackend
@RepositoryCleanup
// annotations present to provide a simple way to lookup default values
public class Settings extends org.nuxeo.runtime.test.runner.Settings {

    public Settings(Description description) {
        super(description);
    }

    public BackendType getBackendType() {
        RepositoryBackend repo = description.getAnnotation(RepositoryBackend.class);
        if (repo == null) {
            repo = this.getClass().getAnnotation(RepositoryBackend.class);
        }
        return repo.value();
    }

    public String getRepositoryUsername() {
        Session sessionFactory = description.getAnnotation(Session.class);
        if (sessionFactory == null) {
            sessionFactory = this.getClass().getAnnotation(Session.class);
        }
        return sessionFactory.user();
    }

    public RepositoryInit getRepositoryInitializer() {
        RepositoryInitializer annotation = description.getAnnotation(RepositoryInitializer.class);
        if (annotation != null) {
            try {
                RepositoryInit instance = annotation.value().newInstance();
                return instance;
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    public Granularity getRepositoryCleanupGranularity() {
        RepositoryCleanup annotation = description.getAnnotation(RepositoryCleanup.class);
        if (annotation == null) {
            // get annotation with default
            annotation = this.getClass().getAnnotation(RepositoryCleanup.class);
        }
        return annotation.value();
    }

}
