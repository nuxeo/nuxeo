$(document).ready( function() {

  //Choix du nombre de zones
  $(".nv-layout").click(function(){
    $("#listColumns>div").removeClass("selected");
    $("#listColumns>div>button").removeClass("selected");
    $(".typeLayout").parent().removeClass("visible");
    $(".typeLayout").parent().addClass("invisible");
    $("a[box='"+$(this).attr("box")+"']").parent().removeClass("invisible").addClass("visible");
    $("button[box='"+$(this).attr("box")+"']").addClass("selected").parent().addClass("selected");
  });

  //Choix du layout
  $(".typeLayout").click(function(){
    $(".typeLayout").removeClass("selected");
    $(this).addClass("selected");
    return false
  });

  $("#openTabsManager").click(function(){
      $("#tabManagerOpen").slideDown();
      $("#tabManager").hide();
      $(".manager").hide();
      $($("li.active > a").attr("id")).show();
       return false;
  });

  $("#closeLinkLayoutManager").click(function(){
      $("#tabManagerOpen").hide();
      $("#gadgetManager").hide();
      $("#ThemeManager").hide();
      $("#tabManager").show();
      return false;
  });

  $(".tabManage").click(function(){
      $(".manager").hide();
      $("li.active").removeClass("active");
      $("a.selected").removeClass("selected");
      $($(this).parent().get(0)).addClass("active");
      $(this).addClass("selected");
    $($(this).attr("id")).show();
    return false;
  });


});