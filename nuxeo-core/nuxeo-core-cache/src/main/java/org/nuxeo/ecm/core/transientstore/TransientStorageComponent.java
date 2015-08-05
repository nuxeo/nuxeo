/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component exposing the {@link TransientStoreService} and managing the unerlying extension point
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class TransientStorageComponent extends DefaultComponent implements TransientStoreService {

    protected Map<String, TransientStoreConfig> configs = new HashMap<String, TransientStoreConfig>();

    @Override
    public TransientStore getStore(String name) {
        return configs.get(name).store;
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                NuxeoException errors = new NuxeoException("Shutdown transient stores");
                for (TransientStoreConfig each : configs.values()) {
                    try {
                        each.shutdown();
                    } catch (RuntimeException cause) {
                        errors.addSuppressed(cause);
                    }
                }
                if (errors.getSuppressed().length > 0) {
                    throw errors;
                }
            }
        });
        NuxeoException errors = new NuxeoException("Initializing transient stores");
        for (TransientStoreConfig each : configs.values()) {
            try {
                each.init();
            } catch (RuntimeException cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    @Override
    public void doGC() {
        for (TransientStoreConfig each : configs.values()) {
            each.store.doGC();
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        TransientStoreConfig config = (TransientStoreConfig) contribution;
        // XXX merge
        configs.put(config.name, config);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        TransientStoreConfig config = (TransientStoreConfig) contribution;
        configs.remove(config.name);
    }

}
