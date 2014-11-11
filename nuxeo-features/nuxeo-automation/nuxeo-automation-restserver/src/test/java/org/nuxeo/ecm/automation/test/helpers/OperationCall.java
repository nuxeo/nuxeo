/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test.helpers;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Object that represent a call to the TestOperation automation operation
 *
 * @since 5.7.2
 */
public class OperationCall {

    private DocumentModel doc;

    private String one;

    private Integer two;

    public OperationCall(DocumentModel doc, String one, Integer two) {
        this.doc = doc;
        this.one = one;
        this.two = two;
    }

    public String getParamOne() {
        return one;
    }

    public int getParamTwo() {
        return two;
    }

    public DocumentModel getDocument() {
        return doc;
    }

}
