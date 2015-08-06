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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentWithXPoint extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("BaseXPoint");

    final Map<String,DummyContribution> contribs = new HashMap<>();

    static ComponentWithXPoint instance;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        instance = this;
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            super.deactivate(context);
        } finally {
            instance = null;
        }
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            DummyContribution dummy = (DummyContribution) contrib;
            this.contribs.put(dummy.message, dummy);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            DummyContribution dummy = (DummyContribution) contrib;
            this.contribs.remove(dummy.message);
        }
    }

    public DummyContribution[] getContributions() {
        return contribs.values().toArray(new DummyContribution[contribs.size()]);
    }

}
