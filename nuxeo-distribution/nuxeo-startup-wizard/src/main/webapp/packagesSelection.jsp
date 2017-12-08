<%@ include file="includes/header.jsp" %>
<%@page import="org.nuxeo.wizard.download.Preset"%><h1><fmt:message key="label.packagesSelection" /></h1>

<%
    String baseUrl = ctx.getBaseUrl();
    if (baseUrl == null) {
        baseUrl = "/nuxeo/";
    }
    DownloadablePackageOptions options = PackageDownloader.instance().getPackageOptions();
%>

<script language="javascript">
  var jsonTree;

  function getTree() {
    $.get('<%=baseUrl%>PackageOptionsResource', function (data) {
      jsonTree = data;
      buildList();
    });
  }

  function buildList() {
    var $tree = $('#tree');
    jsonTree.children.forEach(function (child) {
      $tree.append($('<h2>' + child.label + '</h2>'));
      var $pkgContainer = $('<div class="packageContainer" />');
      child.children.forEach(function (pkg) {
        var $pkg = $('<div class="package" />');
        $pkg.click(handlePackageClick);
        var $chk = createChkBox(pkg);
        // Bind a change listener to check exclusive / implies
        $chk.change(handlePackageChanges);
        $chk.click(handleStopPropagation);
        $pkg.append($chk);

        $pkg.append(createChkDescription(pkg));

        $pkgContainer.append($pkg);
      });
      $tree.append($pkgContainer);
    });

    checkImplies();
    recomputePackageClasses();
  }

  /**
   * Recompute `package` div classes to set checked / disabled / ...
   */
  function recomputePackageClasses() {
    $('div.package').each(function (i, pkg) {
      var $pkg = $(pkg);
      var classes = ['package'];
      if ($pkg.find('input[type="checkbox"]:disabled').size() > 0) {
        classes.push('disabled');
      }
      if ($pkg.find('input[type="checkbox"]:checked').size() > 0) {
        classes.push('checked');
      }
      $pkg.attr('class', classes.join(' '));
    });
  }

  function handlePackageClick() {
    var $i = $(this).find('input[type="checkbox"]');
    $i.click();
    $i.change();
  }

  function handleStopPropagation(e) {
    e.stopPropagation();
  }

  function handlePackageChanges() {
    followExclusiveRules($(this));
    checkImplies();
    recomputePackageClasses();
  }

  function followExclusiveRules($chk) {
    var isExclu = $chk.attr('exclusive') === 'true';
    $chk.parents('.packageContainer').find('input[type="checkbox"]').each(function (i, input) {
      var $input = $(input);
      if ($chk.attr('id') === $input.attr('id')) {
        return;
      }

      if (isExclu) {
        // If $chk is exclusive, uncheck all sibling nodes
        $input.attr('checked', false);
      } else if ($input.attr('exclusive') === 'true') {
        // If $chk is not exclusive, disable sibling exclusive nodes
        $input.attr('checked', false);
      }
    });
  }

  function createChkBox(pkg) {
    var checkBox = "<input type=\"checkbox\"";
    checkBox += " name=\"" + pkg.id + "\" ";
    checkBox += " implies=\"" + pkg.implies + "\" ";
    checkBox += " id=\"pkg_" + pkg.id + "\" ";
    checkBox += " pkg=\"" + pkg.package + "\" ";
    checkBox += " exclusive=\"" + pkg.exclusive + "\" ";
    checkBox += " title=\"" + pkg.label + "\" ";
    checkBox += "/>";
    checkBox += "<label for=";
    checkBox += "\"pkg_" + pkg.id + "\">";
    checkBox += pkg.label;
    checkBox += "</label>";

    checkBox = $(checkBox);
    if (pkg.selected == 'true') {
      checkBox.attr('checked', true);
    }

    return checkBox;
  }


  function createChkDescription(pkg) {
    var checkBox = '';
    if (pkg.description && pkg.description !== 'null') {
      checkBox += "<p class=\"packageDescription\">";
      checkBox += pkg.description;
      checkBox += "</p>";
    }
    return $(checkBox);
  }

  function checkImplies() {
    function enableImplies(i, elt) {
      var $elt = $(elt);
      var implies = $elt.attr('implies');
      if (!implies) {
        return;
      }

      implies.split(',').forEach(function (imply) {
        $("input[type='checkbox'][name='" + imply + "']").each(function (j, implied) {
          var $implied = $(implied);
          $implied.attr('checked', true);
          $implied.attr('disabled', true);

          enableImplies(undefined, implied);
        });
      });
    }

    $("input[type='checkbox']:disabled").attr('disabled', false);
    $("input[type='checkbox']:checked").each(enableImplies);
  }

  function usePreset(optionArray) {
    var $boxes = $("input[type='checkbox']");
    $boxes.removeAttr("checked");
    $boxes.removeAttr("disabled");
    for (var i = 0; i < optionArray.length; i++) {
      var filter = "input[type='checkbox'][name='" + optionArray[i] + "']";
      $(filter).attr("checked", "true");
      $(filter).trigger('click');
      $(filter).attr("checked", "true");
    }
  }

  $(document).ready(function () {
    getTree();
  });
</script>

<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.packagesSelection.description"/> <br/>
</span>
<%
    String presetClass = "display:none";
    if ("true".equals(request.getParameter("showPresets"))) {
        presetClass = "";
    }
%>

<%@ include file="includes/feedback.jsp" %>

<span style="<%=presetClass%>" id="hiddenPresets">
  <div class="presetContainer"> <span class="presetLabel"><fmt:message key="label.packagesSelection.presets"/> :</span>
  <%for (Preset preset : options.getPresets()) { %>
    <span class="presetBtn" id="preset_<%=preset.getId()%>"
          onclick="usePreset(<%=preset.getPkgsAsJsonArray()%>)"><%=preset.getLabel()%> </span>
  <%} %>
  </div>
  </span>
<br/>
<div id="tree"></div>

<span class="screenExplanations">
<fmt:message key="label.packagesSelection.explanations"/> <br/>
</span>

<%@ include file="includes/prevnext.jsp" %>
<%@ include file="includes/footer.jsp" %>
