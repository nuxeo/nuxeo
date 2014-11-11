/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.annotated;

import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author matic
 *
 */
public class DummyComponent extends DefaultComponent {

    protected final DummyAnnotated da = new DummyImpl() ;
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
       if (adapter.isAssignableFrom(DummyAnnotated.class)) {
           return adapter.cast(da);
       }
        return super.getAdapter(adapter);
    }

}
