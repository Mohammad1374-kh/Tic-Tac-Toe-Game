package com.mohammad.tictactoewebsocket.service;
import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;

import com.mohammad.tictactoewebsocket.model.dto.TicTacToeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TicTacToeServiceTest {
    private TicTacToeManager manager;
    private SimpMessagingTemplate messagingTemplate;
    private TicTacToeService service;

    @BeforeEach
    void setUp() {
        manager = mock(TicTacToeManager.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new TicTacToeService(manager);
    }

    @Test
    void testJoinGameDelegatesToManager() {
        TicTacToe mockGame = new TicTacToe("Alice", "Bob");
        when(manager.joinGame("Alice")).thenReturn(mockGame);

        TicTacToe result = service.joinGame("Alice");

        assertEquals(mockGame, result);
        verify(manager).joinGame("Alice");
    }

    @Test
    void testStartGameDelegatesToManager() {
        TicTacToe mockGame = new TicTacToe("Alice", null);
        when(manager.startGame("Alice")).thenReturn(mockGame);

        TicTacToe result = service.startGame("Alice");

        assertEquals(mockGame, result);
        verify(manager).startGame("Alice");
    }

    @Test
    void testLeaveGameDelegatesToManager() {
        service.leaveGame("Alice");
        verify(manager).leaveGame("Alice");
    }

    @Test
    void testHandleEarlyLeaveSendsMessage() {
        TicTacToe mockGame = new TicTacToe("Alice", "Bob");
        mockGame.setGameId("123");
        when(manager.getGameByPlayer("Alice")).thenReturn(mockGame);

        service.handleEarlyLeave("Alice", messagingTemplate);

        ArgumentCaptor<TicTacToeMessage> messageCaptor = ArgumentCaptor.forClass(TicTacToeMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game.123"), messageCaptor.capture());

        TicTacToeMessage msg = messageCaptor.getValue();
        assertEquals("game.earlyLeave", msg.getType());
        assertTrue(msg.getContent().contains("Opponent left the game"));
    }

    @Test
    void testMakeMoveWhenGameNotFoundSendsError() {
        when(manager.getGame("123")).thenReturn(null);

        service.makeMove("Alice", "123", 1, messagingTemplate);

        verify(messagingTemplate).convertAndSend(eq("/topic/game.123"), any(TicTacToeMessage.class));
    }

    @Test
    void testMakeMoveWhenGameWaitingSendsError() {
        TicTacToe mockGame = new TicTacToe("Alice", null);
        mockGame.setGameState(GameState.WAITING_FOR_PLAYER);
        when(manager.getGame("123")).thenReturn(mockGame);

        service.makeMove("Alice", "123", 1, messagingTemplate);

        verify(messagingTemplate).convertAndSend(eq("/topic/game.123"), any(TicTacToeMessage.class));
    }

    @Test
    void testMakeMoveValidTurnSendsMoveAndGameOverIfEnded() {
        TicTacToe mockGame = spy(new TicTacToe("Alice", "Bob"));
        mockGame.setGameId("123");
        mockGame.setGameState(GameState.PLAYER1_TURN);
        when(manager.getGame("123")).thenReturn(mockGame);

        service.makeMove("Alice", "123", 0, messagingTemplate);

        verify(mockGame).makeMove("Alice", 0);
        verify(messagingTemplate).convertAndSend(eq("/topic/game.123"), any(TicTacToeMessage.class));
    }

    @Test
    void testPlayerLeftSendsMessageAndRemovesGame() {
        TicTacToe mockGame = new TicTacToe("Alice", "Bob");
        mockGame.setGameId("321");
        when(manager.getGameByPlayer("Alice")).thenReturn(mockGame);

        service.playerLeft("Alice", messagingTemplate);

        verify(manager).setWinnerByPlayerLeft("Alice");
        verify(manager).leaveGame("Alice");
        verify(messagingTemplate).convertAndSend(eq("/topic/game.321"), any(TicTacToeMessage.class));
    }
}
