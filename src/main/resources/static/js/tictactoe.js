class GameController {
    constructor() {
        this.stompClient = null;
        this.game = null;
        this.player = null;
    }

    /**
     * Sends a message to the server using the STOMP client.
     * @param {Object} message - The message to be sent. Must contain at least a "type" field.
     */
    sendMessage(message) {
        this.stompClient.send(`/app/${message.type}`, {}, JSON.stringify(message));
    }

    /**
     * Connects to the WebSocket server and initializes the STOMP client.
     * @returns {Promise} A promise that resolves when the connection is established.
     */
    connectWebSocket() {
        return new Promise((resolve) => {
            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);
            this.stompClient.connect({}, (frame) => resolve(this.stompClient));
        });
    }

    /**
     * Connects to the WebSocket and subscribes to the general game state updates.
     * Also determines whether to start or join a game based on URL parameters.
     */
    connect() {
        this.connectWebSocket().then(() => {
            this.stompClient.subscribe('/topic/game.state', (message) => {
                this.handleMessage(JSON.parse(message.body));
            });

            const type = new URLSearchParams(window.location.search).get('type');
            if (type === 'start') {
                this.startGame();
            } else if (type === 'join') {
                this.loadGame();
            } else {
                this.startGame();
            }
        });
    }

    /**
     * Sends a move message to the server.
     * @param {Number} move - The index of the cell where the move should be made.
     */
    makeMove(move) {
        this.sendMessage({
            type: "game.move",
            move: move,
            turn: this.game.turn,
            sender: this.player,
            gameId: this.game.gameId
        });
    }

    /**
     * Loads the game for an existing player using the stored player name.
     */
    loadGame() {
        const playerName = localStorage.getItem("playerName");
        this.sendMessage({ type: "game.join", player: playerName });
    }

    /**
     * Starts a new game or prompts the user to join if no player name is found.
     */
    startGame() {
        const playerName = localStorage.getItem("playerName");
        if (playerName) {
            this.sendMessage({ type: "game.start", player: playerName });
        } else {
            this.joinGame();
        }
    }

    /**
     * Prompts the user for their name and stores it locally,
     * then sends a join message to the server.
     */
    joinGame() {
        const playerName = prompt("Enter your name:");
        localStorage.setItem("playerName", playerName);
        this.sendMessage({ type: "game.join", player: playerName });
    }

    /**
     * Updates the local game state and refreshes the UI accordingly.
     * @param {Object} message - The message received from the server containing game state.
     */
    updateGame(message) {
        this.game = this.messageToGame(message);
        ui.updateAll(this.game);
    }

    /**
     * Converts a message received from the server into a game object.
     * @param {Object} message - The message received.
     * @returns {Object} The game object.
     */
    messageToGame(message) {
        return {
            gameId: message.gameId,
            board: message.board,
            turn: message.turn,
            player1: message.player1,
            player2: message.player2,
            gameState: message.gameState,
            winner: message.winner
        };
    }

    /**
     * Handles a message received from the server.
     * @param {Object} message - The message received.
     */
    handleMessage(message) {
        const handlers = {
            "game.join": () => this.updateGame(message),
            "game.move": () => this.updateGame(message),
            "game.gameOver": () => {
                this.updateGame(message);
                message.gameState === 'TIE'
                    ? toastr.success("Game over! It's a tie!")
                    : ui.showWinner(message.winner, message.board);
            },
            "game.joined": () => {
                if (this.game && this.game.gameId !== message.gameId) return;
                this.player = localStorage.getItem("playerName");
                this.updateGame(message);
                this.connectWebSocket().then(() => {
                    this.stompClient.subscribe(`/topic/game.${message.gameId}`, (msg) => {
                        this.handleMessage(JSON.parse(msg.body));
                    });
                });
            },
            "game.left": () => {
                this.updateGame(message);
                toastr.warning("Opponent left the game. You win by default.");
                ui.showWinner(message.winner, message.board);
            },
            "game.earlyLeave": () => toastr.warning(message.content),
            "error": () => toastr.error(message.content),
            "error.join": () => {
                localStorage.setItem("errorMessage", message.content);
                setTimeout(() => window.location.href = "/index", 100);
            }
        };
        if (handlers[message.type]) handlers[message.type]();
    }
}

