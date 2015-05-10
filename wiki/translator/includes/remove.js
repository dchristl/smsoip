$( document ).ready(function() {

$("div").each(function( index ) {
          $this = $(this);
          divId = $this.attr('id');
          divClass = $this.attr('class');
          if (divId == divClass) {
                $this.remove();
          }

});
});