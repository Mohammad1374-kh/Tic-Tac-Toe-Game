package com.mohammad.tictactoewebsocket.controller;

import com.mohammad.tictactoewebsocket.model.TicTacToe;
import com.mohammad.tictactoewebsocket.model.dto.JoinMessage;
import com.mohammad.tictactoewebsocket.model.dto.PlayerMessage;
import com.mohammad.tictactoewebsocket.model.dto.TicTacToeMessage;
import com.mohammad.tictactoewebsocket.service.TicTacToeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MessageControllerTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TicTacToeService gameService;

    @InjectMocks
    private MessageController messageController;

    @Test
    public void testJoinGameReturnsErrorIfGameIsNull() {
        JoinMessage joinMessage = new JoinMessage();
        joinMessage.setPlayer("player1");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionAttributes(new HashMap<>());

        when(gameService.joinGame("player1")).thenReturn(null);

        Object result = messageController.joinGame(joinMessage, accessor);
        Assertions.assertTrue(result instanceof TicTacToeMessage);
        Assertions.assertEquals("error.join", ((TicTacToeMessage) result).getType());
        Assertions.assertEquals("No previous game exists!", ((TicTacToeMessage) result).getContent());
    }

    @Test
    public void testJoinGameReturnsGameMessage() {
        JoinMessage joinMessage = new JoinMessage();
        joinMessage.setPlayer("player1");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionAttributes(new HashMap<>());

        TicTacToe mockGame = new TicTacToe("player1", "player2");

        when(gameService.joinGame("player1")).thenReturn(mockGame);

        Object result = messageController.joinGame(joinMessage, accessor);
        Assertions.assertTrue(result instanceof TicTacToeMessage);
        Assertions.assertEquals("game.joined", ((TicTacToeMessage) result).getType());

        Assertions.assertEquals("player1", accessor.getSessionAttributes().get("player"));
        Assertions.assertEquals(mockGame.getGameId(), accessor.getSessionAttributes().get("gameId"));
    }

    @Test
    public void testStartGameReturnsGameMessage() {
        JoinMessage startMessage = new JoinMessage();
        startMessage.setPlayer("playerX");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionAttributes(new HashMap<>());

        TicTacToe mockGame = new TicTacToe("playerX", null);

        when(gameService.startGame("playerX")).thenReturn(mockGame);

        Object result = messageController.startGame(startMessage, accessor);
        Assertions.assertTrue(result instanceof TicTacToeMessage);
        Assertions.assertEquals("game.joined", ((TicTacToeMessage) result).getType());

        Assertions.assertEquals("playerX", accessor.getSessionAttributes().get("player"));
        Assertions.assertEquals(mockGame.getGameId(), accessor.getSessionAttributes().get("gameId"));
    }

    @Test
    public void testLeaveGameCallsService() {
        PlayerMessage msg = new PlayerMessage();
        msg.setPlayer("player1");
        messageController.leaveGame(msg);
        verify(gameService).playerLeft("player1", messagingTemplate);
    }

    @Test
    public void testLeaveEarlyCallsService() {
        PlayerMessage msg = new PlayerMessage();
        msg.setPlayer("player2");
        messageController.leaveEarly(msg);
        verify(gameService).handleEarlyLeave("player2", messagingTemplate);
    }

    @Test
    public void testMakeMoveCallsService() {
        TicTacToeMessage msg = new TicTacToeMessage();
        msg.setSender("player1");
        msg.setGameId("game123");
        msg.setMove(4);

        messageController.makeMove(msg);

        verify(gameService).makeMove("player1", "game123", 4, messagingTemplate);
    }
}
