/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class ComponentWithXPoint extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("BaseXPoint");

    public static final String XP = "xp";

    private static final Log log = LogFactory.getLog(ComponentWithXPoint.class);

    final List<DummyContribution> contribs = new ArrayList<>();

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            log.debug("Registering: " + ((DummyContribution) contrib).message);
            this.contribs.add((DummyContribution) contrib);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            log.debug("Un-Registering: " + ((DummyContribution) contrib).message);
            this.contribs.remove(contrib);
        }
    }

    public DummyContribution[] getContributions() {
        return contribs.toArray(new DummyContribution[contribs.size()]);
    }

    public Registry getComputedRegistry() {
        return getExtensionPointRegistry(XP);
    }

}
