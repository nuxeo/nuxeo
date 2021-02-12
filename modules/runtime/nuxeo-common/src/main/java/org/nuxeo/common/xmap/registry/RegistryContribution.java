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

import org.apache.commons.lang3.StringUtils;
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

    protected final String tag;

    public RegistryContribution(Context context, XAnnotatedObject object, Element element, String tag) {
        this.context = context;
        this.object = object;
        this.element = element;
        this.tag = tag;
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

    public String getTag() {
        return tag;
    }

    /**
     * @see #getRuntimeExtensionFromTag(String)
     */
    public String getRuntimeExtensionFromTag() {
        return getRuntimeExtensionFromTag(tag);
    }

    /**
     * Helper method for logging, assuming the tag will match a runtime extension id.
     * <p>
     * The tag should match the pattern "componentName#targetExtensionPoint.randomNumber", in which case
     * "componentName#targetExtensionPoint" will be returned.
     */
    public static String getRuntimeExtensionFromTag(String tag) {
        return StringUtils.substringBeforeLast(tag, ".");
    }

}
