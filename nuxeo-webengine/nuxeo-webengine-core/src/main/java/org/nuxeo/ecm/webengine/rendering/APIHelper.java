/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rendering;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class APIHelper implements RenderingExtensionFactory {

    public static final APIHelper INSTANCE = new APIHelper();

    public static final Comparator<DocumentType> DOCTYPE_COMPARATOR = new Comparator<DocumentType>() {
        public int compare(DocumentType o1, DocumentType o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    public Object createTemplate() {
        return this;
    }

    public static DocumentType[] getSortedDocumentTypes() {
        DocumentType[] doctypes = Framework.getService(SchemaManager.class).getDocumentTypes();
        Arrays.sort(doctypes, DOCTYPE_COMPARATOR);
        return doctypes;
    }

    public static Bundle[] getBundles() {
        return Framework.getRuntime().getContext().getBundle().getBundleContext().getBundles();
    }

    public static Collection<RegistrationInfo> getComponents() {
        return Framework.getRuntime().getComponentManager().getRegistrations();
    }

    public static Collection<ComponentName> getPendingComponents() {
        return Framework.getRuntime().getComponentManager().getPendingRegistrations().keySet();
    }

}
