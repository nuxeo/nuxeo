/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

/**
 * A factory for adapters. Adapters can be used to adapt client and session objects.
 * For example you can contribute an adapter on the session to have an API suited
 * for your needs.
 * <p>
 * To register adapters use {@link AutomationClient#registerAdapter(AdapterFactory)}.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AdapterFactory<T> {

    /**
     * The class to adapt.
     * @return
     */
    Class<?> getAcceptType();

    /**
     * The adapter class.
     * @return
     */
    Class<T> getAdapterType();

    /**
     * Adapt the given object and return the adapter instance.
     * @param toAdapt
     * @return
     */
    T getAdapter(Object toAdapt);

}
