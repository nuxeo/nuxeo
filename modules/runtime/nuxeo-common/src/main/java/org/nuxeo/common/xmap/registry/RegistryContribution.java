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
package org.nuxeo.common.xmap.registry;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * Represents contribution items needed to perform registration on a {@link Registry}.
 *
 * @since 11.5
 */
public class RegistryContribution {

    protected final Context context;

    protected final XAnnotatedObject object;

    protected final Element element;

    protected final String marker;

    public RegistryContribution(Context context, XAnnotatedObject object, Element element, String marker) {
        this.context = context;
        this.object = object;
        this.element = element;
        this.marker = marker;
    }

    public Context getContext() {
        return context;
    }

    public XAnnotatedObject getObject() {
        return object;
    }

    public Element getElement() {
        return element;
    }

    public String getMarker() {
        return marker;
    }

}
