import org.nuxeo.theme.Manager
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.formats.Format
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.editor.StyleLayer
import org.nuxeo.theme.themes.ThemeManager

Style style = Context.runScript("getStyleOfSelectedElement.groovy")
if (style == null) {
  return []
}

Style selectedStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")

layers = []
layers.add(new StyleLayer("This style", style.getUid(), style == selectedStyleLayer || selectedStyleLayer == null))

for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
    layers.add(1, new StyleLayer(ancestor.getName(), ancestor.getUid(),
    ancestor == selectedStyleLayer))
}

return layers
