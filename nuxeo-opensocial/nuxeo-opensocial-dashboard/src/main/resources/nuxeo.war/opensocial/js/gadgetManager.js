
jQuery(document).ready(
    function() {

      jQuery(".nv-category").click(
          function() {
            jQuery("#listCategories>div").removeClass("selected");
            jQuery("#listCategories>div>a").removeClass("selected");
            jQuery(".typeGadget").parent().removeClass("visible");
            jQuery(".typeGadget").parent().addClass("invisible");
            jQuery("a[category='" + jQuery(this).attr("category") + "']")
                .parent().removeClass("invisible").addClass(
                    "visible");
            jQuery(
                "a[category='" + jQuery(this).attr("category")
                    + "'][class='nv-category']").addClass(
                "selected").parent().addClass("selected");
          });

      // Choix du gadget
      jQuery(".typeGadget").click( function() {
        jQuery(".typeGadget").removeClass("selected");
        jQuery(this).addClass("selected");
        return false
      });

      jQuery(".directAddLink").click(
          function() {
            var element = jQuery(this);

            jQuery(".typeGadget").removeClass("selected");
            element.parent().find(".typeGadget").addClass(
                "selected");

            var text = element.parent().find(".addGadgetButton").text();
            element.parent().find(".addGadgetButton").text(text + "...");

            element.parent().find(".addGadgetButton").removeClass("addGadgetButton").addClass("addedGadgetButton");
            element.parent().find(".linkAdd").removeClass("linkAdd").addClass("linkAdded");

            setTimeout( function() {
              element.parent().find(".addedGadgetButton").text(text);
              element.parent().find('.addedGadgetButton').removeClass('addedGadgetButton').addClass('addGadgetButton');
              element.parent().find('.linkAdded').removeClass('linkAdded').addClass('linkAdd');
            }, 2000);

            return false
          });

      jQuery("#openGadgetManager").click( function() {
        jQuery("#getGadgetManager").hide();
        jQuery("#gadgetManager").slideDown();
        return false
      });

      jQuery("#closeLinkGadgetManager").click( function() {
        jQuery("#getGadgetManager").show();
        jQuery("#gadgetManager").slideUp();
        return false;
      });

      jQuery(jQuery(".nv-category")[0]).click();
    });