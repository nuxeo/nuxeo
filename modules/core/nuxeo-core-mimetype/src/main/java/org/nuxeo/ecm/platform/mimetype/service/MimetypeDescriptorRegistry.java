/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.mimetype.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.w3c.dom.Element;

/**
 * Registry used to stack {@link MimetypeDescriptor} contributions.
 *
 * @since 11.5
 */
public class MimetypeDescriptorRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(MimetypeDescriptorRegistry.class);

    protected Map<String, MimetypeEntry> mimetypeByNormalizedRegistry = new ConcurrentHashMap<>();

    protected Map<String, MimetypeEntry> mimetypeByExtensionRegistry = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        mimetypeByNormalizedRegistry.clear();
        mimetypeByExtensionRegistry.clear();
        super.initialize();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        MimetypeDescriptor desc = super.doRegister(ctx, xObject, element, extensionId);
        if (desc != null) {
            MimetypeEntry mimetype = desc.getMimetype();
            log.debug("Registering mimetype: {}", mimetype.getNormalized());
            mimetypeByNormalizedRegistry.put(mimetype.getNormalized(), mimetype);
            for (String extension : mimetype.getExtensions()) {
                mimetypeByExtensionRegistry.put(extension, mimetype);
            }
        }
        return (T) desc;
    }

    public boolean isNormalized(String mimeType) {
        return mimeType != null && mimetypeByNormalizedRegistry.containsKey(mimeType);
    }

    public MimetypeEntry getEntryByName(String name) {
        if (name == null) {
            return null;
        }
        return mimetypeByNormalizedRegistry.get(name);
    }

    public MimetypeEntry getEntryByExtension(String extension) {
        if (extension == null) {
            return null;
        }
        return mimetypeByExtensionRegistry.get(extension);
    }

    public Set<Map.Entry<String, MimetypeEntry>> getEntryKeys() {
        return mimetypeByNormalizedRegistry.entrySet();
    }

}
