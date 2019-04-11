/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to collect method annotations from inheritance tree.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
class MethodAnnotations {

    protected final List<Entry> entries = new ArrayList<>();

    public void addMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            addMethod(m);
        }
    }

    public void addMethod(Method m) {
        int mod = m.getModifiers();
        if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
            return; // ignore non public or protected methods
        }
        Annotation[] annos = m.getDeclaredAnnotations();
        if (annos.length == 0) {
            return;
        }
        for (int i = 0, len = entries.size(); i < len; i++) {
            Entry entry = entries.get(i);
            if (entry.isSameAs(m)) {
                for (Annotation anno : annos) {
                    Class<?> annoType = anno.annotationType();
                    Annotation a = entry.annos.get(annoType);
                    if (a == null) {
                        // TODO support merging annotations?
                        entry.annos.put(anno.annotationType(), anno);
                        entry.method = m;
                    } else {
                        entry.annos.put(anno.annotationType(), anno);
                        entry.method = m;
                    }
                }
                return;
            }
        }
        entries.add(new Entry(m, annos));
    }

    public void addSuperMethod(AnnotatedMethod am) {
        for (int i = 0, len = entries.size(); i < len; i++) {
            Entry entry = entries.get(i);
            if (entry.isSameAs(am.method)) {
                Annotation[] annos = am.getAnnotations();
                for (Annotation anno : annos) {
                    Class<?> annoType = anno.annotationType();
                    Annotation a = entry.annos.get(annoType);
                    if (a == null) {
                        // TODO support merging annotations?
                        entry.annos.put(anno.annotationType(), anno);
                    } else {
                        entry.annos.put(anno.annotationType(), anno);
                    }
                }
                return;
            }
        }
        entries.add(new Entry(am.method, am.getAnnotations()));
    }

    static class Entry {
        final Class<?>[] parameterTypes; // store as member to avoid cloning when calling getParameterTypes()

        Method method;

        final Map<Class<? extends Annotation>, Annotation> annos;

        Entry(Method method, Annotation[] annos) {
            this.method = method;
            parameterTypes = method.getParameterTypes();
            this.annos = new HashMap<>();
            for (Annotation anno : annos) {
                this.annos.put(anno.annotationType(), anno);
            }
        }

        public boolean isSameAs(Method m) {
            if (method.getName().equals(m.getName())) {
                if (method.getReturnType() == m.getReturnType()) {
                    Class<?>[] pt = m.getParameterTypes();
                    if (parameterTypes.length == pt.length) {
                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (parameterTypes[i] != pt[i]) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