class UIController {
    /**
     * Updates all parts of the UI based on the current game state.
     * @param {Object} game - The game object containing the current state.
     */
    updateAll(game) {
        this.updatePlayers(game.player1, game.player2, game.winner);
        this.updateTurn(game.turn);
        this.updateWinner(game.winner);
        this.updateBoard(game.board);
    }

    /**
     * Updates the displayed player names.
     * @param {String} player1 - Name of player 1.
     * @param {String} player2 - Name of player 2.
     * @param {String} winner - Name of the winner, if available.
     */
    updatePlayers(player1, player2, winner) {
        document.getElementById("player1").innerHTML = player1;
        document.getElementById("player2").innerHTML = player2 || (winner ? '-' : 'Waiting for player 2...');
    }

    /**
     * Updates the displayed current turn.
     * @param {String} turn - The player whose turn it is.
     */
    updateTurn(turn) {
        document.getElementById("turn").innerHTML = turn;
    }

    /**
     * Updates the displayed winner.
     * @param {String} winner - Name of the winner or '-' if none.
     */
    updateWinner(winner) {
        document.getElementById("winner").innerHTML = winner || '-';
    }

    /**
     * Updates the Tic-Tac-Toe board on the UI.
     * @param {Array} board - The 2D array representing the game board.
     */
    updateBoard(board) {
        let counter = 0;
        board.forEach((row, rowIndex) => {
            row.forEach((cell, cellIndex) => {
                const cellElement = document.querySelector(`.row-${rowIndex} .cell-${cellIndex}`);
                cellElement.innerHTML = cell === ' ' ? `<button onclick="gameController.makeMove(${counter})"> </button>` : `<span class="cell-item">${cell}</span>`;
                counter++;
            });
        });
    }

    /**
     * Displays a success message with the name of the winning player.
     * @param {String} winner - The name of the winning player.
     * @param {Array} board  - The 2D array to get the winning player moves that caused victory
     */
    showWinner(winner, board) {
        toastr.success(`The winner is ${winner}!`);
        const winningPositions = this.getWinnerPositions(board);
        if (winningPositions.length === 3) {
            winningPositions.forEach(pos => {
                const row = Math.floor(pos / 3);
                const cell = pos % 3;
                const cellElement = document.querySelector(`.row-${row} .cell-${cell} span`);
                cellElement.style.backgroundColor = '#b3e6ff';
            });
        }
    }

    /**
     * Determines the positions on the board that resulted in a win.
     * @param {Array} board - The 2D array representing the game board.
     * @returns {Array} An array of indexes representing the winning positions, or an empty array.
     */
    getWinnerPositions(board) {
        const lines = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8],
            [0, 3, 6], [1, 4, 7], [2, 5, 8],
            [0, 4, 8], [2, 4, 6]
        ];
        const flatBoard = board.flat();
        return lines.find(line => {
            const [a, b, c] = line;
            return flatBoard[a] !== ' ' && flatBoard[a] === flatBoard[b] && flatBoard[b] === flatBoard[c];
        }) || [];
    }
}

/**
 * Sets up the "Back to Home" button, handling early or in-progress game exits.
 * @param {GameController} controller - The instance of the game controller.
 * If the two player joined and more than one move were made in the game, the leaving player will lose
 * otherwise if the moves made were lower and equal than two both player may leave and the game will be saved for later
 * and other player will be informed to wait for the other opponents or leave the game
 */
function setupBackToHomeButton(controller) {
    const btn = document.getElementById("backToHomeBtn");
    if (!btn) return;
    btn.addEventListener("click", () => {
        if (!controller.game) return window.location.href = "/index";

        const cellsFilled = controller.game.board.flat().filter(c => c !== " ").length;
        const inProgress = controller.game.gameState === 'PLAYER1_TURN' || controller.game.gameState === 'PLAYER2_TURN';

        if (inProgress && cellsFilled > 1) {
            if (confirm("Are you sure you want to leave the game? You will forfeit and your opponent will win.")) {
                controller.sendMessage({ type: "game.leave", player: controller.player });
                window.location.href = "/index";
            }
        } else if (inProgress && cellsFilled <= 1) {
            controller.sendMessage({
                type: "game.leaveEarly",
                player: controller.player,
                message: "Opponent left the game. Since less than 2 moves were made, the game is saved for later."
            });
            window.location.href = "/index";
        } else {
            window.location.href = "/index";
        }
    });
}

const gameController = new GameController();
const ui = new UIController();

window.onload = () => {
    gameController.connect();
    setupBackToHomeButton(gameController);
};
