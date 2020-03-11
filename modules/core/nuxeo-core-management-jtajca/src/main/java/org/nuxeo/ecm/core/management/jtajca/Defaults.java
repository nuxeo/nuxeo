/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.nuxeo.ecm.core.api.CoreSessionService.CoreSessionRegistrationInfo;

public class Defaults {

    public static final Defaults instance = new Defaults();

    public String name(Class<?> clazz) {
        return name(clazz, "default");
    }

    public String name(Class<?> clazz, String name) {
        return clazz.getPackage().getName() + ":type=" + clazz.getSimpleName() + ",name=" + name;
    }

    public ObjectName objectName(Class<?> clazz, String name) {
        try {
            return new ObjectName(name(clazz, name));
        } catch (MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Cannot build  " + name, e);
        }
    }

    public String printStackTrace(CoreSessionRegistrationInfo info) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            info.printStackTrace(new PrintStream(bos));
            return bos.toString();
        } catch (IOException e) {
            throw new RuntimeException("Cannot write stack to byte array", e);
        }
    }
}
