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
package org.nuxeo.ecm.automation.jaxrs;

import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultJsonAdapter implements JsonAdapter {

    protected Object object;

    public DefaultJsonAdapter(Object object) {
        this.object = object;
    }

    @Override
    public void toJSON(OutputStream out) throws IOException {
        ObjectCodecService service = Framework.getService(ObjectCodecService.class);
        service.write(out, object);
    }

}
