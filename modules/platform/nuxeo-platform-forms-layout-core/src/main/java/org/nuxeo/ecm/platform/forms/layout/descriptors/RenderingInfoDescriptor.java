/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.impl.RenderingInfoImpl;

/**
 * @since 5.5
 */
@XObject("renderingInfo")
public class RenderingInfoDescriptor {

    @XNode("@level")
    String level;

    @XNode("translated")
    boolean translated = false;

    @XNode("message")
    String message;

    public String getLevel() {
        return level;
    }

    public boolean isTranslated() {
        return translated;
    }

    public String getMessage() {
        return message;
    }

    public RenderingInfo getRenderingInfo() {
        return new RenderingInfoImpl(level, getMessage(), isTranslated());
    }

}
