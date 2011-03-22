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
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.io.Serializable;

import org.w3c.dom.Element;

/**
 * This interface is experimental.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ConfigurationDescriptor extends Serializable {

    Element getElement();

    Object getConfiguration();

}
