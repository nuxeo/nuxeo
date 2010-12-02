/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import org.nuxeo.runtime.model.persistence.fs.FileSystemStorage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
