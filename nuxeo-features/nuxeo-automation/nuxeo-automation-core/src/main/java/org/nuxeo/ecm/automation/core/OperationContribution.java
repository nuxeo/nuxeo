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
package org.nuxeo.ecm.automation.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.core.annotations.Operation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("operation")
public class OperationContribution {

    /**
     * The operation class that must be annotated using {@link Operation}
     * annotation.
     */
    @XNode("@class")
    public Class<?> type;

    /**
     * Put it to true to override an existing contribution having the same ID.
     * By default overriding is not permitted and an exception is thrown when
     * this flag is on false.
     */
    @XNode("@replace")
    public boolean replace;

}
