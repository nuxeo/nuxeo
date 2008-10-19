
import org.nuxeo.theme.Manager
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.Format
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.html.Utils
import org.nuxeo.theme.presets.PresetType

selectedElementId = Context.runScript("getSelectedElementId.groovy")
selectedLayerId = Context.runScript("getSelectedStyleLayerId.groovy")
selectedStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")
selectedViewName = Context.runScript("getSelectedViewName.groovy")
selectedElement = Context.runScript("getSelectedElement.groovy")

FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
Style style = (Style) ElementFormatter.getFormatByType(selectedElement, styleType)

if (selectedStyleLayer != null) {
    style = selectedStyleLayer
}
if (style == null) {
    return ""
}

StringBuilder css = new StringBuilder()

List<Style> styles = new ArrayList<Style>()
for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
    styles.add(0, (Style) ancestor)
}
styles.add(style)

for (Style s : styles) {
    viewName = selectedViewName
    if (s.getName() != null) {
        viewName = "*"
    }
    for (path in s.getPathsForView(viewName)) {
        css.append("#stylePreviewArea")
        css.append(' ').append(path).append(" {")

        Properties styleProperties = s.getPropertiesFor(viewName, path)
        Enumeration<?> propertyNames = Utils.getCssProperties().propertyNames()
        while (propertyNames.hasMoreElements()) {
            propertyName = (String) propertyNames.nextElement()
            value = styleProperties.getProperty(propertyName)
            if (value == null) {
                continue
            }
            css.append(propertyName)
            css.append(':')
            PresetType preset = ThemeManager.resolvePreset(value)
            if (preset != null) {
                value = preset.getValue()
            }
            css.append(value)
            css.append('')
       }
       css.append('}')
    }
}

Response.writer.write(css.toString())
