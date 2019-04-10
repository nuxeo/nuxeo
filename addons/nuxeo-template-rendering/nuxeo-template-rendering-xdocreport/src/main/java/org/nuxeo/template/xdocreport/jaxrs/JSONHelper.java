/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.xdocreport.jaxrs;

import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;

import com.fasterxml.jackson.core.JsonGenerator;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class JSONHelper {

    public static void writeResource(Resource resource, OutputStream out) throws IOException {
        String prefix = "{ \"resource\" : { \"name\" : \"resources\", \"type\" : \"CATEGORY\", \"children\" : ";
        String suffix = " }}";
        out.write(prefix.getBytes());
        try (JsonGenerator jg = JsonHelper.createJsonGenerator(out)) {
            jg.writeObject(resource);
        }
        out.write(suffix.getBytes());
    }

}
