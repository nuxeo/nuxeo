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

import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.util.StringList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StringToList implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        String content = (String) objectToAdapt;
        List<String> li = StringUtils.split(content, ',', '\\', true);
        return new StringList(li);
    }

}
