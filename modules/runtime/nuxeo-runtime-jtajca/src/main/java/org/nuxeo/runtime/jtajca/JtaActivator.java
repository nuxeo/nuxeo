/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *   Stephane Lacoin
 */
package org.nuxeo.runtime.jtajca;

import javax.naming.NamingException;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * If this bundle is present in the running platform it should automatically install the NuxeoContainer.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JtaActivator extends DefaultComponent {

    public static final String AUTO_ACTIVATION = "null";

    @Override
    public void activate(ComponentContext context) {
        try {
            NuxeoContainer.install();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            NuxeoContainer.uninstall();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

}
