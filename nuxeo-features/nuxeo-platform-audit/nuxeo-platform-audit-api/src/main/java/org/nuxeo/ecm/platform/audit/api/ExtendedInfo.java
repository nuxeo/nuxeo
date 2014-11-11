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
package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;

/**
 * Extended audit info entities, used to persist contributed extended information.
 *
 * @author Stephane Lacoin (Nuxeo EP software engineer)
 */
public interface ExtendedInfo extends Serializable {

    Long getId();

    void setId(Long id);

    Serializable getSerializableValue();

    <T> T getValue(Class<T> clazz);

}
