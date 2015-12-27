/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.runtime.api;

import java.rmi.dgc.VMID;

/**
 * Provides a way to identify a Nuxeo Runtime instance.
 * <p>
 * Identifier can be:
 * <p>
 * <ul>
 * <li>automatically generated (default) based on a {@link VMID}
 * <li>explicitly set as a system property (org.nuxeo.runtime.instance.id)
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class RuntimeInstanceIdentifier {

    protected static final VMID vmid = new VMID();

    protected static String id;

    public static final String INSTANCE_ID_PROPERTY_NAME = "org.nuxeo.runtime.instance.id";

    private RuntimeInstanceIdentifier() {
    }

    public static String getId() {
        if (id == null) {
            id = Framework.getProperty(INSTANCE_ID_PROPERTY_NAME, getVmid().toString());
        }
        return id;
    }

    public static VMID getVmid() {
        return vmid;
    }

}
