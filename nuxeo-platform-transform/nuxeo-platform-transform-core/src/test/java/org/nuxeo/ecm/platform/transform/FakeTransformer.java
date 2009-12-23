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
 *     Nuxeo - initial API and implementation
 *
 * $Id: FakeTransformer.java 21875 2007-07-03 16:52:17Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform;

import java.util.List;

import org.nuxeo.ecm.platform.transform.transformer.AbstractTransformer;


/*
 * Foo transformer.
 *
 * @author janguenot
 */
public class FakeTransformer extends AbstractTransformer {

    private static final long serialVersionUID = 1L;

    public FakeTransformer() {
    }

    public FakeTransformer(String name, List<String> pluginChains) {
        super(name, pluginChains);
    }

    public Object transform(Object source, Object options) {
        // Auto-generated method stub
        return null;
    }
}
