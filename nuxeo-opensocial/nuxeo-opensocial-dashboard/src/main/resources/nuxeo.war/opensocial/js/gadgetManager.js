$(document).ready(
    function() {

      $(".nv-category").click(
          function() {
            $("#listCategories>div").removeClass("selected");
            $("#listCategories>div>a").removeClass("selected");
            $(".typeGadget").parent().removeClass("visible");
            $(".typeGadget").parent().addClass("invisible");
            $("a[category='" + $(this).attr("category") + "']")
                .parent().removeClass("invisible").addClass(
                    "visible");
            $(
                "a[category='" + $(this).attr("category")
                    + "'][class='nv-category']").addClass(
                "selected").parent().addClass("selected");
          });

      // Choix du gadget
      $(".typeGadget").click( function() {
        $(".typeGadget").removeClass("selected");
        $(this).addClass("selected");
        return false
      });

      $(".directAddLink").click(
          function() {
            var element = $(this);

            $(".typeGadget").removeClass("selected");
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

      $("#openGadgetManager").click( function() {
        $("#getGadgetManager").hide();
        $("#gadgetManager").slideDown();
        return false
      });

      $("#closeLinkGadgetManager").click( function() {
        $("#getGadgetManager").show();
        $("#gadgetManager").slideUp();
        return false;
      });

      $($(".nv-category")[0]).click();
    });