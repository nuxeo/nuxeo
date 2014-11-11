/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class CompositeValueAdapter implements ValueAdapter {

    protected List<ValueAdapter> adapters = new ArrayList<ValueAdapter>();

    public List<ValueAdapter> getAdapters() {
        return adapters;
    }

    public void addAdapter(ValueAdapter adapter) {
        adapters.add(adapter);
    }

    public <T> T getValue(Shell shell, Class<T> type, String value) {
        for (ValueAdapter adapter : adapters) {
            T result = adapter.getValue(shell, type, value);
            if (result != null) {
                return result;
            }
        }
        throw new ShellException("Unknown type adapter for: " + type);
    }

}
