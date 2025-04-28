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

/**
 * Unit tests for the {@link TicTacToeService} class, which provides game-related services
 * such as joining a game, starting a game, making moves, handling player leaves, and broadcasting messages.
 * This class uses {@link Mockito} for mocking dependencies such as {@link TicTacToeManager} and {@link SimpMessagingTemplate}.
 * The tests verify that the service interacts correctly with the {@link TicTacToeManager}, performs the correct actions,
 * and sends appropriate messages using the messaging template.
 */
@ExtendWith(MockitoExtension.class)
public class TicTacToeServiceTest {
    private TicTacToeManager manager;
    private SimpMessagingTemplate messagingTemplate;
    private TicTacToeService service;

    /**
     * Sets up the test environment, initializing the necessary mocks and the service to be tested.
     */
    @BeforeEach
    void setUp() {
        manager = mock(TicTacToeManager.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new TicTacToeService(manager);
    }

    /**
     * Tests that the {@link TicTacToeService#joinGame(String)} method delegates to the {@link TicTacToeManager#joinGame(String)}
     * method and returns the expected result.
     */
    @Test
    void testJoinGameDelegatesToManager() {
        TicTacToe mockGame = new TicTacToe("Alice", "Bob");
        when(manager.joinGame("Alice")).thenReturn(mockGame);

        TicTacToe result = service.joinGame("Alice");

        assertEquals(mockGame, result);
        verify(manager).joinGame("Alice");
    }

    /**
     * Tests that the {@link TicTacToeService#startGame(String)} method delegates to the {@link TicTacToeManager#startGame(String)}
     * method and returns the expected result.
     */
    @Test
    void testStartGameDelegatesToManager() {
        TicTacToe mockGame = new TicTacToe("Alice", null);
        when(manager.startGame("Alice")).thenReturn(mockGame);

        TicTacToe result = service.startGame("Alice");

        assertEquals(mockGame, result);
        verify(manager).startGame("Alice");
    }

    /**
     * Tests that the {@link TicTacToeService#leaveGame(String)} method delegates to the {@link TicTacToeManager#leaveGame(String)}
     * method, ensuring the correct behavior when a player leaves the game.
     */
    @Test
    void testLeaveGameDelegatesToManager() {
        service.leaveGame("Alice");
        verify(manager).leaveGame("Alice");
    }

    /**
     * Tests that the {@link TicTacToeService#handleEarlyLeave(String, SimpMessagingTemplate)} method sends a message
     * indicating that a player left early when they leave the game before it is finished.
     */
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

    /**
     * Tests that the {@link TicTacToeService#makeMove(String, String, int, SimpMessagingTemplate)} method sends an error
     * message if the game is not found.
     */
    @Test
    void testMakeMoveWhenGameNotFoundSendsError() {
        when(manager.getGame("123")).thenReturn(null);

        service.makeMove("Alice", "123", 1, messagingTemplate);

        verify(messagingTemplate).convertAndSend(eq("/topic/game.123"), any(TicTacToeMessage.class));
    }

    /**
     * Tests that the {@link TicTacToeService#makeMove(String, String, int, SimpMessagingTemplate)} method sends an error
     * message if the game is in the waiting state and a move is attempted.
     */
    @Test
    void testMakeMoveWhenGameWaitingSendsError() {
        TicTacToe mockGame = new TicTacToe("Alice", null);
        mockGame.setGameState(GameState.WAITING_FOR_PLAYER);
        when(manager.getGame("123")).thenReturn(mockGame);

        service.makeMove("Alice", "123", 1, messagingTemplate);

        verify(messagingTemplate).convertAndSend(eq("/topic/game.123"), any(TicTacToeMessage.class));
    }

    /**
     * Tests that the {@link TicTacToeService#makeMove(String, String, int, SimpMessagingTemplate)} method processes
     * a valid move, updates the game state, and sends the move along with a game-over message if the game ends.
     */
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

    /**
     * Tests that the {@link TicTacToeService#playerLeft(String, SimpMessagingTemplate)} method sends a message indicating
     * that the player left and removes the game from the manager.
     */
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
