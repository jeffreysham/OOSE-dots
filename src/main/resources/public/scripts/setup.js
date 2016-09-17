$(function() {

    var numRows = 5;
    var numCols = 5;

    $('#board').append('<table><tbody></tbody></table>');

    for (var i = 0; i < 2*numRows+1; i++) {

        var row = Math.floor(i/2);

        if (i % 2 == 0) {
            // Build a row with alternating horizontal lines and dots

            $("tbody").append('<tr class="hline-row-'+row+'"></tr>');

            for (var j = 0; j < 2*numCols+1; j++) {

                if (j % 2 == 0) {
                    $('.hline-row-'+row).append('<td class="dot"></td>');
                }
                else {
                    $('.hline-row-'+row).append('<td class="dot"></td>');
                }
            }
        }
        else {
            // Build a row with alternating vertical lines and boxes

            console.log("Buzz");
        }
    }
});