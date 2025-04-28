package com.mohammad.tictactoewebsocket.controller;

import com.mohammad.tictactoewebsocket.model.TicTacToe;
import com.mohammad.tictactoewebsocket.model.dto.JoinMessage;
import com.mohammad.tictactoewebsocket.model.dto.PlayerMessage;
import com.mohammad.tictactoewebsocket.model.dto.TicTacToeMessage;
import com.mohammad.tictactoewebsocket.service.TicTacToeService;
import com.mohammad.tictactoewebsocket.utils.TicTacToeMessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Controller class for handling WebSocket messages and managing the Tic-Tac-Toe games.
 */
@Controller
public class MessageController {

    /**
     * Template for sending messages to clients through the message broker.
     */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    /**
     * Service class for managing Tic-Tac-Toe game logic.
     */
    @Autowired
    private TicTacToeService gameService;

    /**
     * Handles a request from a player to join an existing game.
     * If no existing game is available, returns an error message.
     *
     * @param message         the join message containing the player's name
     * @param headerAccessor  the header accessor for setting session attributes
     * @return                the updated game state or an error message
     */
    @MessageMapping("/game.join")
    @SendTo("/topic/game.state")
    public Object joinGame(@Payload JoinMessage message, SimpMessageHeaderAccessor headerAccessor) {
        TicTacToe game = gameService.joinGame(message.getPlayer());
        if (game == null) {
            return createError("No previous game exists!", "error.join");
        }

        headerAccessor.getSessionAttributes().put("gameId", game.getGameId());
        headerAccessor.getSessionAttributes().put("player", message.getPlayer());

        TicTacToeMessage gameMessage = TicTacToeMessageUtil.fromGame(game);
        gameMessage.setType("game.joined");
        return gameMessage;
    }


    /**
     * Handles a request from a player to start a new game.
     *
     * @param message         the start message containing the player's name
     * @param headerAccessor  the header accessor for setting session attributes
     * @return                the created game state
     */
    @MessageMapping("/game.start")
    @SendTo("/topic/game.state")
    public Object startGame(@Payload JoinMessage message, SimpMessageHeaderAccessor headerAccessor) {
        TicTacToe game = gameService.startGame(message.getPlayer());

        headerAccessor.getSessionAttributes().put("gameId", game.getGameId());
        headerAccessor.getSessionAttributes().put("player", message.getPlayer());

        TicTacToeMessage gameMessage = TicTacToeMessageUtil.fromGame(game);
        gameMessage.setType("game.joined");
        return gameMessage;
    }

    /**
     * Handles a request when a player voluntarily leaves an ongoing game
     * after that moves made are more than one
     * @param message  the player message containing the player's name
     */
    @MessageMapping("/game.leave")
    public void leaveGame(@Payload PlayerMessage message) {
        gameService.playerLeft(message.getPlayer(), messagingTemplate);
    }

    /**
     * Handles a request when a player leaves early
     * when moves made are less than one
     * @param message  the player message containing the player's name
     */
    @MessageMapping("/game.leaveEarly")
    public void leaveEarly(@Payload PlayerMessage message) {
        gameService.handleEarlyLeave(message.getPlayer(), messagingTemplate);
    }

    /**
     * Handles a request when a player makes a move during their turn.
     *
     * @param message  the move message containing move details, sender, and game ID
     */
    @MessageMapping("/game.move")
    public void makeMove(@Payload TicTacToeMessage message) {
        gameService.makeMove(message.getSender(), message.getGameId(), message.getMove(), messagingTemplate);
    }

    /**
     * Creates a standardized error message to be sent to the client.
     *
     * @param content  the error message content
     * @param type     the type of error
     * @return         the constructed error message
     */
    private TicTacToeMessage createError(String content, String type) {
        TicTacToeMessage errorMessage = new TicTacToeMessage();
        errorMessage.setType(type);
        errorMessage.setContent(content);
        return errorMessage;
    }
}
