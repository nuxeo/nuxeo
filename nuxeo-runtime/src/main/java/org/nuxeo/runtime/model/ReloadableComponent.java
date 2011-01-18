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
package org.nuxeo.runtime.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A component that expose a reload method usefull to completely reload the component and preserving
 * already registered extensions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ReloadableComponent extends DefaultComponent implements Reloadable {

    protected List<Extension> extensions = new ArrayList<Extension>();

    @Override
    public void registerExtension(Extension extension) throws Exception {
        super.registerExtension(extension);
        extensions.add(extension);
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        extensions.remove(extension);
        super.unregisterExtension(extension);
    }

    public void reload(ComponentContext context) throws Exception {
        deactivate(context);
        activate(context);
        for (Extension xt : extensions) {
            super.registerExtension(xt);
        }
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

}
