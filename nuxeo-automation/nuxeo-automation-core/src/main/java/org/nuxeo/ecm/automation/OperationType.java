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

import java.util.Map;

/**
 * Describe an operation class. Each registered operation will be stored in the
 * registry as an instance of this class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface OperationType {

    String getId();

    Class<?> getType();

    Object newInstance(OperationContext ctx, Map<String, Object> args)
            throws Exception;

    /**
     * Gets the service that registered that type.
     */
    AutomationService getService();

    OperationDocumentation getDocumentation();

}
