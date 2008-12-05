import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.styles.Style

selectedStyleLayerId = Context.runScript("getSelectedStyleLayerId.groovy")
if (selectedStyleLayerId == null) {
  return null
}

return (Style) ThemeManager.getFormatById(selectedStyleLayerId)

