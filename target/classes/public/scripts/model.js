function Model() {

    this.gameId = null;
    this.playerId = null;
    this.playerType = null;
    this.state = null;
    this.redScore = null;
    this.blueScore = null;
    this.whoseTurn = null;
}


Model.prototype.createGame = function(playerType) {

    var params = {
        playerType: playerType
    };

    var self = this;

    return $.post('/dots/api/games', JSON.stringify(params), null, 'json')
        .done(function (data) {
            self.gameId = data.gameId;
            self.playerId = data.playerId;
            self.playerType = data.playerType;
        });
}


Model.prototype.joinGame = function(gameId) {

    var self = this;

    return $.ajax({
        url: '/dots/api/games/'+gameId,
        method: 'PUT',
        dataType: 'json'
    }).done(function (data) {
        self.gameId = data.gameId;
        self.playerId = data.playerId;
        self.playerType = data.playerType;
    });
}


Model.prototype.makeHorizontalMove = function(row, col) {

    var playerId = this.playerId;

    var params = {
        playerId: playerId,
        row: row,
        col: col
    };

    return $.post('/dots/api/games/'+this.gameId+'/hmove', JSON.stringify(params), null, 'json');
}


Model.prototype.makeVerticalMove = function(row, col) {

    var playerId = this.playerId;

    var params = {
        playerId: playerId,
        row: row,
        col: col
    };
    return $.post('/dots/api/games/'+this.gameId+'/vmove', JSON.stringify(params), null, 'json');
}


Model.prototype.getBoard = function() {
    return $.get('/dots/api/games/'+this.gameId+'/board', '{}', null, 'json');
}


Model.prototype.getState = function() {

    var self = this;

    return $.get('/dots/api/games/'+this.gameId+'/state', '{}', null, 'json')
        .done(function (data) {
            self.state = null;
            self.redScore = null;
            self.blueScore = null;
            self.whoseTurn = null;
        });
}
