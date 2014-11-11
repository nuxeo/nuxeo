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
 * $Id$
 */

package org.nuxeo.runtime.model;


/**
 * @author Bogdan Stefanescu
 *
 */
public interface ComponentContext {

    Property getProperty(String property);

    Object getPropertyValue(String property, Object defValue);

    Object getPropertyValue(String property);

    String[] getPropertyNames();

    RuntimeContext getRuntimeContext();

}
