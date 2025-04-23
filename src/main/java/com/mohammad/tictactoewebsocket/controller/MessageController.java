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


@Controller
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TicTacToeService gameService;

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

    @MessageMapping("/game.leave")
    public void leaveGame(@Payload PlayerMessage message) {
        gameService.playerLeft(message.getPlayer(), messagingTemplate);
    }

    @MessageMapping("/game.leaveEarly")
    public void leaveEarly(@Payload PlayerMessage message) {
        gameService.handleEarlyLeave(message.getPlayer(), messagingTemplate);
    }

    @MessageMapping("/game.move")
    public void makeMove(@Payload TicTacToeMessage message) {
        gameService.makeMove(message.getSender(), message.getGameId(), message.getMove(), messagingTemplate);
    }

    private TicTacToeMessage createError(String content, String type) {
        TicTacToeMessage errorMessage = new TicTacToeMessage();
        errorMessage.setType(type);
        errorMessage.setContent(content);
        return errorMessage;
    }
}
