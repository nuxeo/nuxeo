/*
 * (C) Copyright 2011 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.theme.elements;

/**
 * Interface implemented by element that carry class information (typically
 * rendered as HTML 'class' attributes).
 */
public interface ClassHolder {

    String getClassName();

    void setClassName(String className);

}
