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
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;

/**
 * @since 5.5
 */
public class RenderingInfoImpl implements RenderingInfo {

    private static final long serialVersionUID = 1L;

    protected String level;

    protected String message;

    protected boolean translated = false;

    public RenderingInfoImpl(String level, String message, boolean translated) {
        super();
        this.level = level;
        this.message = message;
        this.translated = translated;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public boolean isTranslated() {
        return translated;
    }

    @Override
    public RenderingInfo clone() {
        return new RenderingInfoImpl(level, message, translated);
    }

}
