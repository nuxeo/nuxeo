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

public abstract class AbstractClassHolderElement extends AbstractElement
        implements ClassHolder {

    String className;

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

}
