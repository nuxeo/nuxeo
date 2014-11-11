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

import org.nuxeo.runtime.service.AdaptableServiceImpl;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Service2Impl extends AdaptableServiceImpl implements Service2 {

    protected final Service1 s1;

    public Service2Impl(Service1 s1) {
        this.s1 = s1;
    }

    @Override
    public void m2() {
        System.out.println("method: Service2Impl::m2()");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return s1.getAdapter(adapter);
    }

}
