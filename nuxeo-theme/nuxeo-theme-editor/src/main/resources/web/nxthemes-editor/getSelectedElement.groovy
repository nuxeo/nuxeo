
import org.nuxeo.theme.themes.ThemeManager

id = Context.runScript("getSelectedElementId.groovy")
if (!id) {
    return null
}

return ThemeManager.getElementById(id)
