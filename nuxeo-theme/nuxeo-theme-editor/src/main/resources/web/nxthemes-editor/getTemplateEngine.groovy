
import org.nuxeo.theme.themes.ThemeManager

applicationPath = Context.runScript("getApplicationPath.groovy")
return ThemeManager.getTemplateEngine(applicationPath)
