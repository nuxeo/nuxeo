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
package org.nuxeo.ecm.automation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AdapterNotFoundException extends OperationException {

    private static final long serialVersionUID = 1L;

    public AdapterNotFoundException(String message, OperationContext ctx) {
        super(message+System.getProperty("line.separator")+ctx.getFormattedTrace());
    }

}
