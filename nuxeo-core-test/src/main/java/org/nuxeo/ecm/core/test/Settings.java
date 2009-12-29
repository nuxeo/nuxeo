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
import org.nuxeo.ecm.core.test.annotations.CleanupLevel;
import org.nuxeo.ecm.core.test.annotations.Repository;
import org.nuxeo.ecm.core.test.annotations.RepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Session;
import org.nuxeo.runtime.test.runner.Bundles;

@Session
@Repository
// these annotations are present just to provide a simple way to lookup default
// values
public class Settings {

    private final Description description;

    public Settings(Description description) {
        this.description = description;
    }

    public RepoType getRepoType() {
        Repository repo = description.getAnnotation(Repository.class);
        if (repo == null) {
            return this.getClass().getAnnotation(Repository.class).value();
        }
        return repo.value();
    }

    public String getRepoUsername() {
        Session sessionFactory = description.getAnnotation(Session.class);
        if (sessionFactory == null) {
            return this.getClass().getAnnotation(Session.class).user();
        }
        return sessionFactory.user();
    }

    public String[] getBundles() {
        Bundles annotation = description.getAnnotation(Bundles.class);
        if (annotation != null) {
            return annotation.value();
        } else {
            return new String[0];
        }
    }

    public RepoFactory getRepoFactory() {

        RepositoryFactory annotation = description.getAnnotation(RepositoryFactory.class);
        if (annotation != null) {
            try {
                RepoFactory instance = annotation.value().newInstance();
                return instance;
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    public Level getCleanUpLevel() {
        CleanupLevel annotation = description.getAnnotation(CleanupLevel.class);
        if (annotation != null) {
            return annotation.value();
        } else {
            return Level.CLASS;
        }
    }

}
