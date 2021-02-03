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
package org.nuxeo.ecm.platform.mail.action;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.w3c.dom.Element;

/**
 * Custom registry to handle compatibility with deprecated override attribute, as well as on funky merge behaviour.
 *
 * @since 11.5
 */
public class MessageActionPipeRegistry extends MapRegistry {

    @Override
    protected boolean shouldMerge(Context ctx, XAnnotatedObject xObject, Element element, String extensionId, String id,
            Object existing) {
        if (element.hasAttribute("override")) {
            String message = String.format(
                    "Usage of the \"override\" attribute on MessageActionPipeDescriptor contribution '%s',"
                            + " in extension '%s', is deprecated: use the \"merge\" attribute instead.",
                    id, extensionId);
            DeprecationLogger.log(message, "11.5");
            MessageActionPipeDescriptor ob = (MessageActionPipeDescriptor) xObject.newInstance(ctx, element);
            return !ob.override;
        }
        return super.shouldMerge(ctx, xObject, element, extensionId, id, existing);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);
        Object existing = contributions.get(id);
        boolean isNew = existing == null;
        boolean toBeMerged = shouldMerge(ctx, xObject, element, extensionId, id, existing);
        MessageActionPipeDescriptor res = super.doRegister(ctx, xObject, element, extensionId);
        if (isNew || !toBeMerged) {
            res.fillMissingActionDestination();
        }
        return (T) res;
    }

}
