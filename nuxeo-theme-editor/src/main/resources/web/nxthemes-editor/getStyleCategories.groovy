import java.util.regex.Matcher
import java.util.regex.Pattern
import org.nuxeo.theme.html.Utils
import org.nuxeo.theme.editor.StyleCategory

selectedStyleCategory = Context.runScript("getStylePropertyCategory.groovy")

Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")

Map<String, StyleCategory> categories = new LinkedHashMap<String, StyleCategory>()
Enumeration<?> elements = Utils.getCssProperties().elements()

categories.put("", new StyleCategory("", "all", selectedStyleCategory.equals("")))
        
while (elements.hasMoreElements()) {
    element = (String) elements.nextElement()
    Matcher categoryMatcher = cssCategoryPattern.matcher(element)
    if (categoryMatcher.find()) {
        value = categoryMatcher.group(1)
        boolean selected = value.equals(selectedStyleCategory)
        categories.put(value, new StyleCategory(value, value, selected))
    }
}

return new ArrayList<StyleCategory>(categories.values())
