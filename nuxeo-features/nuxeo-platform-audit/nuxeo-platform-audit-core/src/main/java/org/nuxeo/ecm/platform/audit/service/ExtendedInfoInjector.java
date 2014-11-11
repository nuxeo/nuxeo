/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */

package org.nuxeo.ecm.platform.audit.service;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public class ExtendedInfoInjector  {

    protected final ExpressionEvaluator evaluator;

    public ExtendedInfoInjector(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public void injectExtendedInfo(Map<String, ExtendedInfo> info,
            ExtendedInfoDescriptor descriptor,
            ExtendedInfoContext context) {
        Serializable value = evaluator.evaluateExpression(
                context, descriptor.getExpression(), Serializable.class);
        if (value == null) {
            return;
        }
        String key = descriptor.getKey();
        info.put(key, ExtendedInfoImpl.createExtendedInfo(value));
    }

}
