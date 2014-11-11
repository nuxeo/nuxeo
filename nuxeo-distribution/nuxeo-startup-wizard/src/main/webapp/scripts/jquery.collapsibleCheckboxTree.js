/**
 *
 * This version was modified to include constraints management.
 *
 **/
 /**
    Project: Collapsible Checkbox Tree jQuery Plugin
    Version: 1.0.1
  Author: Lewis Jenkins
  Website: http://www.redcarrot.co.uk/2009/11/11/collapsible-checkbox-tree-jquery-plugin/

    License:
        The CheckTree jQuery plugin is currently available for use in all personal or
        commercial projects under both MIT and GPL licenses. This means that you can choose
        the license that best suits your project and use it accordingly.
*/
(function($) {

  $.fn.collapsibleCheckboxTree = function(options) {

    var defaults = {
      checkParents : true, // When checking a box, all parents are checked
      checkChildren : false, // When checking a box, all children are checked
      uncheckChildren : true, // When unchecking a box, all children are unchecked
      initialState : 'default', // Options - 'expand' (fully expanded), 'collapse' (fully collapsed) or default
      displayCB : function() {}
    };

    var options = $.extend(defaults, options);

    $(this).find("input[type='checkbox']:first").attr('disabled','true');
    $(this).find("input[type='checkbox']:first").attr('checked','true');

    this.each(function() {

      var $root = this;

      // Add button
      $(this).before('<div id="buttons" style="display:none"><button id="expand">Expand All</button><button id="collapse">Collapse All</button></div>');

      // Hide all except top level
      $("ul", $(this)).addClass('hide');
      // Check parents if necessary
      if (defaults.checkParents) {
        $("input:checked").parents("li").find("input[type='checkbox']:first").attr('checked', true);
      }
      // Check children if necessary
      if (defaults.checkChildren) {
        $("input:checked").parent("li").find("input[type='checkbox']").attr('checked', true);
      }
      // Show checked and immediate children of checked
      $("li:has(input:checked) > ul", $(this)).removeClass('hide');
      // Add tree links
      $("li", $(this)).prepend('<span>&nbsp;</span>');
      $("li:has(> ul:not(.hide)) > span", $(this)).addClass('expanded').html('-');
      $("li:has(> ul.hide) > span", $(this)).addClass('collapsed').html('+');

      // Checkbox function
      $("input[type='checkbox']", $(this)).click(function(){

        var exclusive = $(this).attr("exclusive");
        if ("true"==exclusive) {
          exclusive=true;
        } else {
          exclusive=false;
        }

        // If checking ...
        if ($(this).is(":checked")) {

          // Show immediate children  of checked
          $("> ul", $(this).parent("li")).removeClass('hide');
          // Update the tree
          $("> span.collapsed", $(this).parent("li")).removeClass("collapsed").addClass("expanded").html('-');

          // Check parents if necessary
          if (defaults.checkParents) {
            $(this).parents("li").find("input[type='checkbox']:first").attr('checked', true);
          }

          // Check children if necessary
          if (defaults.checkChildren) {
            $(this).parent("li").find("input[type='checkbox']").attr('checked', true);
            // Show all children of checked
            $("ul", $(this).parent("li")).removeClass('hide');
            // Update the tree
            $("span.collapsed", $(this).parent("li")).removeClass("collapsed").addClass("expanded").html('-');
          }

        // If unchecking...
        } else {

          // Uncheck children if necessary
          if (defaults.uncheckChildren) {
            $(this).parent("li").find("input[type='checkbox']").attr('checked', false);
            // Hide all children
            //$("ul", $(this).parent("li")).addClass('hide');
            // Update the tree
            $("span.expanded", $(this).parent("li")).removeClass("expanded").addClass("collapsed").html('+');
          }
        }

        // Manage constraints
        var parents = $(this).parents("li").find("input[type='checkbox']:first");
        for (var i=0; i < parents.length; i++) {
          var node = $(parents[i]);
          if (node.attr("exclusive")=="true") {
            if (node.is(":checked")) {
              // disable sibblings
              node.parent("li").siblings().find("input[type='checkbox']:checked").attr('checked',false);
              node.parent("li").siblings().find("input[type='checkbox']").attr("disabled","true");
            } else {
              node.parent("li").siblings().find("input[type='checkbox']").removeAttr("disabled");
            }
          } else {
            if (node.is(":checked")) {
                // disable exclusive sibblings
                var exclusiveNodes = node.parent("li").siblings().find("input[type='checkbox'][exclusive='true']");
                if (exclusiveNodes.length>0) {
                  for (var j=0; j< exclusiveNodes.length; j++) {
                    var eNode = $(exclusiveNodes[j]);
                    eNode.attr('checked',false);
                    eNode.attr("disabled","true");
                    eNode.parent("li").find("input[type='checkbox']").attr('checked',false);
                    eNode.parent("li").find("input[type='checkbox']").attr("disabled","true");
                  }
                }
            }
            else {
              if (node.parent("li").siblings().find("input[type='checkbox'][exclusive='false']:checked").length==0) {
                var exclusiveNodes =node.parent("li").siblings().find("input[type='checkbox'][exclusive='true']");
                exclusiveNodes.removeAttr("disabled");
                for (var j=0; j< exclusiveNodes.length; j++) {
                    var eNode = $(exclusiveNodes[j]);
                    eNode.parent("li").find("input[type='checkbox']").removeAttr('disabled');
                 }
              }
            }
          }
        }

        $($root).find("input[type='checkbox']:first").attr('disabled','true');
        $($root).find("input[type='checkbox']:first").attr('checked','true');

        defaults.displayCB();
      });


      // Tree function
      $("li:has(> ul) span", $(this)).click(function(){

        // If was previously collapsed...
        if ($(this).is(".collapsed")) {

          // ... then expand
          $("> ul", $(this).parent("li")).removeClass('hide');
          // ... and update the html
          $(this).removeClass("collapsed").addClass("expanded").html('-');

        // If was previously expanded...
        } else if ($(this).is(".expanded")) {

          // ... then collapse
          $("> ul", $(this).parent("li")).addClass('hide');
          // and update the html
          $(this).removeClass("expanded").addClass("collapsed").html('+');
        }

      });

      // Button functions

      // Expand all
      $("#expand").click(function () {
        // Show all children
        $("ul", $root).removeClass('hide');
        // and update the html
        $("li:has(> ul) > span", $root).removeClass("collapsed").addClass("expanded").html('-');
        return false;
      });
      // Collapse all
      $("#collapse").click(function () {
        // Hide all children
        $("ul", $root).addClass('hide');
        // and update the html
        $("li:has(> ul) > span", $root).removeClass("expanded").addClass("collapsed").html('+');
        return false;
      });

      switch(defaults.initialState) {
        case 'expand':
          $("#expand").trigger('click');
          break;
        case 'collapse':
          $("#collapse").trigger('click');
          break;
      }
    });

    return this;

  };

})(jQuery);
