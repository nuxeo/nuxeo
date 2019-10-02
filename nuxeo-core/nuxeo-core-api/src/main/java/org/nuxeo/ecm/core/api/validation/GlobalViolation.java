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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.api.validation;

import java.util.Locale;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * Global validation violation simply handling a message key.
 *
 * @since 11.1
 */
public class GlobalViolation implements ValidationViolation {

    private static final long serialVersionUID = 1L;

    protected String messageKey;

    public GlobalViolation(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * @return The message if it's found in message bundles, {@link #getMessageKey()} otherwise.
     */
    @Override
    public String getMessage(Locale locale) {
        Locale computedLocale = locale != null ? locale : Constraint.MESSAGES_DEFAULT_LANG;
        return I18NUtils.getMessageString(Constraint.MESSAGES_BUNDLE, getMessageKey(), null, computedLocale);
    }

}
