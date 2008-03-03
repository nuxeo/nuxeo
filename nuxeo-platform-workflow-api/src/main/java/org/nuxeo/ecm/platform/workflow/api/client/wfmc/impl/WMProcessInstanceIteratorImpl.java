/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: WMProcessInstanceIteratorImpl.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstanceIterator;

/**
 * Process instance iterator implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WMProcessInstanceIteratorImpl implements WMProcessInstanceIterator {

    private static final long serialVersionUID = 1L;

    // :FIXME: iterator is fake for now. Do the implementation
    private final List<WMProcessInstance> l = new ArrayList<WMProcessInstance>();

    private transient Iterator<WMProcessInstance> it;

    public WMProcessInstanceIteratorImpl() {
        super();
        it = null;
    }

    public WMProcessInstanceIteratorImpl(List<WMProcessInstance> procs) {
        super();
        if (procs != null) {
            l.addAll(procs);
        }
    }

    public boolean hasNext() {
        if (getIterator() != null) {
            return getIterator().hasNext();
        }
        return false;
    }

    public WMProcessInstance next() {
        if (getIterator() != null) {
            return getIterator().next();
        }
        return null;
    }

    public void remove() {
        if (getIterator() != null) {
            getIterator().remove();
        }
    }

    public int size() {
        return l.size();
    }

    private Iterator<WMProcessInstance> getIterator() {
        if (it == null) {
            it = l.iterator();
        }
        return it;
    }

}
