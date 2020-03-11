/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import org.nuxeo.runtime.model.persistence.fs.FileSystemStorage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractContribution implements Contribution {

    protected String id;

    protected boolean loaded;

    protected final String name;

    protected String description;

    protected boolean disabled;

    protected AbstractContribution(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        if (id == null) {
            id = ContributionPersistenceComponent.getComponentName(getName());
        }
        return id;
    }

    protected void load() {
        if (!loaded) {
            FileSystemStorage.loadMetadata(this);
            loaded = true;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        load();
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isDisabled() {
        load();
        return disabled;
    }

    @Override
    public void setDisabled(boolean isDisabled) {
        this.disabled = isDisabled;
    }

}
