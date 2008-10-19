
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.editor.StyleFieldProperty
import java.util.regex.Matcher

viewName = Context.runScript("getSelectedViewName.groovy")
Style style =  Context.runScript("getStyleOfSelectedElement.groovy")
Style currentStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")

if (currentStyleLayer != null) {
    style = currentStyleLayer
}

fieldProperties = []
if (style == null) {
    return fieldProperties
}
path = Context.runScript("getCurrentStyleSelector.groovy")
if (path == null) {
    return fieldProperties
}

if (style.getName() != null) {
    viewName = "*"
}

Properties properties = style.getPropertiesFor(viewName, path)
selectedCategory =Context.runScript("getStylePropertyCategory.groovy")

Properties cssProperties = Utils.getCssProperties()
Enumeration<?> propertyNames = cssProperties.propertyNames()
while (propertyNames.hasMoreElements()) {
    name = (String) propertyNames.nextElement()
    value = properties == null ? "" : properties.getProperty(name, "")
    type = cssProperties.getProperty(name)

    if (!selectedCategory.equals("")) {
        Matcher categoryMatcher = cssCategoryPattern.matcher(type)
        if (!categoryMatcher.find()) {
            continue
        }
        if (!categoryMatcher.group(1).equals(selectedCategory)) {
            continue
        }
    }

    fieldProperties.add(new StyleFieldProperty(name, value, type))
}

return fieldProperties
