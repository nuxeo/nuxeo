/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client.adapters;

import java.io.IOException;

import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PojoMarshaller;

/**
 * This automation client business service provides access to Business operations @{link BusinessCreateOperation} and
 * @{link BusinessUpdateOperation}
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
    public T create(T o, String name, String parentPath) throws IOException {
        checkMarshaller(o);
        return (T) session.newRequest("Business.BusinessCreateOperation").setInput(o).set("name", name).set(
                "parentPath", parentPath).execute();
    }

    /**
     * This method is calling @{BusinessUpdateOperation}
     *
     * @param o the object to send (pojo client side)
     * @return the pojo returned by the server
     */
    public T update(T o) throws IOException {
        checkMarshaller(o);
        return (T) session.newRequest("Business.BusinessUpdateOperation").setInput(o).execute();
    }

    /**
     * This method is calling @{BusinessFetchOperation}
     *
     * @param o the object to send (pojo client side)
     * @return the pojo returned by the server
     */
    public T fetch(T o) throws IOException {
        return (T) session.newRequest("Business.BusinessFetchOperation").setInput(o).execute();
    }
}
