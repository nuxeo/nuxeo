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
 * $Id: FakePlugin.java 21875 2007-07-03 16:52:17Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;

/*
 * Foo plugin.
 *
 * @author janguenot
 */
public class FakePlugin extends AbstractPlugin {

    private static final long serialVersionUID = 1L;

    public FakePlugin() {
    }

    public FakePlugin(String name) {
        super(name);
    }

    public FakePlugin(String name, List<String> sourceMimeTypes,
            String destinationMimeType,
            Map<String, Serializable> defaultOptions) {
        super(name, sourceMimeTypes, destinationMimeType, defaultOptions);
    }

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        super.transform(options, sources);
        return null;
    }

}
