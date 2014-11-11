/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * Light Event implementation
 * Used to reduce memory footprint of {@link Event} stacked in {@link EventBundle}
 *
 * @author Thierry Delprat
 *
 */
public class ShallowEvent extends EventImpl {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static ShallowEvent create(Event event) {
        EventContext ctx = event.getContext();
        List<Object> newArgs = new ArrayList<Object>();
        for (Object arg : ctx.getArguments()) {
            Object newArg = arg;
            if (arg instanceof DocumentModel) {
                DocumentModel oldDoc = (DocumentModel) arg;
                DocumentRef ref = oldDoc.getRef();
                if (ref != null) {
                    //newArg = new DocumentModelImpl(null,oldDoc.getType(),oldDoc.getId(),oldDoc.getPath(), oldDoc.getRef(), oldDoc.getParentRef(), oldDoc.getDeclaredSchemas(), oldDoc.getDeclaredFacets());
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
            newCtx = new DocumentEventContext(null, ctx.getPrincipal(),
                    (DocumentModel) newArgs.get(0), (DocumentRef) newArgs
                            .get(1));
        } else {
            newCtx = new EventContextImpl(null, ctx.getPrincipal());
            ((EventContextImpl) newCtx).setArgs(newArgs.toArray());
        }

        newCtx.setRepositoryName(ctx.getRepositoryName());
        Map<String, Serializable> newProps = new HashMap<String, Serializable>();
        for (Entry<String, Serializable> prop : ctx.getProperties().entrySet()) {
            Serializable propValue = prop.getValue();
            if (propValue instanceof DocumentModel) {
                DocumentModel oldDoc = (DocumentModel) propValue;
                //propValue = new DocumentModelImpl(null,oldDoc.getType(),oldDoc.getId(),oldDoc.getPath(), oldDoc.getRef(), oldDoc.getParentRef(), oldDoc.getDeclaredSchemas(), oldDoc.getDeclaredFacets());
                propValue = new ShallowDocumentModel(oldDoc);
            }
            // XXX treat here other cases !!!!
            newProps.put(prop.getKey(), propValue);
        }
        newCtx.setProperties(newProps);
        return new ShallowEvent(event.getName(), newCtx, event.getFlags(),
                event.getTime());
    }

    public ShallowEvent(String name, EventContext ctx, int flags,
            long creationTime) {
        super(name, ctx, flags, creationTime);
    }

}
