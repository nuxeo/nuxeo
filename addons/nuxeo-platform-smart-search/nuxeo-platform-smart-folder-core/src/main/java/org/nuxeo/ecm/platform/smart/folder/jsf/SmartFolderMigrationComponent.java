/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.folder.jsf;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component to trigger {@link SmartFolderMigrationHandler} at activation.
 *
 * @since 8.1
 */
public class SmartFolderMigrationComponent extends DefaultComponent {

    protected final SmartFolderMigrationHandler migrationHandler = new SmartFolderMigrationHandler();

    @Override
    public void activate(ComponentContext context) {
        migrationHandler.install();
    }

    @Override
    public void deactivate(ComponentContext context) {
        migrationHandler.uninstall();
    }

}
