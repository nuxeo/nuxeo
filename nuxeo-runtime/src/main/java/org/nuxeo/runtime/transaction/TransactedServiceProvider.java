/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.AnnotatedServiceProvider;

/**
 * Allocate transacted invocation handlers and return proxies if
 * service is annotated with Transacted annotations. The provider is to be
 * lazy installed by the client module using the install method.
 *
 * @author matic
 *
 */
public class TransactedServiceProvider extends AnnotatedServiceProvider {

    protected static final Log log = LogFactory.getLog(TransactedServiceProvider.class);

    public static final TransactedServiceProvider INSTANCE = new TransactedServiceProvider();

    public static void install() {
        INSTANCE.installSelf();
    }

    @Override
    protected <T> T newProxy(T object, Class<T> clazz) {
        return  TransactedInstanceHandler.newProxy(object, clazz);
    }

    @Override
    protected Class<Transacted> annotationClass() {
        return Transacted.class;
    }


}
