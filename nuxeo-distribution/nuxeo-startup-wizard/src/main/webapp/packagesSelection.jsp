<%@ include file="includes/header.jsp" %>


<%@page import="org.nuxeo.wizard.download.Preset"%><h1><fmt:message key="label.packagesSelection" /></h1>

<%
String baseUrl = ctx.getBaseUrl();
if (baseUrl==null) {
    baseUrl="/nuxeo/";
}
DownloadablePackageOptions options = PackageDownloader.instance().getPackageOptions();

%>
<script type="text/javascript" src="<%=contextPath%>/scripts/jquery.collapsibleCheckboxTree.js"></script>
<script language="javascript">

var jsonTree;

function getTree() {
  $.get('<%=baseUrl%>PackageOptionsResource', function(data) {
     jsonTree = data;
     displayTree();
     displayBlocs();
  });
}

function createCheckBox(pkg) {
  var checkBox = "<input type=\"checkbox\"";
  checkBox += " name=\"" + pkg.id + "\" ";
  checkBox += " pkg=\"" + pkg.package + "\" ";
  checkBox += " exclusive=\"" + pkg.exclusive + "\" ";
  checkBox += ">";
  checkBox += pkg.label;
  checkBox += "</input>";

  checkBox = $(checkBox);
  if (pkg.selected=='true') {
    checkBox.attr('checked',true);
  }
  return checkBox;
}

function addNode(container, pkg, level) {
  var li = $("<li></li>");
  var chk = createCheckBox(pkg);
  li.append(chk);
  var nbChildren = pkg.children.length;
  if (nbChildren>0) {
    var ul = $("<ul></ul>");
    li.append(ul);
    for (var i=0; i <nbChildren; i++) {
        addNode(ul, pkg.children[i], level+1);
    }
  }
  container.append(li);
}

function displayTree() {
    $('#tree').html("");
    var ul = $("<ul id=\"treeul\"></ul>");
    addNode(ul,jsonTree, 0);
    $('#tree').append(ul);
    ul.collapsibleCheckboxTree({
      checkParents : true, // When checking a box, all parents are checked (Default: true)
      checkChildren : false, // When checking a box, all children are checked (Default: false)
      uncheckChildren : true, // When unchecking a box, all children are unchecked (Default: true)
      initialState : 'expand', // Options - 'expand' (fully expanded), 'collapse' (fully collapsed) or default
      displayCB : displayBlocs
    });

}

function drawRow(container) {
  var div = $("<div class=\"nxprow\"></div>");
  container.prepend(div);
  return div;
}

function drawBloc(container, idx, node2Display, parent, nbSiblings) {
    var selected = node2Display.selected;
    var node = node2Display.node;
    if (node == null) {
        return;
    }
    var span = $("<div class=\"nxpblock\">" + node.shortlabel + "</div>");
    span.attr('pkg', node.package);
    if (node2Display.root) {
      span.attr('root', true);
    }
    var width = parent.width()/nbSiblings - 4;
    if (node2Display.parent) {
      width = node2Display.parent.width / nbSiblings -4 ;
    }
    node.width=width;
    span.css("width", width +"px");
    span.css("float","left")
    span.css("background-color",node.color);
    span.css("color",node.textcolor);
    if (!selected) {
      span.toggleClass("unselectedBloc");
    }
    container.append(span);
    return span;
}

function displayRow(container, parent, level, childrenNodes2Display) {

  var row = drawRow(container)
  for (var i = 0; i < childrenNodes2Display.length; i++) {
    drawBloc(row, i, childrenNodes2Display[i], parent,childrenNodes2Display.length);
  }
  container.append($("<div style=\"clear:both;\"></div>"));
  return row;
}

function displayNodes(container, parent,  level, nodes, ids) {
  var childrenNodes2Display = [];
  var allChildrenNodes2Display = [];
  var childrenNodes = [];
  var oneSelected=false;
  for (var j = 0; j < nodes.length; j++) {
    var node = nodes[j];
    if (node==null) {
        continue;
    }
    for (var i = 0; i < node.children.length; i++) {
      var child = node.children[i];
      var child2Display={'node' : child, 'parent' : node};
      child2Display.selected=false;
      if ($.inArray(child.id, ids)>=0) {
        oneSelected=true;
        child2Display.selected=true;
        if (child.exclusive=='true') {
          childrenNodes2Display = [child2Display];
          childrenNodes = [ child ];
          break;
        } else {
          childrenNodes2Display.push(child2Display);
          childrenNodes.push(child);
        }
      } else {
        if (child.exclusive!='true') {
          childrenNodes2Display.push(child2Display);
        }
      }
      allChildrenNodes2Display.push(child2Display);
    }
  }
  if (childrenNodes2Display.length>0) {
    if (oneSelected) {
      parent = displayRow(container, parent,level, childrenNodes2Display);
      displayNodes(container, parent, level+1, childrenNodes, ids);
    } else {
      parent = displayRow(container, parent,level, allChildrenNodes2Display);
    }
  }
}

function displayBlocs() {
  var checkBoxes = $("input[type='checkbox']:checked");
  var ids = [];
  for (var i = 0; i < checkBoxes.length; i++) {
    ids.push($(checkBoxes[i]).attr('name'));
  }
  // draw root
  var container = $("#blocs");
  container.html("");
  var parent = $("<div class=\"nxprow\" style=\"height:2px\"></div>");
  container.append(parent);
  var row = drawRow(container);
  parent = drawBloc(row,0,{ 'node':jsonTree, 'selected' : true, 'root' : true}, parent,1);
  displayNodes(container, parent, 0, [jsonTree], ids);
  // bind click
  $(".nxpblock").click(function(event) {
    if ($(event.target).attr("root")) {
      // can not deselect the root !!!
      return;
    }
    var targetPkg = $(event.target).attr("pkg");
    var filter = "input[type='checkbox'][name='" + targetPkg + "']";
    if ( $(filter).attr("checked")==true) {
        $(filter).removeAttr("checked");
        $(filter).trigger('click');
        $(filter).removeAttr("checked");
    }
    else {
        $(filter).attr("checked","true");
        $(filter).trigger('click');
        $(filter).attr("checked","true");
    }
  });

}

function usePreset(optionArray) {
  $("input[type='checkbox']").removeAttr("checked");
  $("input[type='checkbox']").removeAttr("disabled");
  for (var i = 0; i <optionArray.length; i++) {
    var filter = "input[type='checkbox'][name='" + optionArray[i] + "']";
    $(filter).attr("checked","true");
    $(filter).trigger('click');
    $(filter).attr("checked","true");
  }
}

$(document).ready(function(){
     getTree();
   });
</script>

<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.packagesSelection.description" /> <br/>
</span>
<%
String presetClass = "display:none";
if ("true".equals(request.getParameter("showPresets"))) {
    presetClass = "";
}
%>

<%@ include file="includes/feedback.jsp" %>

  <span style="<%=presetClass%>" id="hiddenPresets">
  <div class="presetContainer"> <span class="presetLabel"><fmt:message key="label.packagesSelection.presets" /> :</span>
  <%for (Preset preset : options.getPresets()) { %>
    <span class="presetBtn" id="preset_<%=preset.getId()%>" onclick="usePreset(<%=preset.getPkgsAsJsonArray()%>)"><%=preset.getLabel()%> </span>
  <%} %>
  </div>
  </span>
  <br/>
  <div id="tree"></div>
  <div class="blocContainer">
     <div id="blocs"></div>
  </div>
  <div style="clear:both"></div>

<span class="screenExplanations">
<fmt:message key="label.packagesSelection.explanations" /> <br/>
</span>

  <%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>