package nxthemesEditor;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.theme.*;
import org.nuxeo.theme.elements.*;
import org.nuxeo.theme.formats.*;
import org.nuxeo.theme.formats.widgets.*;
import org.nuxeo.theme.formats.styles.*;
import org.nuxeo.theme.formats.layouts.*;
import org.nuxeo.theme.fragments.*;
import org.nuxeo.theme.presets.*;
import org.nuxeo.theme.templates.*;
import org.nuxeo.theme.themes.*;
import org.nuxeo.theme.types.*;
import org.nuxeo.theme.perspectives.*;
import org.nuxeo.theme.uids.*;
import org.nuxeo.theme.views.*;
import org.nuxeo.theme.editor.*;

@WebModule(name="nxthemes-editor")

@Path("/nxthemes-editor")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {

  @GET @POST
  @Path("perspectiveSelector")
  public Object renderPerspectiveSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("perspectiveSelector.ftl").arg("perspectives", getPerspectives());
  }

  @GET @POST
  @Path("themeSelector")
  public Object renderThemeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("themeSelector.ftl").arg(
            "current_theme_name", getCurrentThemeName(path)).arg(
            "themes", getThemes(path)).arg(
            "pages", getPages(path));
  }

  @GET @POST
  @Path("canvasModeSelector")
  public Object renderCanvasModeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("canvasModeSelector.ftl");
  }

  @GET @POST
  @Path("backToCanvas")
  public Object renderBackToCanvas(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("backToCanvas.ftl");
  }

  @GET @POST
  @Path("themeManager")
  public Object renderThemeManager(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("themeManager.ftl").arg("themes", getThemeDescriptors());
  }

  @GET @POST
  @Path("fragmentFactory")
  public Object renderFragmentFactory(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("fragmentFactory.ftl").arg("fragments", getFragments());
  }

  @GET @POST
  @Path("elementEditor")
  public Object renderElementEditor(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementEditor.ftl").arg("selected_element", getSelectedElement());
  }

  @GET @POST
  @Path("elementDescription")
  public Object renderElementDescription(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementDescription.ftl").arg("selected_element", getSelectedElement());
  }

  @GET @POST
  @Path("elementPadding")
  public Object renderElementPadding(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementPadding.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "padding_of_selected_element", getPaddingOfSelectedElement());
  }

  @GET @POST
  @Path("elementProperties")
  public Object renderElementProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementProperties.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "element_properties", getSelectedElementProperties());
  }

  @GET @POST
  @Path("elementStyle")
  public Object renderElementStyle(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementStyle.ftl").arg("selected_element", getSelectedElement()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "style_edit_mode", getStyleEditMode()).arg(
            "style_of_selected_element", getStyleOfSelectedElement()).arg(
            "current_theme_name", getCurrentThemeName(path)).arg(
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "inherited_style_name_of_selected_element", getInheritedStyleNameOfSelectedElement()).arg(
            "named_styles", getNamedStyles());
  }

  @GET @POST
  @Path("elementWidget")
  public Object renderElementWidget(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementWidget.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "view_names_for_selected_element", getViewNamesForSelectedElement(path));
  }

  @GET @POST
  @Path("elementVisibility")
  public Object renderElementVisibility(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementVisibility.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "is_selected_element_always_visible", isSelectedElementAlwaysVisible()).arg(
            "perspectives", getPerspectives());
  }

  @GET @POST
  @Path("stylePicker")
  public Object renderStylePicker(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("stylePicker.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup());
  }
  
  @GET @POST
  @Path("areaStyleChooser")
  public Object renderAreaStyleChooser(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("areaStyleChooser.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup()).arg(
            "selected_preset_group", getSelectedPresetGroup());
  }

  @GET @POST
  @Path("styleProperties")
  public Object renderStyleProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("styleProperties.ftl").arg(
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "style_selectors", getStyleSelectorsForSelectedElement()).arg(
            "rendered_style_properties", getRenderedStylePropertiesForSelectedElement()).arg(
            "selected_style_selector", getSelectedStyleSelector()).arg(
            "style_properties", getStylePropertiesForSelectedElement()).arg(
            "style_categories", getStyleCategories()).arg(
            "element_style_properties", getElementStyleProperties());
  }
  
  @GET @POST
  @Path("select_element")
  public void selectElement(@QueryParam("id") String id) {
    def ctx = WebEngine.getActiveContext();
    SessionManager.setSelectedElementId(ctx, id);
  }
   
  public static List<ThemeDescriptor> getThemeDescriptors() {
    return ThemeManager.getThemesDescriptors()
  }
  
  public static List<Identifiable> getNamedStyles() {
      String currentThemeName = getCurrentThemeName();
      def styles = [];
      namedStyles = Manager.getThemeManager().getNamedObjects(currentThemeName, "style");
      if (namedStyles) {
          styles.add("");
          for (namedStyle in namedStyles) {
              styles.add(namedStyle.getName());
           }
      }
      return styles;
  }
  
  public static List<FragmentType> getFragments(String applicationPath) {
      def fragments = []
      String templateEngine = getTemplateEngine(applicationPath);
      for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
          FragmentType fragmentType = (FragmentType) f;
          FragmentInfo fragmentInfo = new FragmentInfo(fragmentType);
          for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
              String viewTemplateEngine = viewType.getTemplateEngine();
              if (!"*".equals(viewType.getViewName()) && templateEngine.equals(viewTemplateEngine)) {
                  fragmentInfo.addView(viewType);
              }
          }
          if (fragmentInfo.size() > 0) {
               fragments.add(fragmentInfo);
          }
      }
      return fragments;
  }
  
  public static Element getSelectedElement() {
    def ctx = WebEngine.getActiveContext();
    String id = SessionManager.getSelectedElementId(ctx);
    if (id == null) {
      return null;
    }
    return ThemeManager.getElementById(id);
  } 
  
  public static List<StyleLayer> getStyleLayersOfSelectedElement() {
      Style style = getStyleOfSelectedElement();
      if (style == null) {
        return [];
      }
      Style selectedStyleLayer = getSelectedStyleLayer();
      def layers = [];
      layers.add(new StyleLayer("This style", style.getUid(), style == selectedStyleLayer || selectedStyleLayer == null));
      for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
          layers.add(1, new StyleLayer(ancestor.getName(), ancestor.getUid(), ancestor == selectedStyleLayer));
      }
      return layers;
  }
  
  public static boolean isSelectedElementAlwaysVisible() {
      Element selectedElement = getSelectedElement();
      return Manager.getPerspectiveManager().isAlwaysVisible(selectedElement);
  }
  
  public static List<PerspectiveType> getPerspectives() {
      return PerspectiveManager.listPerspectives();
  }
  
  public static List<PerspectiveType> getPerspectivesOfSelectedElement() {
      Element selectedElement = getSelectedElement();
      def perspectives = [];
      for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(selectedElement)) {
          perspectives.add(perspectiveType.name);
      }
      return perspectives;
  }
  
  public static String getStyleEditMode() {
      def ctx = WebEngine.getActiveContext();
      return SessionManager.getStyleEditMode(ctx);
  }
  
  public static List<String> getStyleSelectorsForSelectedElement() {
      Element element = getSelectedElement();
      String viewName = getSelectedViewName();
      Style style = getStyleOfSelectedElement();
      Style selectedStyleLayer = getSelectedStyleLayer();
      def selectors = [];
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer;
      }
      if (style != null) {
          if (style.getName() != null) {
              viewName = "*";
          }
          Set<String> paths = style.getPathsForView(viewName);
          String current = getSelectedStyleSelector();
          if (current != null && !paths.contains(current)) {
              selectors.add(current);
          }
          for (path in paths) {
              selectors.add(path);
          }
      }
      return selectors;
  }
  
  public static getElementStyleProperties() {
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>");
      Style style = getStyleOfSelectedElement();
      Style selectedStyleLayer = getSelectedStyleLayer();
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer;
      }
      List<StyleFieldProperty> fieldProperties = [];
      if (style == null) {
          return fieldProperties;
      }
      String path = getSelectedStyleSelector();
      if (path == null) {
          return fieldProperties;
      }
      String viewName = getSelectedViewName();
      if (style.getName() != null) {
          viewName = "*";
      }
      Properties properties = style.getPropertiesFor(viewName, path);
      String selectedCategory = getStylePropertyCategory();

      Properties cssProperties = Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement();
          String value = properties == null ? "" : properties.getProperty(name, "");
          String type = cssProperties.getProperty(name);
          if (!selectedCategory.equals("*")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type);
              if (!categoryMatcher.find()) {
                  continue;
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue;
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type));
      }
      return fieldProperties;    
  }
  
  public static List getStylePropertiesForSelectedElement() {
      String viewName = getSelectedViewName();
      Style style = getStyleOfSelectedElement();
      Style selectedStyleLayer = getSelectedStyleLayer();
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer;
      }
      def fieldProperties = [];
      if (style == null) {
          return fieldProperties;
      }
      String path = getSelectedStyleSelector();
      if (path == null) {
          return fieldProperties;
      }
      if (style.getName() != null) {
          viewName = "*";
      }
      Properties properties = style.getPropertiesFor(viewName, path);
      String selectedCategory = getSelectedStylePropertyCategory();
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>");
      Properties cssProperties = Utils.getCssProperties();
      Enumeration<?> propertyNames = cssProperties.propertyNames();
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement();
          String value = properties == null ? "" : properties.getProperty(name, "");
          String type = cssProperties.getProperty(name);
          if (!selectedCategory.equals("")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type);
              if (!categoryMatcher.find()) {
                  continue;
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue;
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type));
      }
      return fieldProperties;
  }
  
  public static String getStylePropertyCategory() {
      def ctx = WebEngine.getActiveContext();
      String category = SessionManager.getStylePropertyCategory(ctx);
      if (!category) {
          category = '*';
      }
      return category;
  }
  
  public static List<StyleCategory> getStyleCategories() {
      String selectedStyleCategory = getStylePropertyCategory();
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>");
      Map<String, StyleCategory> categories = new LinkedHashMap<String, StyleCategory>();
      Enumeration<?> elements = Utils.getCssProperties().elements();
      categories.put("", new StyleCategory("*", "all", selectedStyleCategory.equals("*")));
      while (elements.hasMoreElements()) {
          Element element = (String) elements.nextElement();
          Matcher categoryMatcher = cssCategoryPattern.matcher(element);
          if (categoryMatcher.find()) {
              String value = categoryMatcher.group(1);
              boolean selected = value.equals(selectedStyleCategory);
              categories.put(value, new StyleCategory(value, value, selected));
          }
      }
      return new ArrayList<StyleCategory>(categories.values());
  }
  
  public String getInheritedStyleNameOfSelectedElement() {
      Style style = getStyleOfSelectedElement();
      Style ancestor = (Style) ThemeManager.getAncestorFormatOf(style);
      if (ancestor != null) {
          return ancestor.getName();
      }
      return "";
  }
  
  public List<StyleFieldProperty> getElementStyleProperties() {
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>");
      Style style = getStyleOfSelectedElement();
      Style selectedStyleLayer = getSelectedStyleLayer();
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer;
      }
      List<StyleFieldProperty> fieldProperties = [];
      if (style == null) {
          return fieldProperties;
      }
      path = getSelectedStyleSelector();
      if (path == null) {
          return fieldProperties;
      }
      viewName = getSelectedViewName();
      if (style.getName() != null) {
          viewName = "*";
      }
      Properties properties = style.getPropertiesFor(viewName, path);
      String selectedCategory = getStylePropertyCategory();
      Properties cssProperties = Utils.getCssProperties();
      Enumeration<?> propertyNames = cssProperties.propertyNames();
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement();
          String value = properties == null ? "" : properties.getProperty(name, "");
          String type = cssProperties.getProperty(name);
          if (!selectedCategory.equals("*")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type);
              if (!categoryMatcher.find()) {
                  continue;
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue;
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type));
      }
      return fieldProperties;   
  }
  
  public static String getSelectedStyleSelector() {
      def ctx = WebEngine.getActiveContext();
      return SessionManager.getSelectedStyleSelector(ctx);
  }
  
  public static Style getSelectedStyleLayer() {
      String selectedStyleLayerId = getSelectedStyleLayerId();
      if (selectedStyleLayerId == null) {
        return null;
      }
      return (Style) ThemeManager.getFormatById(selectedStyleLayerId);
  }
  
  public static String getSelectedStyleLayerId() {
      def ctx = WebEngine.getActiveContext();
      return SessionManager.getSelectedStyleLayerId(ctx);
  } 
  
  public static Style getStyleOfSelectedElement() {
      Element element = getSelectedElement();
      if (element == null) {
        return null;
      }
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style");
      return (Style) ElementFormatter.getFormatByType(element, styleType);
  }
  
  public static PaddingInfo getPaddingOfSelectedElement() {
      Element element = getSelectedElement();
      String top = "";
      String bottom = "";
      String left = "";
      String right = "";
      if (element != null) {
          Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout");
          top = layout.getProperty("padding-top");
          bottom = layout.getProperty("padding-bottom");
          left = layout.getProperty("padding-left");
          right = layout.getProperty("padding-right");
      }
      return new PaddingInfo(top, bottom, left, right);
  }

  public static String getRenderedStylePropertiesForSelectedElement() {
      Style style = getStyleOfSelectedElement();
      Style currentStyleLayer = getSelectedStyleLayer();
      if (currentStyleLayer != null) {
          style = currentStyleLayer;
      }
      if (style == null) {
          return "";
      }
      def viewNames = [];
      String viewName = getSelectedViewName();
      if (style.getName() != null) {
          viewName = "*";
      }
      viewNames.add(viewName);
      boolean RESOLVE_PRESETS = false;
      boolean IGNORE_VIEW_NAME = true;
      boolean IGNORE_CLASSNAME = true;
      boolean INDENT = true;
      return Utils.styleToCss(style, viewNames, RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT);
  }
  
  public static List<String> getPresetGroupsForSelectedCategory() {
      def groups = [];
      String category = getSelectedStyleCategory();
      groups.add("");
      if (category == null) {
          return groups;
      }
      def groupNames = [];
      for (Type type : Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
          PresetType preset = (PresetType) type;
          String group = preset.getGroup();
          if (!preset.getCategory().equals(category)) {
              continue;
          }
          if (!groupNames.contains(group)) {
              groups.add(group);
          }
          groupNames.add(group);
      }
      return groups;
  }
    
  public static Widget getWidgetOfSelectedElement() {
    Element element = getSelectedElement();
    if (element == null) {
      return null;
    }
    FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget");
    return (Widget) ElementFormatter.getFormatByType(element, widgetType);
  }
  
  public static String getViewNameOfSelectedElement() {
    Widget widget = getWidgetOfSelectedElement();
    if (widget == null) {
      return "";
    }
    return widget.getName();
  }

  public static List<String> getViewNamesForSelectedElement(String applicationPath) {
      Element selectedElement = getSelectedElement();
      String templateEngine = getTemplateEngine(applicationPath);
      def viewNames = [];
      if (selectedElement == null) {
          return viewNames;
      }
      if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
          return viewNames;
      }
      FragmentType fragmentType = ((Fragment) selectedElement).getFragmentType();
      for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
          String viewName = viewType.getViewName();
          String viewTemplateEngine = viewType.getTemplateEngine();
          if (!"*".equals(viewName) && templateEngine.equals(viewTemplateEngine)) {
              viewNames.add(viewName);
          }
      }
      return viewNames;
  }
   
  public static List<FieldProperty> getSelectedElementProperties() {
      Element selectedElement = getSelectedElement();
      return org.nuxeo.theme.editor.Utils.getPropertiesOf(selectedElement); 
  }
  
  public static List<PresetInfo> getPresetsForSelectedGroup() {
      String category = getSelectedStyleCategory();
      String group = getSelectedPresetGroup();
      def presets = [];
      for (type in Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
          PresetType preset = (PresetType) type;
          if (!preset.getCategory().equals(category)) {
              continue;
          }
          if (!preset.getGroup().equals(group)) {
              continue;
          }
          presets.add(new PresetInfo(preset));
      }
      return presets;
  }
  
  public static String getSelectedPresetGroup() {
      def ctx = WebEngine.getActiveContext();
      String category = SessionManager.getPresetGroup(ctx);
      return category;
  }
  
  public static String getSelectedStyleCategory() {
        def ctx = WebEngine.getActiveContext();
        String category = SessionManager.getStyleCategory(ctx);
        if (!category) {
            category = "page";
        }
        return category;
  }
  
  public static getStylePropertiesForSelectedElement () {
      String viewName = getSelectedViewName();
      Style style = getStyleOfSelectedElement();
      Style selectedStyleLayer = getSelectedStyleLayer();
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      def fieldProperties = []
      if (style == null) {
          return fieldProperties;
      }
      String path = getSelectedStyleSelector();
      if (path == null) {
          return fieldProperties;
      }
      if (style.getName() != null) {
          viewName = "*";
      }
      Properties properties = style.getPropertiesFor(viewName, path);
      String selectedCategory = getSelectedStylePropertyCategory();
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>");
      Properties cssProperties = Utils.getCssProperties();
      Enumeration<?> propertyNames = cssProperties.propertyNames();
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement();
          String value = properties == null ? "" : properties.getProperty(name, "");
          String type = cssProperties.getProperty(name);
          if (!selectedCategory.equals("")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type);
              if (!categoryMatcher.find()) {
                  continue;
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue;
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type));
      }
      return fieldProperties;
  }
  
  public static String getTemplateEngine(String applicationPath) {
      return ThemeManager.getTemplateEngineName(applicationPath);
  }
  
  public static String getDefaultTheme(String applicationPath) {
    return ThemeManager.getDefaultTheme(applicationPath);
  }

  public static String getCurrentThemeName(String applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath);
    def ctx = WebEngine.getActiveContext();
    String currentPagePath = ctx.getCookie("nxthemes.theme");
    if (currentPagePath == null) {
      currentPagePath = defaultTheme;
    }
    return currentPagePath.split("/")[0];
  }

  public static List<PageElement> getPages(String applicationPath) {
    ThemeManager themeManager = Manager.getThemeManager();
    def ctx = WebEngine.getActiveContext();
    String currentPagePath = ctx.getCookie("nxthemes.theme");
    String defaultTheme = getDefaultTheme(applicationPath);
    String defaultPageName = defaultTheme.split("/")[1];

    def pages = [];
    if (!currentPagePath || !currentPagePath.contains("/")) {
      currentPagePath = defaultTheme;
    }

    String currentThemeName = currentPagePath.split("/")[0];
    String currentPageName = currentPagePath.split("/")[1];
    ThemeElement currentTheme = themeManager.getThemeByName(currentThemeName);

    if (currentTheme == null) {
      return pages;
    }

    for (PageElement page : ThemeManager.getPagesOf(currentTheme)) {
      String pageName = page.getName();
      String link = String.format("%s/%s", currentThemeName, pageName);
      String className = pageName.equals(currentPageName) ? "selected" : "";
      if (defaultPageName.equals(pageName)) {
        className += " default";
      }
      pages.add(new PageInfo(pageName, link, className));
    }
    return pages;
  }

  public static List<ThemeElement> getThemes(String applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath);
    def themes = [];
    if (!defaultTheme.contains("/")) {
      return themes;
    }

    String defaultThemeName = defaultTheme.split("/")[0];
    String defaultPageName = defaultTheme.split("/")[1];
    String currentThemeName = getCurrentThemeName(applicationPath);

    for (themeName in Manager.getThemeManager().getThemeNames()) {
      String link = String.format("%s/%s", themeName, defaultPageName);
      String className = themeName.equals(currentThemeName) ? "selected" : "";
      if (link.equals(defaultThemeName)) {
        className += " default";
      }
      themes.add(new ThemeInfo(themeName, link, className));
    }
    return themes;
  }

}

