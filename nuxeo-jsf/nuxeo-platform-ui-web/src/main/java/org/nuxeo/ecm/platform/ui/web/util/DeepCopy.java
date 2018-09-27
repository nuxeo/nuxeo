/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DeepCopy.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

/**
 * Deep copy utils.
 * <p>
 * Most classes implementing the {@link Cloneable} interface only perform a shallow copy of an object. This class
 * performs deep cloning serializing and deserializing an object. Therefore only serializable objects can be copied
 * using this util.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
// XXX AT: needs optimizing, see
// http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
public final class DeepCopy {

    // Utility class.
    private DeepCopy() {
    }

    public static Object deepCopy(Object object) {
        return SerializationUtils.clone((Serializable) object);
    }

}
