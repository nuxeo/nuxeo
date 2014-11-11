
import org.nuxeo.theme.html.Utils
import org.nuxeo.theme.formats.styles.Style


Style style = Context.runScript("getStyleOfSelectedElement.groovy")
Style currentStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")

if (currentStyleLayer != null) {
    style = currentStyleLayer
}
if (style == null) {
    return ""
}

viewNames = []
viewName = Context.runScript("getSelectedViewName.groovy")
if (style.getName() != null) {
    viewName = "*"
}
viewNames.add(viewName)

RESOLVE_PRESETS = false
IGNORE_VIEW_NAME = true
IGNORE_CLASSNAME = true
INDENT = true

return Utils.styleToCss(style, viewNames, RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT)
