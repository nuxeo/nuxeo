import org.nuxeo.theme.Manager
import org.nuxeo.theme.uids.Identifiable

currentThemeName = Context.runScript("getCurrentThemeName.groovy")

styles = []

namedStyles = Manager.getThemeManager().getNamedObjects(currentThemeName, "style")
if (namedStyles) {
    styles.add("")
    for (namedStyle in namedStyles) {
        styles.add(namedStyle.getName())
     }
}

return styles

