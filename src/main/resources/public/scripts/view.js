States = {
    WAITING_TO_START: 'WAITING_TO_START',
    IN_PROGRESS: 'IN_PROGRESS',
    FINISHED: 'FINISHED'
};

WhoseTurn = {
    RED: 'RED',
    BLUE: 'BLUE',
    FINISHED: 'FINISHED'
};


function View() {
    this.setup();
}


View.prototype.setup = function() {
    $('#game-info').html('<h4 class="score">No game in progress. Please create or join a game.</h4>');
}


View.prototype.scoreText = function(redScore, blueScore, playerType) {
    var el =
        '<h4 class="score red">Red: '+redScore + (playerType == 'RED' ? ' (You)' : '') + '</h4>' +
        '<h4 class="score blue">Blue: '+blueScore + (playerType == 'BLUE' ? ' (You)' : '') +'</h4>';
    return el;
}


View.prototype.updateState = function(state, whoseTurn, redScore, blueScore, playerType, gameId) {

    if (state === States.WAITING_TO_START) {
        var el =
            '<h4 class="score">You are ' +
                '<emph class="' + playerType.toLowerCase() + '">' + playerType.toLowerCase() + '</emph>.'+
            '</h4>' +
            '<h4 class="score">Waiting for the other player to join...</h4>' +
            '<h4 class="score">Send <a target="_blank" href="#/join/'+gameId+'">this link</a> to the other player for them to join!</h4>';
        $('#game-info').html(el);
    }
    else if (state === States.IN_PROGRESS) {
        var el = '';
        if (whoseTurn === playerType)
            el += '<h4 class="score">It\'s your turn!</h4>';
        else
            el += '<h4 class="score">Waiting for the other player to move...</h4>';
        el += this.scoreText(redScore, blueScore, playerType);
        $('#game-info').html(el);
    }
    else if (state === States.FINISHED) {
        var el = '';
        if (redScore > blueScore) {
            if (playerType === 'RED')
                el += '<h4 class="score">You won!</h4>';
            else
                el += '<h4 class="score">You lost!</h4>';
        }
        else if (blueScore > redScore) {
            if (playerType === 'BLUE')
                el += '<h4 class="score">You won!</h4>';
            else
                el += '<h4 class="score">You lost!</h4>';
        }
        else {
            el += '<h4 class="score">It\'s a tie!</h4>';
        }
        el += this.scoreText(redScore, blueScore, playerType);
        $('#game-info').html(el);
    }
    else {
        this.postError('State '+state+' is invalid! Must be one of WAITING_TO_START, IN_PROGRESS, or FINISHED.');
    }
}


View.prototype.postInfo = function(message) {
    var el = '<div class="alert alert-info fade in">' +
                  '<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>' +
                        message +
             '</div>';
    $('#rest-info').prepend(el);
}

View.prototype.postInfoWithCode = function(message, status, textStatus) {
    message += ' Error '+status+': '+textStatus;
    var el = '<div class="alert alert-info fade in">' +
                  '<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>' +
                        message +
             '</div>';
    $('#rest-info').prepend(el);
}

View.prototype.postError = function(message) {
    var el = '<div class="alert alert-danger fade in">' +
                  '<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>' +
                        message +
             '</div>';
    $('#rest-info').prepend(el);
}


View.prototype.postErrorWithCode = function(message, status, textStatus) {
    message += ' Error '+status+': '+textStatus;
    var el = '<div class="alert alert-danger fade in">' +
                  '<a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>' +
                        message +
             '</div>';
    $('#rest-info').prepend(el);
}