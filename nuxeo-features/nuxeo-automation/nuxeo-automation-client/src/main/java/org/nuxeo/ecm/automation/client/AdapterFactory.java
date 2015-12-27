/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

/**
 * A factory for adapters. Adapters can be used to adapt client and session objects. For example you can contribute an
 * adapter on the session to have an API suited for your needs.
 * <p>
 * To register adapters use {@link AutomationClient#registerAdapter(AdapterFactory)}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface AdapterFactory<T> {

    /**
     * Adapt the given object and return the adapter instance.
     *
     * @param toAdapt
     * @return
     */
    T getAdapter(Session session, Class<T> clazz);

}
