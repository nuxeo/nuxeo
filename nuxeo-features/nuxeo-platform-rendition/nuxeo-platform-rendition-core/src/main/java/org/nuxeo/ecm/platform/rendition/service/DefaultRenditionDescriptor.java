/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for the default rendition of a document.
 *
 * @since 9.3
 */
@XObject("defaultRendition")
public class DefaultRenditionDescriptor {

    public static final String DEFAULT_SCRIPT_LANGUAGE = "JavaScript";

    @XNode("script")
    protected String script;

    /**
     * @since 10.3
     */
    @XNode("@reason")
    protected String reason;

    @XNode("script@language")
    protected String scriptLanguage;

    public String getScript() {
        return script;
    }

    public String getScriptLanguage() {
        return scriptLanguage == null ? DEFAULT_SCRIPT_LANGUAGE : scriptLanguage;
    }

    // empty constructor
    public DefaultRenditionDescriptor() {
    }

    // copy constructor
    public DefaultRenditionDescriptor(DefaultRenditionDescriptor other) {
        script = other.script;
        scriptLanguage = other.scriptLanguage;
        reason = other.reason;
    }

    public void merge(DefaultRenditionDescriptor other) {
        if (other.script != null) {
            script = other.script;
        }
        if (other.scriptLanguage != null) {
            scriptLanguage = other.scriptLanguage;
        }
        if (other.reason != null) {
            reason = other.reason;
        }
    }

}
