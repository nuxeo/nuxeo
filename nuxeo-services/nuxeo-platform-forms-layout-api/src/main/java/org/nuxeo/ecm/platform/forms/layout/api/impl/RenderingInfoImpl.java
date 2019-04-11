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

    @Override
    public String getLevel() {
        return level;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isTranslated() {
        return translated;
    }

    @Override
    public RenderingInfo clone() {
        return new RenderingInfoImpl(level, message, translated);
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RenderingInfoImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        RenderingInfoImpl ri = (RenderingInfoImpl) obj;
        return new EqualsBuilder().append(level, ri.level).append(message, ri.message).append(translated, ri.translated).isEquals();
    }

}
