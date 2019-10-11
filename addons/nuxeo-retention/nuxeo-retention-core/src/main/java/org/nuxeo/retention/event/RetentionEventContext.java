/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.event;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.retention.RetentionConstants;

/**
 * Specialized event context for retention events.
 *
 * @since 11.1
 */
public class RetentionEventContext extends UnboundEventContext {

    private static final long serialVersionUID = 1L;

    public RetentionEventContext(NuxeoPrincipal principal) {
        super(principal, null);
        setProperty("category", RetentionConstants.EVENT_CATEGORY);
    }

    public String getInput() {
        Serializable input = getProperty(RetentionConstants.INPUT_PROPERTY_KEY);
        if (input instanceof String) {
            return (String) input;
        }
        return null;
    }

    public void setInput(String input) {
        setProperty(RetentionConstants.INPUT_PROPERTY_KEY, input);
    }

}
