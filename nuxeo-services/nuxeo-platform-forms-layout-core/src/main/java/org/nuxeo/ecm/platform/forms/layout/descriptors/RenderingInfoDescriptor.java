/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
