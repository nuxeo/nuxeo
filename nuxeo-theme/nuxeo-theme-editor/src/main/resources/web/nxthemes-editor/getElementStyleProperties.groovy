
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.editor.StyleFieldProperty
import org.nuxeo.theme.html.Utils
import java.util.regex.Matcher
import java.util.regex.Pattern

Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")

Style style = Context.runScript("getStyleOfSelectedElement.groovy")
Style selectedStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")
if (selectedStyleLayer != null) {
    style = selectedStyleLayer
}

List<StyleFieldProperty> fieldProperties = []
if (style == null) {
    return fieldProperties
}
path = Context.runScript("getSelectedStyleSelector.groovy")
if (path == null) {
    return fieldProperties
}

viewName = Context.runScript("getSelectedViewName.groovy")
if (style.getName() != null) {
    viewName = "*"
}
Properties properties = style.getPropertiesFor(viewName, path)
selectedCategory = Context.runScript("getStylePropertyCategory.groovy")

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

