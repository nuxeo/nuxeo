package editor

import java.io.*
import javax.ws.rs.*
import javax.ws.rs.core.*
import javax.ws.rs.core.Response.ResponseBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern
import net.sf.json.JSONObject
import org.nuxeo.ecm.core.rest.*
import org.nuxeo.ecm.webengine.forms.*
import org.nuxeo.ecm.webengine.model.*
import org.nuxeo.ecm.webengine.model.impl.*
import org.nuxeo.ecm.webengine.model.exceptions.*
import org.nuxeo.ecm.webengine.*
import org.nuxeo.theme.*
import org.nuxeo.theme.elements.*
import org.nuxeo.theme.formats.*
import org.nuxeo.theme.formats.widgets.*
import org.nuxeo.theme.formats.styles.*
import org.nuxeo.theme.formats.layouts.*
import org.nuxeo.theme.events.*
import org.nuxeo.theme.fragments.*
import org.nuxeo.theme.presets.*
import org.nuxeo.theme.properties.*
import org.nuxeo.theme.templates.*
import org.nuxeo.theme.themes.*
import org.nuxeo.theme.types.*
import org.nuxeo.theme.perspectives.*
import org.nuxeo.theme.uids.*
import org.nuxeo.theme.views.*
import org.nuxeo.theme.editor.*

@WebModule(name="nxthemes-editor", guard="user=Administrator")

