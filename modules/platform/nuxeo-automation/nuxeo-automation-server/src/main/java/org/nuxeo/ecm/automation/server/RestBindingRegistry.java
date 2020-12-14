/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.server;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedMember;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.RegistryContribution;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.w3c.dom.Element;

/**
 * Registry to handle specific id on chains as well as disablement.
 *
 * @since 11.5
 */
public class RestBindingRegistry extends MapRegistry {

    @Override
    protected void register(RegistryContribution rc) {
        Context ctx = rc.getContext();
        XAnnotatedObject xObject = rc.getObject();
        Element element = rc.getElement();

        // generate the new instance in all cases, to check if this is a chain and if it is disabled
        RestBinding ob = (RestBinding) xObject.newInstance(ctx, element);
        boolean isChain = ob.chain;
        boolean isDisabled = ob.isDisabled;
        String name = ob.name;
        // adjust id
        String id = isChain ? Constants.CHAIN_ID_PREFIX + name : name;
        XAnnotatedMember remove = xObject.getRemove();
        if (remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element))) {
            contributions.remove(id);
            return;
        }
        Object contrib;
        XAnnotatedMember merge = xObject.getMerge();
        if (merge != null && Boolean.TRUE.equals(merge.getValue(ctx, element))) {
            contrib = xObject.newInstance(ctx, element, contributions.get(id));
        } else {
            contrib = xObject.newInstance(ctx, element);
        }
        contributions.put(id, contrib);
        // handle deprecated disablement
        if (isDisabled) {
            String message = String.format(
                    "Usage of \"disabled\" attribute on RestBinding contribution '%s',"
                            + " in extension '%s', is deprecated: use \"enable\" attribute instead",
                    id, StringUtils.substringBeforeLast(rc.getTag(), "."));
            DeprecationLogger.log(message, "11.5");
            disabled.add(id);
        }
        XAnnotatedMember enable = xObject.getEnable();
        if (enable != null && enable.hasValue(ctx, element)) {
            // avoid enabling an existing disabled contribution if not explicitely specified
            Object enabled = enable.getValue(ctx, element);
            if (enabled != null && Boolean.FALSE.equals(enabled)) {
                disabled.add(id);
            } else {
                disabled.remove(id);
            }
        }
    }

}
