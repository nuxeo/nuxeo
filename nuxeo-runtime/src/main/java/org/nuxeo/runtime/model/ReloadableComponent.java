/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
