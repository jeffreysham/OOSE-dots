function Board(game) {

    this.game = game;

    this.numRows = 4;
    this.numCols = 4;

    this.size = 360;
    this.radius = 8;

    this.pad = 20;

    this.redColor = '#d44';
    this.blueColor = '#44d';
    this.filledColor = '#333';
    this.unfilledColor = '#bbb';
    this.selectedColor = '#888';

    this.setupBoard();
}


Board.prototype.resetBoard = function() {
    $('.hline, .vline').css({stroke: this.unfilledColor, transition: '0.125s'}).removeAttr('filled');
    $('.box').css({fill: 'white', transition: '0.125s'});
}


Board.prototype.updateBoard = function(horizontalLines, verticalLines, boxes) {

    for (var i in horizontalLines) {
        var hline = horizontalLines[i];
        if (hline.filled) {
            $('.hline[row="'+hline.row+'"][col="'+hline.col+'"]')
                .attr('filled', true)
                .css({stroke: this.filledColor, transition: '0.125s' });
        }
    }

    for (var i in verticalLines) {
        var vline = verticalLines[i];
        if (vline.filled) {
            $('.vline[row="'+vline.row+'"][col="'+vline.col+'"]')
                .attr('filled', true)
                .css({stroke: this.filledColor, transition: '0.125s' });
        }
    }

    for (var i in boxes) {
        var box = boxes[i];

        var color = 'white';
        if (box.owner === 'RED')
            color = this.redColor;
        else if (box.owner === 'BLUE')
            color = this.blueColor;

        if (box.owner !== 'NONE')
            $('.box[row="'+box.row+'"][col="'+box.col+'"]')
                .css({fill: color, transition: '0.125s' });
    }
}


Board.prototype.setupBoard = function() {

    var size    = this.size;
    var pad     = this.pad;
    var r       = this.radius;

    var numRows = this.numRows;
    var numCols = this.numCols;

    var yPos = function(y) { return pad + (size-2*pad) * y/numRows; }
    var xPos = function(x) { return pad + (size-2*pad) * x/numCols; }
    var boxSize = yPos(1)-yPos(0);

    $('#board > svg').attr('width', size).attr('height', size);


    // Create boxes

    for (var y = 0; y < numRows; y++) {
        for (var x = 0; x < numCols; x++) {
            var box = $('<rect class="box" />')
                .attr('row', y).attr('col', x)
                .attr('y', yPos(y))
                .attr('x', xPos(x))
                .attr('width', boxSize)
                .attr('height', boxSize)
                .attr('fill', 'white');
            $('#board > svg').append(box);
        }
    }

    // Create horizontal lines

    for (var y = 0; y < numRows+1; y++) {
        for (var x = 0; x < numCols; x++) {
            var line = $('<line class="hline" />')
                .attr('row', y).attr('col', x)
                .attr('y1', yPos(y))
                .attr('x1', xPos(x))
                .attr('y2', yPos(y))
                .attr('x2', xPos(x+1))
                .css('stroke', this.unfilledColor)
                .css('stroke-width', r);
            $('#board > svg').append(line);
        }
    }

    // Create vertical lines

    for (var y = 0; y < numRows; y++) {
        for (var x = 0; x < numCols+1; x++) {
            var line = $('<line class="vline" />')
                .attr('row', y).attr('col', x)
                .attr('y1', yPos(y))
                .attr('x1', xPos(x))
                .attr('y2', yPos(y+1))
                .attr('x2', xPos(x))
                .css('stroke', this.unfilledColor)
                .css('stroke-width', r);

            $('#board > svg').append(line);
        }
    }

    // Create dots

    for (var y = 0; y < numRows+1; y++) {
        for (var x = 0; x < numCols+1; x++) {
            var dot = $('<circle class="dot" />')
                .attr('cy', yPos(y))
                .attr('cx', xPos(x))
                .attr('r', r)
                .attr('fill', this.filledColor);
            $('#board > svg').append(dot);
        }
    }

    // Refresh the board

    $("#board").html($("#board").html());

    // Add actions to lines

    var boardRef = this;
    var game = this.game;

    var selectedColor = this.selectedColor;
    var unfilledColor = this.unfilledColor;
    var filledColor = this.filledColor;

    $('.hline, .vline').hover(
        function() {
            if ($(this).attr('filled'))
                $(this).css({stroke: filledColor, strokeWidth: 1.25*r, transition: '0.125s'});
            else
                $(this).css({stroke: selectedColor, strokeWidth: 1.25*r, transition: '0.125s'});
        },
        function() {
            if ($(this).attr('filled'))
                $(this).css({stroke: filledColor, strokeWidth: r, transition: '0.125s'})
            else
                $(this).css({stroke: unfilledColor, strokeWidth: r, transition: '0.125s'})
        }
    );

    $('.hline').on('click', function() {
        var row = $(this).attr('row');
        var col = $(this).attr('col');

        var self = this;

        game.makeHorizontalMove(row, col)
            .done(function (data) {
                $(self).attr('filled', true).css({stroke: filledColor, transition: '0.125s'});
            })
            .fail(function () {
                if (!$(self).attr('filled'))
                    $(self).css({stroke: unfilledColor, transition: '0.125s'});
            });
    });

    $('.vline').on('click', function() {
        var row = $(this).attr('row');
        var col = $(this).attr('col');

        var self = this;

        game.makeVerticalMove(row, col)
            .done(function (data) {
                $(self).attr('filled', true).css({stroke: filledColor, transition: '0.125s'});
            })
            .fail(function () {
                if (!$(self).attr('filled'))
                    $(self).css({stroke: unfilledColor, transition: '0.125s'});
            });
    });
}
