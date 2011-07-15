/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.server.jaxrs.io.resolvers;

import org.nuxeo.ecm.automation.server.jaxrs.io.InputResolver;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author matic
 *
 */
public class DocumentInputResolver implements InputResolver<DocumentRef> {

    @Override
    public String getType() {
       return "doc";
    }

    @Override
    public DocumentRef getInput(String content) {
        return docRefFromString(content);
    }

     public static DocumentRef docRefFromString(String input) {
        if (input.startsWith("/")) {
            return new PathRef(input);
        } else {
            return new IdRef(input);
        }
    }

}
