import java.util.Properties
import org.nuxeo.theme.editor.FieldProperty
import org.nuxeo.theme.properties.FieldIO
import org.nuxeo.theme.properties.FieldInfo

selectedElement = Context.runScript("getSelectedElement.groovy")

def FieldInfo getFieldInfo(Class<?> c, String name) {
    try {
        return c.getField(name).getAnnotation(FieldInfo.class);
    } catch (Exception e) {
    }
    return null;
}


fieldProperties = []
if (selectedElement == null) {
    return fieldProperties
}

Properties properties = new Properties()
try {
    properties = FieldIO.dumpFieldsToProperties(selectedElement)
} catch (Exception e) {
    return fieldProperties
}

if (properties == null) {
    return fieldProperties
}

c = selectedElement.getClass()
names = properties.propertyNames()
while (names.hasMoreElements()) {
    name = (String) names.nextElement()
    value = properties.getProperty(name)
    fieldProperties.add(new FieldProperty(name, value.trim(), getFieldInfo(c, name)))
}

return fieldProperties

