/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
