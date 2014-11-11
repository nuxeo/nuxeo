/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.event.test.virusscan.service;

import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ScanComponent extends DefaultComponent {

    protected ScanService scanService;

    @Override
    public <T> T getAdapter(Class<T> adapter) {

        if (true) {
            return adapter.cast(getScanService());
        }

        if (adapter.getName().equals(ScanService.class.getName())) {
            return adapter.cast(getScanService());
        }
        return super.getAdapter(adapter);
    }

    /**
     * build the scanService singleton instance.
     *
     * @return
     */
    protected ScanService getScanService() {
        if (scanService == null) {
            scanService = new DummyVirusScanner();
        }
        return scanService;
    }
}
