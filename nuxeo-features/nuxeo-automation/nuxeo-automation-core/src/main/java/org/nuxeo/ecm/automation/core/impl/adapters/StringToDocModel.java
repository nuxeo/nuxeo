/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.impl.adapters.helper.TypeAdapterHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StringToDocModel implements TypeAdapter {

    @Override
    public DocumentModel getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        try {
            String value = (String) objectToAdapt;
            return ctx.getCoreSession().getDocument(TypeAdapterHelper.createRef(ctx, value));
        } catch (TypeAdaptException e) {
            throw e;
        } catch (NuxeoException e) {
            throw new TypeAdaptException(e);
        }
    }
}
