
import org.nuxeo.theme.formats.styles.Style

viewName = Context.runScript("getSelectedViewName.groovy")
Style style =  Context.runScript("getStyleOfSelectedElement.groovy")
Style currentStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")

selectors = []

if (currentStyleLayer != null) {
    style = currentStyleLayer
}

if (style != null) {
    // named styles are not associated to any view
    if (style.getName() != null) {
        viewName = "*"
    }
    Set<String> paths = style.getPathsForView(viewName)
    current = Context.runScript("getSelectedStyleSelector.groovy")
    if (current != null && !paths.contains(current)) {
        selectors.add(current)
    }
    for (path in paths) {
        selectors.add(path)
    }
}

return selectors
