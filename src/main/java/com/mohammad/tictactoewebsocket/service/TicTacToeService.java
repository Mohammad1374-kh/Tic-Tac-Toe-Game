package com.mohammad.tictactoewebsocket.service;

import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;
import com.mohammad.tictactoewebsocket.model.dto.TicTacToeMessage;
import com.mohammad.tictactoewebsocket.utils.TicTacToeMessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service class that manages the flow of Tic-Tac-Toe games.
 * It communicates with the TicTacToeManager to handle player actions and sends updates through WebSocket messaging.
 */
@Service
public class TicTacToeService {
    private final TicTacToeManager ticTacToeManager;

    /**
     * Constructs a new TicTacToeService with a TicTacToeManager dependency.
     *
     * @param ticTacToeManager the manager responsible for managing games
     */
    @Autowired
    public TicTacToeService(TicTacToeManager ticTacToeManager) {
        this.ticTacToeManager = ticTacToeManager;
    }

    /**
     * Allows a player to join an existing ongoing game.
     *
     * @param player the player's identifier
     * @return the game the player joined, or null if none
     */
    public TicTacToe joinGame(String player) {
        return ticTacToeManager.joinGame(player);
    }

    /**
     * Starts a new game or lets the player join a waiting game.
     *
     * @param player the player's identifier
     * @return the game the player started or joined
     */
    public TicTacToe startGame(String player) {
        return ticTacToeManager.startGame(player);
    }

    /**
     * Removes a player from a game.
     *
     * @param player the player's identifier
     */
    public void leaveGame(String player) {
        ticTacToeManager.leaveGame(player);
    }

    /**
     * Handles a situation where a player leaves early (e.g., before making sufficient moves <= 1).
     * Sends a notification to the opponent.
     *
     * @param player             the player who left
     * @param messagingTemplate  the WebSocket messaging template
     */
    public void handleEarlyLeave(String player, SimpMessagingTemplate messagingTemplate) {
        TicTacToe game = ticTacToeManager.getGameByPlayer(player);
        if (game != null) {
            TicTacToeMessage message = TicTacToeMessageUtil.fromGame(game);
            message.setType("game.earlyLeave");
            message.setContent("Opponent left the game. Since less than 2 moves were made, the game is saved for later.");
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), message);
        }
    }

    /**
     * Processes a player's move in the game and updates the game state accordingly.
     * Sends game updates or error messages via WebSocket.
     *
     * @param player             the player making the move
     * @param gameId             the game identifier
     * @param move               the move (0-8) corresponding to the board position
     * @param messagingTemplate  the WebSocket messaging template
     */
    public void makeMove(String player, String gameId, int move, SimpMessagingTemplate messagingTemplate) {
        TicTacToe game = ticTacToeManager.getGame(gameId);
        if (game == null || game.isGameOver()) {
            sendErrorMessage(messagingTemplate, gameId, "Game not found or is already over.");
            return;
        }

        if (game.getGameState().equals(GameState.WAITING_FOR_PLAYER)) {
            sendErrorMessage(messagingTemplate, gameId, "Game is waiting for another player to join.");
            return;
        }

        if (game.getTurn().equals(player)) {
            game.makeMove(player, move);
            TicTacToeMessage stateMessage = new TicTacToeMessage(game);
            stateMessage.setType("game.move");
            messagingTemplate.convertAndSend("/topic/game." + gameId, stateMessage);

            if (game.isGameOver()) {
                TicTacToeMessage overMessage = TicTacToeMessageUtil.fromGame(game);
                overMessage.setType("game.gameOver");
                messagingTemplate.convertAndSend("/topic/game." + gameId, overMessage);
                ticTacToeManager.removeGame(gameId);
            }
        }
    }

    /**
     * Handles the case where a player leaves during an ongoing game and updates the opponent about the situation.
     *
     * @param player             the player who left
     * @param messagingTemplate  the WebSocket messaging template
     */
    public void playerLeft(String player, SimpMessagingTemplate messagingTemplate) {
        TicTacToe game = ticTacToeManager.getGameByPlayer(player);
        if (game != null) {
            ticTacToeManager.setWinnerByPlayerLeft(player);
            TicTacToeMessage message = TicTacToeMessageUtil.fromGame(game);
            message.setType("game.left");
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), message);
            ticTacToeManager.leaveGame(player);
        }
    }

    /**
     * Sends an error message via WebSocket to a specific game channel.
     *
     * @param messagingTemplate the WebSocket messaging template
     * @param gameId             the game identifier
     * @param content            the error message content
     */
    private void sendErrorMessage(SimpMessagingTemplate messagingTemplate, String gameId, String content) {
        TicTacToeMessage errorMessage = new TicTacToeMessage();
        errorMessage.setType("error");
        errorMessage.setContent(content);
        messagingTemplate.convertAndSend("/topic/game." + gameId, errorMessage);
    }
}
