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
 *
 * $Id$
 */

package org.nuxeo.runtime.service.sample;

import org.nuxeo.runtime.service.Adapter;
import org.nuxeo.runtime.service.AnnotatedAdapterFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Adapter(type=Service1.class, interfaces={Service2.class})
public class Service2Adapter extends AnnotatedAdapterFactory<Service1> {

    @Override
    public <T> T getAdapter(Service1 instance, Class<T> adapter) {
        return adapter.cast(new Service2Impl(instance));
    }

}
