/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;

/**
 * Light Event implementation. Used to reduce memory footprint of {@link Event} stacked in {@link EventBundle}.
 *
 * @author Thierry Delprat
 */
public class ShallowEvent extends EventImpl {

    private static final long serialVersionUID = 1L;

    public static ShallowEvent create(Event event) {
        EventContext ctx = event.getContext();
        List<Object> newArgs = new ArrayList<>();
        for (Object arg : ctx.getArguments()) {
            Object newArg = arg;
            if (arg instanceof DocumentModel) {
                DocumentModel oldDoc = (DocumentModel) arg;
                DocumentRef ref = oldDoc.getRef();
                if (ref != null) {
                    newArg = new ShallowDocumentModel(oldDoc);
                } else {
                    newArg = null;
                }
            }
            // XXX treat here other cases !!!!
            newArgs.add(newArg);
        }

        EventContext newCtx = null;
        if (ctx instanceof DocumentEventContext) {
            newCtx = new DocumentEventContext(null, ctx.getPrincipal(), (DocumentModel) newArgs.get(0),
                    (DocumentRef) newArgs.get(1));
        } else {
            newCtx = new EventContextImpl(null, ctx.getPrincipal());
            ((EventContextImpl) newCtx).setArgs(newArgs.toArray());
        }

        newCtx.setRepositoryName(ctx.getRepositoryName());
        Map<String, Serializable> newProps = new HashMap<>();
        for (Entry<String, Serializable> prop : ctx.getProperties().entrySet()) {
            Serializable propValue = prop.getValue();
            if (propValue instanceof DocumentModel) {
                DocumentModel oldDoc = (DocumentModel) propValue;
                propValue = new ShallowDocumentModel(oldDoc);
            }
            // XXX treat here other cases !!!!
            newProps.put(prop.getKey(), propValue);
        }
        newCtx.setProperties(newProps);
        return new ShallowEvent(event.getName(), newCtx, event.getFlags(), event.getTime());
    }

    public ShallowEvent(String name, EventContext ctx, int flags, long creationTime) {
        super(name, ctx, flags, creationTime);
    }

}
