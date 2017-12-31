/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.repository;

import java.util.concurrent.Callable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A high-level repository descriptor, from which you get a {@link CoreSession} when calling {@link #open}.
 * <p>
 * This is obsolete as an extension point, use org.nuxeo.ecm.core.storage.sql.RepositoryService instead. Descriptor kept
 * for backward-compatibility.
 * <p>
 * Note that this is still use as an object returned by the core api RepositoryManager.
 */
@XObject("repository")
public class Repository {

    @XNode("@name")
    private String name;

    @XNode("@label")
    private String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    /**
     * Factory to used to create the low-level repository.
     */
    private Callable<Object> repositoryFactory;

    public Repository() {
    }

    public Repository(String name, String label, Boolean isDefault, Callable<Object> repositoryFactory) {
        this.name = name;
        this.label = label;
        this.isDefault = isDefault;
        this.repositoryFactory = repositoryFactory;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public boolean isDefault() {
        return Boolean.TRUE.equals(isDefault);
    }

    public Callable<Object> getRepositoryFactory() {
        return repositoryFactory;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name=" + name + ", label=" + label + '}';
    }

}
