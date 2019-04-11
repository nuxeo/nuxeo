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
 *     Florent Guillaume
 */

package org.nuxeo.runtime.jtajca;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.transaction.UserTransaction;

/**
 * Factory for the UserTransaction.
 */
public class NuxeoUserTransactionFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name objName, Context nameCtx, Hashtable<?, ?> env) {
        Reference ref = (Reference) obj;
        if (!UserTransaction.class.getName().equals(ref.getClassName())) {
            return null;
        }
        return NuxeoContainer.ut;
    }

}
