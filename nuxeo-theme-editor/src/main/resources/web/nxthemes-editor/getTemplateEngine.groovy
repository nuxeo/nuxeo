
import org.nuxeo.them.themes.ThemeManager

applicationPath = Context.runScript("getApplicationPath.groovy")
return ThemeManager.getTemplateEngine(applicationPath);
