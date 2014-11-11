/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */


package org.nuxeo.ecm.automation.core.util;

import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Factory interface to create a {@link Blob} from a JSON Object
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 *
 */
public interface JSONBlobDecoder {

    Blob getBlobFromJSON(ObjectNode jsonObject);

}
