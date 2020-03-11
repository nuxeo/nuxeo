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
package org.nuxeo.ecm.automation;

/**
 * An object that can adapt a given object instance to an object of another type A type adapter accepts only one type of
 * objects and can produce only one type of object.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface TypeAdapter {

    /**
     * Adapt the given object to an instance of the given target type. The input object cannot be null. Throws an
     * exception if the object cannot be adapted.
     *
     * @param ctx
     * @param objectToAdapt
     * @throws TypeAdaptException when the object cannot be adapted
     */
    Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException;

}
