package org.nuxeo.apidoc.introspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SPI {

    protected static List<Class<?>> filter(Class<?> klass) {
        List<Class<?>> spi = new ArrayList<Class<?>>();
        for (Field field : klass.getDeclaredFields()) {
            String cName = field.getType().getCanonicalName();
            if (cName.startsWith("org.nuxeo")) {
                // remove XObjects
                Class<?> fieldClass = field.getType();
                Annotation[] annotations = fieldClass.getDeclaredAnnotations();
                if (annotations.length == 0) {
                    spi.add(fieldClass);
                }
            }
        }
        return spi;
    }

}