@Path("/nxthemes-editor")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {
    
    @GET
    @Path("perspectiveSelector")
    public Object renderPerspectiveSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
      return getTemplate("perspectiveSelector.ftl").arg("perspectives", getPerspectives())
    }

    @GET
    @Path("themeSelector")
    public Object renderThemeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
      return getTemplate("themeSelector.ftl").arg(
              "current_theme_name", getCurrentThemeName(path)).arg(
              "themes", getThemes(path)).arg(
              "pages", getPages(path))
    }

  @GET
  @Path("canvasModeSelector")
  public Object renderCanvasModeSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("canvasModeSelector.ftl")
  }

  @GET
  @Path("backToCanvas")
  public Object renderBackToCanvas(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("backToCanvas.ftl")
  }

  @GET
  @Path("presetManager")
  public Object renderPresetManager(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("presetManager.ftl").arg(
            "preset_groups", getPresetGroups()).arg(
            "theme_names", Manager.getThemeManager().getThemeNames())
  }
  
  @GET
  @Path("themeManager")
  public Object renderThemeManager(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("themeManager.ftl").arg("themes", getThemeDescriptors())
  }

  @GET
  @Path("fragmentFactory")
  public Object renderFragmentFactory(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("fragmentFactory.ftl").arg("fragments", getFragments(path)).arg("selected_element_id", getSelectedElementId())
  }

  @GET
  @Path("elementEditor")
  public Object renderElementEditor(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementEditor.ftl").arg("selected_element", getSelectedElement())
  }

  @GET
  @Path("elementDescription")
  public Object renderElementDescription(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementDescription.ftl").arg("selected_element", getSelectedElement())
  }

  @GET
  @Path("elementPadding")
  public Object renderElementPadding(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementPadding.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "padding_of_selected_element", getPaddingOfSelectedElement())
  }

  @GET
  @Path("elementProperties")
  public Object renderElementProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementProperties.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "element_properties", getSelectedElementProperties())
  }

  @GET
  @Path("elementStyle")
  public Object renderElementStyle(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementStyle.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "style_of_selected_element", getStyleOfSelectedElement()).arg(
            "current_theme_name", getCurrentThemeName(path)).arg(
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "inherited_style_name_of_selected_element", getInheritedStyleNameOfSelectedElement()).arg(
            "named_styles", getNamedStyles(path))
  }

  @GET
  @Path("elementWidget")
  public Object renderElementWidget(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementWidget.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(
            "view_names_for_selected_element", getViewNamesForSelectedElement(path))
  }

  @GET
  @Path("elementVisibility")
  public Object renderElementVisibility(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("elementVisibility.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "perspectives_of_selected_element", getPerspectivesOfSelectedElement()).arg(        
            "is_selected_element_always_visible", isSelectedElementAlwaysVisible()).arg(
            "perspectives", getPerspectives())
  }

  @GET
  @Path("stylePicker")
  public Object renderStylePicker(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("stylePicker.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "current_theme_name", getCurrentThemeName(path)).arg(                          
            "selected_preset_group", getSelectedPresetGroup()).arg(                            
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup(path))
  }
  
  @GET
  @Path("areaStyleChooser")
  public Object renderAreaStyleChooser(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("areaStyleChooser.ftl").arg(
            "style_category", getSelectedStyleCategory()).arg(
            "current_theme_name", getCurrentThemeName(path)).arg(                              
            "preset_groups", getPresetGroupsForSelectedCategory()).arg(
            "presets_for_selected_group", getPresetsForSelectedGroup(path)).arg(
            "selected_preset_group", getSelectedPresetGroup())
  }

  @GET
  @Path("styleProperties")
  public Object renderStyleProperties(@QueryParam("org.nuxeo.theme.application.path") String path) {
    return getTemplate("styleProperties.ftl").arg(
            "selected_element", getSelectedElement()).arg(
            "style_edit_mode", getStyleEditMode()).arg(                    
            "style_layers_of_selected_element", getStyleLayersOfSelectedElement()).arg(
            "style_selectors", getStyleSelectorsForSelectedElement()).arg(
            "rendered_style_properties", getRenderedStylePropertiesForSelectedElement()).arg(
            "selected_style_selector", getSelectedStyleSelector()).arg(
            "style_properties", getStylePropertiesForSelectedElement()).arg(
            "style_categories", getStyleCategories()).arg(
            "selected_view_name", getViewNameOfSelectedElement()).arg(                    
            "element_style_properties", getElementStyleProperties())
  }

  @GET
  @Path("render_view_icon")
  public Response renderViewIcon(@QueryParam("name") String viewTypeName) {
      byte[] content = org.nuxeo.theme.editor.Editor.getViewIconContent(viewTypeName)
      ResponseBuilder builder = Response.ok(content)
      // builder.type(???)
      return builder.build();
   }

  @GET
  @Path("render_css_preview")
  public String renderCssPreview() {
      String selectedElementId = getSelectedElementId()
      Style selectedStyleLayer = getSelectedStyleLayer()
      String selectedViewName = getViewNameOfSelectedElement()
      Element selectedElement = getSelectedElement()
      return Editor.renderCssPreview(selectedElement, selectedStyleLayer, selectedViewName)
  }
  
  
  @GET
  @Path("xml_export")
  public Response xmlExport(@QueryParam("theme") String themeName, @QueryParam("download") Integer download, @QueryParam("indent") Integer indent) {
      if (themeName == null) {
          return
      }
      ThemeElement theme = Manager.getThemeManager().getThemeByName(themeName)
      if (theme == null) {
          return;
      }

      ThemeSerializer serializer = new ThemeSerializer();
      if (indent == null) {
          indent = 0
      }
      
      String xml = serializer.serializeToXml(theme, indent);
      if (xml == null) {
          return
      }

      ResponseBuilder builder = Response.ok(xml)
      if (download != null) {
          builder.header("Content-disposition", String.format(
                  "attachment; filename=theme-%s.xml", theme.getName()))
      }
      builder.type("text/xml")
      return builder.build()
  }  
  
  @POST
  @Path("clear_selections")
  public void clearSelections() {
    SessionManager.setElementId(null);
    SessionManager.setStyleEditMode(null);
    SessionManager.setStyleLayerId(null);
    SessionManager.setStyleSelector(null);
    SessionManager.setStylePropertyCategory(null);
    SessionManager.setStyleCategory(null);
    SessionManager.setPresetGroup(null);
    SessionManager.setClipboardElementId(null);
  }
  
  @POST
  @Path("select_element")
  public void selectElement() {
    String id = ctx.getForm().getString("id")
    SessionManager.setElementId(id)
  }
  
  @POST
  @Path("add_page")
  public String addPage() {
      String pagePath = ctx.getForm().getString("path")
	  try {
	      return Editor.addPage(pagePath)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }      
  }
  
  @POST
  @Path("add_theme")
  public String addTheme() {
      String name = ctx.getForm().getString("name")
	  try {
	      return Editor.addTheme(name)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }
  
  @POST
  @Path("align_element")
  public void alignElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String position = form.getString("position")
      Element element = ThemeManager.getElementById(id)
      Editor.alignElement(element, position)
  }
  
  @POST
  @Path("assign_style_property")
  public void assignStyleProperty() {
      FormData form = ctx.getForm()
      String id = form.getString("element_id")
      String propertyName = form.getString("property")
      String value = form.getString("value")          
      Element element = ThemeManager.getElementById(id)
      Editor.assignStyleProperty(element, propertyName, value)
  }
  
  @POST
  @Path("copy_element")
  public void copyElement() {
      String id = ctx.getForm().getString("id")
      SessionManager.setClipboardElementId(id)
  }
  
  @POST
  @Path("create_named_style")
  public void createNamedStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String themeName = form.getString("theme_name")
      String styleName = form.getString("style_name")
      Element element = ThemeManager.getElementById(id)
      Editor.createNamedStyle(element, styleName, themeName)
  }
  
  @POST
  @Path("create_style")
  public void createStyle() {
      Element element = getSelectedElement()
      Editor.createStyle(element)
  }
  
  @POST
  @Path("delete_element")
  public void deleteElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")      
      Element element = ThemeManager.getElementById(id)
	  try {
	      Editor.deleteElement(element)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }            
  }
  
  @POST
  @Path("delete_named_style")
  public void deleteNamedStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")        
      String themeName = form.getString("theme_name")
      String styleName = form.getString("style_name")
      Element element = ThemeManager.getElementById(id)
	  try {
	      Editor.deleteNamedStyle(element, styleName, themeName)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }           
  }
  
  @POST
  @Path("duplicate_element")
  public String duplicateElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")        
      Element element = ThemeManager.getElementById(id)
	  try {
	      return Editor.duplicateElement(element)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }
  
  @POST
  @Path("expire_themes")
  public void expireThemes() {
      Editor.expireThemes()
  }
  
  @POST
  @Path("insert_fragment")
  public void insertFragment() {
      FormData form = ctx.getForm()
      String destId = form.getString("dest_id")    
      String typeName = form.getString("type_name")             
      Element destElement = ThemeManager.getElementById(destId)
      Editor.insertFragment(destElement, typeName)
  }
  
  @POST
  @Path("insert_section_after")
  public void insertSectionAfter() {
      FormData form = ctx.getForm()
      String id = form.getString("id")           
      Element element = ThemeManager.getElementById(id)
      Editor.insertSectionAfter(element)
  }

  @POST
  @Path("add_preset")
  public String addPreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String presetName = form.getString("preset_name")      
      String category = form.getString("category")        
      return Editor.addPreset(themeName, presetName, category);
  }
  
  @POST
  @Path("edit_preset")
  public void editPreset() {
      FormData form = ctx.getForm()
      String themeName = form.getString("theme_name")
      String presetName = form.getString("preset_name")      
      String category = form.getString("category")     
      String value = form.getString("value")           
      Editor.editPreset(themeName, presetName, value);
  }
  
  @POST
  @Path("make_element_use_named_style")
  public void makeElementUseNamedStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String styleName = form.getString("style_name")
      String themeName = form.getString("theme_name")
      Element element = ThemeManager.getElementById(id)
      Editor.makeElementUseNamedStyle(element, styleName, themeName)
  }
  
  @POST
  @Path("move_element")
  public void moveElement() {
      FormData form = ctx.getForm()
      String srcId = form.getString("src_id")
      String destId = form.getString("dest_id")
      def order = form.getString("order") as Integer
      Element srcElement = ThemeManager.getElementById(srcId)
      Element destElement = ThemeManager.getElementById(destId)
      Editor.moveElement(srcElement, destElement, order)
  }
  
  @POST
  @Path("paste_element")
  public String pasteElement() {
      FormData form = ctx.getForm()
      String destId = form.getString("dest_id")      
      String id = getClipboardElement()
      if (id == null) {
          throw new ThemeEditorException("Nothing to paste")
      }
      Element element = ThemeManager.getElementById(id)
	  try {
	      return Editor.pasteElement(element, destId)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }      
  }
  
  @POST
  @Path("repair_theme")
  public void repairTheme() {
	  FormData form = ctx.getForm()
      String themeName = form.getString("name")
	  try {
	      Editor.repairTheme(themeName)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }
  
  @POST
  @Path("save_theme")
  public void saveTheme() {
      FormData form = ctx.getForm()
      String src = form.getString("src")
      def indent = form.getString("indent") as Integer
      try {
          Editor.saveTheme(src, indent)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }

  @POST
  @Path("load_theme")
  public void loadTheme() {
      FormData form = ctx.getForm()
      String src = form.getString("src")      
      try {
          Editor.loadTheme(src)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }
  }
  
  @POST
  @Path("select_preset_group")
  public void selectPresetGroup() {
      FormData form = ctx.getForm()
      String group = form.getString("group")        
      SessionManager.setPresetGroup(group)
  }
  
  @POST
  @Path("select_style_category")
  public void selectStyleCategory() {
      FormData form = ctx.getForm()
      String category = form.getString("category")      
      SessionManager.setStyleCategory(category)
  }
  
  @POST
  @Path("select_style_edit_mode")
  public void selectStyleEditMode() {
      FormData form = ctx.getForm()
      String mode = form.getString("mode")        
      SessionManager.setStyleEditMode(mode)
  }
  
  @POST
  @Path("select_style_layer")
  public void selectStyleLayer() {
      FormData form = ctx.getForm()
      String uid = form.getString("uid")      
      Style layer = (Style) ThemeManager.getFormatById(uid)
      if (layer != null) {
          SessionManager.setStyleLayerId(uid)
      }
  }
  
  @POST
  @Path("select_style_property_category")
  public void selectStylePropertyCategory() {
      FormData form = ctx.getForm()
      String category = form.getString("category")           
      SessionManager.setStylePropertyCategory(category)
  }
  
  @POST
  @Path("select_style_selector")
  public void selectStyleSelector() {
      FormData form = ctx.getForm()
      String selector = form.getString("selector")      
      SessionManager.setStyleSelector(selector)      
  }
  
  @POST
  @Path("update_element_description")
  public void updateElementDescription() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String description = form.getString("description")
      Element element = ThemeManager.getElementById(id)
      Editor.updateElementDescription(element, description)
  }
  
  @POST
  @Path("update_element_properties")
  public void updateElementProperties() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String property_map = form.getString("property_map")
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = ThemeManager.getElementById(id)
	  try {
	      Editor.updateElementProperties(element, propertyMap)
      } catch (ThemeException e) {
          throw new ThemeEditorException(e.getMessage(), e)
      }      
  }

  @POST
  @Path("update_element_width")
  public void updateElementWidth() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String width = form.getString("width")         
      Format layout = ThemeManager.getFormatById(id)
      Editor.updateElementWidth(layout, width)
  }

  @POST
  @Path("update_element_style_css")
  public void updateElementStyleCss() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String viewName = form.getString("view_name")
      String cssSource = form.getString("css_source")      
      Element element = ThemeManager.getElementById(id)
      Style selectedStyleLayer = getSelectedStyleLayer()
      Editor.updateElementStyleCss(element, selectedStyleLayer, viewName, cssSource) 
  }

  @POST
  @Path("split_element")
  public void splitElement() {
      FormData form = ctx.getForm()
      String id = form.getString("id")      
      Element element = ThemeManager.getElementById(id)
      Editor.splitElement(element)
   }

  @POST
  @Path("update_element_style")
  public void updateElementStyle() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String path = form.getString("path")           
      String viewName = form.getString("view_name")      
      String property_map = form.getString("property_map")      
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = ThemeManager.getElementById(id)
      Style currentStyleLayer = getSelectedStyleLayer()
      Editor.updateElementStyle(element, currentStyleLayer, path, viewName, propertyMap)
  }

  @POST
  @Path("update_element_visibility")
  public String updateElementVisibility() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      List<String> perspectives = form.getList("perspectives")
      boolean alwaysVisible = Boolean.valueOf(form.getString("always_visible"))
      Element element = ThemeManager.getElementById(id)
      Editor.updateElementVisibility(element, perspectives, alwaysVisible)
  }
  
  @POST
  @Path("update_element_layout")
  public void updateElementPadding() {
      FormData form = ctx.getForm()
      String property_map = form.getString("property_map")      
      Map propertyMap = JSONObject.fromObject(property_map)
      Element element = getSelectedElement()
      Editor.updateElementLayout(element, propertyMap)
  }
  
  @POST
  @Path("update_element_widget")
  public void updateElementWidget() {
      FormData form = ctx.getForm()
      String id = form.getString("id")
      String viewName = form.getString("view_name")
      Element element = ThemeManager.getElementById(id)
      Editor.updateElementWidget(element, viewName)
  }

  
  /* API */
   
  public static List<ThemeDescriptor> getThemeDescriptors() {
      return ThemeManager.getThemeDescriptors()
  }
  
  public static List<String> getNamedStyles(String applicationPath) {
      String currentThemeName = getCurrentThemeName(applicationPath)
      def styles = []
      List<Identifiable> namedStyles = Manager.getThemeManager().getNamedObjects(currentThemeName, "style")
      if (namedStyles) {
          styles.add("")
          for (namedStyle in namedStyles) {
              styles.add(namedStyle.getName())
           }
      }
      return styles
  }
  
  public static List<FragmentType> getFragments(applicationPath) {
      def fragments = []
      String templateEngine = getTemplateEngine(applicationPath)
      for (f in Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
          FragmentType fragmentType = (FragmentType) f
          FragmentInfo fragmentInfo = new FragmentInfo(fragmentType)
          for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
              String viewTemplateEngine = viewType.getTemplateEngine()
              if (!"*".equals(viewType.getViewName()) && templateEngine.equals(viewTemplateEngine)) {
                  fragmentInfo.addView(viewType)
              }
          }
          if (fragmentInfo.size() > 0) {
               fragments.add(fragmentInfo)
          }
      }
      return fragments
  }
  
  public static String getSelectedElementId() {
      return SessionManager.getElementId()
  }
  
  public static Element getSelectedElement() {
    String id = getSelectedElementId()
    if (id == null) {
      return null
    }
    return ThemeManager.getElementById(id)
  } 
  
  public static String getClipboardElement() {
      return SessionManager.getClipboardElementId()
  }
  
  public static List<StyleLayer> getStyleLayersOfSelectedElement() {
      Style style = getStyleOfSelectedElement()
      if (style == null) {
        return []
      }
      Style selectedStyleLayer = getSelectedStyleLayer()
      def layers = []
      layers.add(new StyleLayer("This style", style.getUid(), style == selectedStyleLayer || selectedStyleLayer == null))
      for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
          layers.add(1, new StyleLayer(ancestor.getName(), ancestor.getUid(), ancestor == selectedStyleLayer))
      }
      return layers
  }
  
  public static boolean isSelectedElementAlwaysVisible() {
      Element selectedElement = getSelectedElement()
      return Manager.getPerspectiveManager().isAlwaysVisible(selectedElement)
  }
  
  public static List<PerspectiveType> getPerspectives() {
      return PerspectiveManager.listPerspectives()
  }
  
  public static List<PerspectiveType> getPerspectivesOfSelectedElement() {
      Element selectedElement = getSelectedElement()
      def perspectives = []
      for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(selectedElement)) {
          perspectives.add(perspectiveType.name)
      }
      return perspectives
  }
  
  public static String getStyleEditMode() {
      return SessionManager.getStyleEditMode()
  }
  
  public static List<String> getStyleSelectorsForSelectedElement() {
      Element element = getSelectedElement()
      String viewName = getViewNameOfSelectedElement()
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      def selectors = []
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      if (style != null) {
          if (style.getName() != null) {
              viewName = "*"
          }
          Set<String> paths = style.getPathsForView(viewName)
          String current = getSelectedStyleSelector()
          if (current != null && !paths.contains(current)) {
              selectors.add(current)
          }
          for (path in paths) {
              selectors.add(path)
          }
      }
      return selectors
  }
  
  public static List<StyleFieldProperty> getElementStyleProperties() {
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      List<StyleFieldProperty> fieldProperties = []
      if (style == null) {
          return fieldProperties
      }
      String path = getSelectedStyleSelector()
      if (path == null) {
          return fieldProperties
      }
      String viewName = getViewNameOfSelectedElement()
      if (style.getName() != null) {
          viewName = "*"
      }
      Properties properties = style.getPropertiesFor(viewName, path)
      String selectedCategory = getSelectedStylePropertyCategory()

      Properties cssProperties = org.nuxeo.theme.html.Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
          String type = cssProperties.getProperty(name)
          if (!selectedCategory.equals("*")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type)
              if (!categoryMatcher.find()) {
                  continue
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type))
      }
      return fieldProperties    
  }
  
  public static List getStylePropertiesForSelectedElement() {
      String viewName = getViewNameOfSelectedElement()
      Style style = getStyleOfSelectedElement()
      Style selectedStyleLayer = getSelectedStyleLayer()
      if (selectedStyleLayer != null) {
          style = selectedStyleLayer
      }
      def fieldProperties = []
      if (style == null) {
          return fieldProperties
      }
      String path = getSelectedStyleSelector()
      if (path == null) {
          return fieldProperties
      }
      if (style.getName() != null) {
          viewName = "*"
      }
      Properties properties = style.getPropertiesFor(viewName, path)
      String selectedCategory = getSelectedStylePropertyCategory()
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Properties cssProperties = org.nuxeo.theme.html.Utils.getCssProperties()
      Enumeration<?> propertyNames = cssProperties.propertyNames()
      while (propertyNames.hasMoreElements()) {
          String name = (String) propertyNames.nextElement()
          String value = properties == null ? "" : properties.getProperty(name, "")
                  String type = cssProperties.getProperty(name)
          if (!selectedCategory.equals("")) {
              Matcher categoryMatcher = cssCategoryPattern.matcher(type)
              if (!categoryMatcher.find()) {
                  continue
              }
              if (!categoryMatcher.group(1).equals(selectedCategory)) {
                  continue
              }
          }
          fieldProperties.add(new StyleFieldProperty(name, value, type))
      }
      return fieldProperties
  }
  
  public static String getSelectedStylePropertyCategory() {
      String category = SessionManager.getStylePropertyCategory()
      if (!category) {
          category = '*'
      }
      return category
  }
  
  public static List<StyleCategory> getStyleCategories() {
      String selectedStyleCategory = getSelectedStylePropertyCategory()
      Pattern cssCategoryPattern = Pattern.compile("<(.*?)>")
      Map<String, StyleCategory> categories = new LinkedHashMap<String, StyleCategory>()
      Enumeration<?> elements = org.nuxeo.theme.html.Utils.getCssProperties().elements()
      categories.put("", new StyleCategory("*", "all", selectedStyleCategory.equals("*")))
      while (elements.hasMoreElements()) {
          String element = (String) elements.nextElement()
          Matcher categoryMatcher = cssCategoryPattern.matcher(element)
          if (categoryMatcher.find()) {
              String value = categoryMatcher.group(1)
              boolean selected = value.equals(selectedStyleCategory)
              categories.put(value, new StyleCategory(value, value, selected))
          }
      }
      return new ArrayList<StyleCategory>(categories.values())
  }
  
  public static String getInheritedStyleNameOfSelectedElement() {
      Style style = getStyleOfSelectedElement()
      Style ancestor = (Style) ThemeManager.getAncestorFormatOf(style)
      if (ancestor != null) {
          return ancestor.getName()
      }
      return ""
  }

  public static String getSelectedStyleSelector() {
      return SessionManager.getStyleSelector()
  }
  
  public static Style getSelectedStyleLayer() {
      String selectedStyleLayerId = getSelectedStyleLayerId()
      if (selectedStyleLayerId == null) {
        return null
      }
      return (Style) ThemeManager.getFormatById(selectedStyleLayerId)
  }
  
  public static String getSelectedStyleLayerId() {
      return SessionManager.getStyleLayerId()
  } 
  
  public static Style getStyleOfSelectedElement() {
      Element element = getSelectedElement()
      if (element == null) {
        return null
      }
      FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
      return (Style) ElementFormatter.getFormatByType(element, styleType)
  }
  
  public static PaddingInfo getPaddingOfSelectedElement() {
      Element element = getSelectedElement()
      String top = ""
      String bottom = ""
      String left = ""
      String right = ""
      if (element != null) {
          Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout")
          top = layout.getProperty("padding-top")
          bottom = layout.getProperty("padding-bottom")
          left = layout.getProperty("padding-left")
          right = layout.getProperty("padding-right")
      }
      return new PaddingInfo(top, bottom, left, right)
  }

  public static String getRenderedStylePropertiesForSelectedElement() {
      Style style = getStyleOfSelectedElement()
      Style currentStyleLayer = getSelectedStyleLayer()
      if (currentStyleLayer != null) {
          style = currentStyleLayer
      }
      if (style == null) {
          return ""
      }
      def viewNames = []
      String viewName = getViewNameOfSelectedElement()
      if (style.getName() != null) {
          viewName = "*"
      }
      viewNames.add(viewName)
      boolean RESOLVE_PRESETS = false
      boolean IGNORE_VIEW_NAME = true
      boolean IGNORE_CLASSNAME = true
      boolean INDENT = true
      return org.nuxeo.theme.html.Utils.styleToCss(style, viewNames, RESOLVE_PRESETS, IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT)
  }
  
    
  public static Widget getWidgetOfSelectedElement() {
    Element element = getSelectedElement()
    if (element == null) {
      return null
    }
    FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget")
    return (Widget) ElementFormatter.getFormatByType(element, widgetType)
  }
  
  public static String getViewNameOfSelectedElement() {
    Widget widget = getWidgetOfSelectedElement()
    if (widget == null) {
      return ""
    }
    return widget.getName()
  }

  public static List<String> getViewNamesForSelectedElement(applicationPath) {
      Element selectedElement = getSelectedElement()
      String templateEngine = getTemplateEngine(applicationPath)
      def viewNames = []
      if (selectedElement == null) {
          return viewNames
      }
      if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
          return viewNames
      }
      FragmentType fragmentType = ((Fragment) selectedElement).getFragmentType()
      for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
          String viewName = viewType.getViewName()
          String viewTemplateEngine = viewType.getTemplateEngine()
          if (!"*".equals(viewName) && templateEngine.equals(viewTemplateEngine)) {
              viewNames.add(viewName)
          }
      }
      return viewNames
  }
  
  public static List<FieldProperty> getSelectedElementProperties() {
      Element selectedElement = getSelectedElement()
      return org.nuxeo.theme.editor.Utils.getPropertiesOf(selectedElement) 
  }
  
  /* Presets */
  
  public static List<String> getPresetGroupsForSelectedCategory() {
      return getPresetGroups(getSelectedStyleCategory())
  }
  
  public static List<String> getPresetGroups(String category) {
      def groups = []
      def groupNames = []
      for (PresetType preset : PresetManager.getGlobalPresets(null, category)) {
          String group = preset.getGroup()
          if (!groupNames.contains(group)) {
              groups.add(group)
          }
          groupNames.add(group)
      }
      return groups
  }
    
  public static List<PresetInfo> getGlobalPresets(String group) {
      def presets = []
      for (preset in  PresetManager.getGlobalPresets(group, null)) {
          presets.add(new PresetInfo(preset))
      }
      return presets
  }
  
  public static List<PresetInfo> getCustomPresets(String themeName) {
      def presets = []
      for (preset in PresetManager.getCustomPresets(themeName, null)) {
          presets.add(new PresetInfo(preset))
      }
      return presets
  }

  public static List<PresetInfo> getPresetsForSelectedGroup(applicationPath) {
      String category = getSelectedStyleCategory()
      String group = getSelectedPresetGroup()
      String themeName = getCurrentThemeName(applicationPath)
      def presets = []
      def presetTypes = group ? PresetManager.getGlobalPresets(group, category) : PresetManager.getCustomPresets(themeName, category)
      for (preset in presetTypes) {
          presets.add(new PresetInfo(preset))
      }
      return presets
  }
  
  /* Session */
  
  public static String getSelectedPresetGroup() {
      String category = SessionManager.getPresetGroup()
      return category
  }
  
  public static String getSelectedStyleCategory() {
        String category = SessionManager.getStyleCategory()
        if (!category) {
            category = "page"
        }
        return category
  }
  
  public static String getTemplateEngine(applicationPath) {
      return ThemeManager.getTemplateEngineName(applicationPath)
  }
  
  public static String getDefaultTheme(applicationPath) {
    return ThemeManager.getDefaultTheme(applicationPath)
  }

  public static String getCurrentThemeName(applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath)
    def ctx = WebEngine.getActiveContext()
    String currentPagePath = ctx.getCookie("nxthemes.theme")
    if (currentPagePath == null) {
      currentPagePath = defaultTheme
    }
    return currentPagePath.split("/")[0]
  }

  public static List<PageElement> getPages(applicationPath) {
    ThemeManager themeManager = Manager.getThemeManager()
    def ctx = WebEngine.getActiveContext()
    String currentPagePath = ctx.getCookie("nxthemes.theme")
    String defaultTheme = getDefaultTheme(applicationPath)
    String defaultPageName = defaultTheme.split("/")[1]

    def pages = []
    if (!currentPagePath || !currentPagePath.contains("/")) {
      currentPagePath = defaultTheme
    }

    String currentThemeName = currentPagePath.split("/")[0]
    String currentPageName = currentPagePath.split("/")[1]
    ThemeElement currentTheme = themeManager.getThemeByName(currentThemeName)

    if (currentTheme == null) {
      return pages
    }

    for (PageElement page : ThemeManager.getPagesOf(currentTheme)) {
      String pageName = page.getName()
      String link = String.format("%s/%s", currentThemeName, pageName)
      String className = pageName.equals(currentPageName) ? "selected" : ""
      if (defaultPageName.equals(pageName)) {
        className += " default"
      }
      pages.add(new PageInfo(pageName, link, className))
    }
    return pages
  }

  public static List<ThemeElement> getThemes(applicationPath) {
    String defaultTheme = getDefaultTheme(applicationPath)
    def themes = []
    if (!defaultTheme.contains("/")) {
      return themes
    }
    String defaultThemeName = defaultTheme.split("/")[0]
    String defaultPageName = defaultTheme.split("/")[1]
    String currentThemeName = getCurrentThemeName(applicationPath)
    for (themeName in Manager.getThemeManager().getThemeNames()) {
      String link = String.format("%s/%s", themeName, defaultPageName)
      String className = themeName.equals(currentThemeName) ? "selected" : ""
      if (link.equals(defaultThemeName)) {
        className += " default"
      }
      themes.add(new ThemeInfo(themeName, link, className))
    }
    return themes
  }

  // handle errors
  public Response handleError(WebApplicationException e) {
      if (e instanceof ThemeEditorException) {
          return Response.status(500).entity(e.getMessage()).build();
      } else {
          return super.handleError(e)
      }
  }
  
}

