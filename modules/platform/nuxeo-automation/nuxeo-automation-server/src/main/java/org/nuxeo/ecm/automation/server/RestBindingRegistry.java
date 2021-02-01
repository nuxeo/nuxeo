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

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
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
    protected String computeId(Context ctx, XAnnotatedObject xObject, Element element) {
        // generate the new instance in all cases, to check if this is a chain
        RestBinding ob = (RestBinding) xObject.newInstance(ctx, element);
        String name = ob.name;
        // adjust id
        return ob.chain ? Constants.CHAIN_ID_PREFIX + name : name;
    }

    @Override
    protected Boolean shouldEnable(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        // handle deprecated disablement
        if (element.hasAttribute("disabled")) {
            String message = String.format(
                    "Usage of the \"disabled\" attribute on RestBinding contribution '%s',"
                            + " in extension '%s', is deprecated: use the \"enable\" attribute instead.",
                    computeId(ctx, xObject, element), extensionId);
            DeprecationLogger.log(message, "11.5");
            RestBinding ob = (RestBinding) xObject.newInstance(ctx, element);
            return !ob.isDisabled;
        }
        return super.shouldEnable(ctx, xObject, element, extensionId);
    }

}
