/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.OperationContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Helper {

    private Helper() {
    }

    public static void updateContext(OperationContext ctx, String id, String message, String title) {
        updateTestParam(ctx, "chain", id);
        updateTestParam(ctx, "message", message);
        updateTestParam(ctx, "title", title);
    }

    protected static void updateTestParam(OperationContext ctx, String param, String value) {
        String v = (String) ctx.get(param);
        if (v == null) {
            v = value;
        } else {
            v += "," + value;
        }
        ctx.put(param, v);
    }

}
