import org.nuxeo.theme.Manager
import org.nuxeo.theme.formats.styles.Style

selectedStyleLayerId = Context.runScript("getSelectedStyleLayerId.groovy")
if (selectedStyleLayerId == null) {
  return null
}

return (Style) Manager.getUidManager().getObjectByUid(selectedStyleLayerId)

