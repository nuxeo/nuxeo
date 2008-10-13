import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.styles.Style

Style style = Context.runScript("getStyleOfSelectedElement.groovy")
Style ancestor = (Style) ThemeManager.getAncestorFormatOf(style)
if (ancestor != null) {
    return ancestor.getName()
}
return ""
