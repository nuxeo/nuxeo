/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.services.adapter.extension;

import org.nuxeo.runtime.services.adapter.AdapterFactory;

/**
 * An internal interface that exposes portion of AdapterFactoryProxy functionality
 * without the need to import the class itself.
 */
public interface IAdapterFactoryExt {

    /**
     * Loads the real adapter factory, but only if its associated plug-in is
     * already loaded. Returns the real factory if it was successfully loaded.
     * @param force if <code>true</code> the plugin providing the
     *              factory will be loaded if necessary, otherwise no plugin activations
     *              will occur.
     */
    AdapterFactory loadFactory(boolean force);

    String[] getAdapterNames();
}
