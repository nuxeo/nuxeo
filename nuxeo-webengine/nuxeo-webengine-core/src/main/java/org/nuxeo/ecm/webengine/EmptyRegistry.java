/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * Empty impl for deprecated {@link ResourceRegistry}. This will be removed in future.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated
 */
@Deprecated
public class EmptyRegistry implements ResourceRegistry {

    @Override
    public void addBinding(ResourceBinding binding) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addMessageBodyReader(MessageBodyReader<?> reader) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addMessageBodyWriter(MessageBodyWriter<?> writer) {
        // TODO Auto-generated method stub
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
    }

    @Override
    public ResourceBinding[] getBindings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void reload() {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeBinding(ResourceBinding binding) {
        // TODO Auto-generated method stub
    }

}
