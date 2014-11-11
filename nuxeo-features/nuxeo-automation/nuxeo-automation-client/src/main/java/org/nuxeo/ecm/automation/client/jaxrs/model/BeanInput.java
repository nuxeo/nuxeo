/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;

/**
 * @author matic
 *
 */
public class BeanInput<T> implements OperationInput {

    protected final Class<T> clazz;
    
    protected final T value;
    
    protected final JsonMarshaller<T> marshaller;
    
    public BeanInput(Class<T> clazz, T value) {
        this.clazz = clazz;
        this.value = value;
        this.marshaller = JsonMarshalling.getMarshaller(clazz);
    }
    
    @Override
    public boolean isBinary() {
        return false;
    }


    @Override
    public String getInputType() {
        return marshaller.getType();
    }

    @Override
    public String getInputRef() {
        return String.format("%s:%s",marshaller.getType(), marshaller.getReference(value));
    }
    
}
