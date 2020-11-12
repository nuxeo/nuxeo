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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import java.util.HashMap;

import javax.mail.Message;

/**
 * The execution context of an actions pipe.
 *
 * @author Alexandre Russel
 */
public class ExecutionContext extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected ExecutionContext initialContext;

    public ExecutionContext() {
    }

    public ExecutionContext(Message message) {
        put("message", message);
    }

    public ExecutionContext(Message message, ExecutionContext initialContext) {
        this(message);
        this.initialContext = initialContext;
    }

    public Message getMessage() {
        return (Message) get("message");
    }

    public ExecutionContext getInitialContext() {
        return initialContext;
    }

}
