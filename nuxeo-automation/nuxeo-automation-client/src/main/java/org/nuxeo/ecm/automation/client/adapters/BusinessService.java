/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client.adapters;

import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PojoMarshaller;

/**
 * This automation client business service provides access to Business
 * operations @{link BusinessCreateOperation} and @{link
 * BusinessUpdateOperation}
 *
 * @since 5.7
 */
public class BusinessService<T> {

    protected final Session session;

    public BusinessService(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    /**
     * Check if Client JsonMarshalling already contains o Marshaller
     *
     * @param o the pojo to add to Client JsonMarshalling
     */
    private void checkMarshaller(T o) {
        if (JsonMarshalling.getMarshaller(o.getClass()) == null) {
            JsonMarshalling.addMarshaller(PojoMarshaller.forClass(o.getClass()));
        }
    }

    /**
     * This method is calling @{BusinessCreateOperation}
     *
     * @param o the object to send (pojo client side)
     * @param name the id/name of the NX document
     * @param parentPath the path of the NX document parent
     * @return the pojo returned by the server
     */
    @SuppressWarnings("unchecked")
    public T create(T o, String name, String parentPath) throws Exception {
        checkMarshaller(o);
        return (T) session.newRequest("Business.BusinessCreateOperation").setInput(
                o).set("name", name).set("parentPath", parentPath).execute();
    }

    /**
     * This method is calling @{BusinessUpdateOperation}
     *
     * @param o the object to send (pojo client side)
     * @return the pojo returned by the server
     */
    public T update(T o) throws Exception {
        checkMarshaller(o);
        return (T) session.newRequest("Business.BusinessUpdateOperation").setInput(
                o).execute();
    }

    /**
     * This method is calling @{BusinessFetchOperation}
     *
     * @param o the object to send (pojo client side)
     * @return the pojo returned by the server
     */
    public T fetch(T o) throws Exception {
        return (T) session.newRequest("Business.BusinessFetchOperation").setInput(
                o).execute();
    }
}
