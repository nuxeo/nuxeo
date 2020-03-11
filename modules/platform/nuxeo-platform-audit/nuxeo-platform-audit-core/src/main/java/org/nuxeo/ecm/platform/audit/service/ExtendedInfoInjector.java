/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */

package org.nuxeo.ecm.platform.audit.service;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public class ExtendedInfoInjector {

    protected final ExpressionEvaluator evaluator;

    public ExtendedInfoInjector(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public void injectExtendedInfo(Map<String, ExtendedInfo> info, ExtendedInfoDescriptor descriptor,
            ExtendedInfoContext context) {
        Serializable value = evaluator.evaluateExpression(context, descriptor.getExpression(), Serializable.class);
        if (value == null) {
            return;
        }
        String key = descriptor.getKey();
        info.put(key, ExtendedInfoImpl.createExtendedInfo(value));
    }

}
