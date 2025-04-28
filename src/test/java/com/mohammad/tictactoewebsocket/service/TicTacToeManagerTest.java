package com.mohammad.tictactoewebsocket.service;

import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link TicTacToeManager} class.
 * <p>
 * This test class verifies the functionality of the game manager, including starting games,
 * joining games, handling player exits, setting winners, and managing game lifecycle events.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
public class TicTacToeManagerTest {

    private TicTacToeManager manager;

    /**
     * Initializes a new {@link TicTacToeManager} instance before each test.
     */
    @BeforeEach
    void setUp() {
        manager = new TicTacToeManager();
    }

    /**
     * Tests that starting a game with a new player creates a new game.
     */
    @Test
    void testStartGameCreatesNewGame() {
        TicTacToe game = manager.startGame("player1");

        assertNotNull(game);
        assertEquals("player1", game.getPlayer1());
        assertNull(game.getPlayer2());
        assertEquals(game, manager.getGame(game.getGameId()));
    }

    /**
     * Tests that starting a game with another player joins an existing waiting game.
     */
    @Test
    void testStartGameJoinsExistingGame() {
        TicTacToe first = manager.startGame("player1");
        TicTacToe second = manager.startGame("player2");

        assertEquals(first.getGameId(), second.getGameId());
        assertEquals("player1", second.getPlayer1());
        assertEquals("player2", second.getPlayer2());
        assertEquals(GameState.PLAYER1_TURN, second.getGameState());
    }

    /**
     * Tests that joining an existing ongoing game returns the correct game.
     */
    @Test
    void testJoinGameReturnsOngoingGame() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.setGameState(GameState.PLAYER1_TURN);

        TicTacToe joinedGame = manager.joinGame("player1");

        assertNotNull(joinedGame);
        assertEquals(game.getGameId(), joinedGame.getGameId());
    }

    /**
     * Tests that joining a finished game returns {@code null}.
     */
    @Test
    void testJoinGameReturnsNullIfNotFound() {
        TicTacToe game = manager.startGame("player1");
        game.setGameState(GameState.PLAYER1_WON);  // not in progress

        TicTacToe joined = manager.joinGame("player1");

        assertNull(joined);
    }

    /**
     * Tests that leaving a game correctly removes the game from the manager.
     */
    @Test
    void testLeaveGameRemovesGameAndPlayers() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");

        manager.leaveGame("player1");

        assertNull(manager.getGame(game.getGameId()));
    }

    /**
     * Tests that when a player leaves, the opponent is set as the winner.
     */
    @Test
    void testSetWinnerByPlayerLeftAssignsWinner() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");

        manager.setWinnerByPlayerLeft("player1");

        assertEquals("player2", game.getWinner());
        assertEquals(GameState.PLAYER2_WON, game.getGameState());
    }

    /**
     * Tests that if a player leaves without an opponent, no winner is assigned.
     */
    @Test
    void testSetWinnerByPlayerLeftDoesNothingIfNoOpponent() {
        TicTacToe game = manager.startGame("player1");

        manager.setWinnerByPlayerLeft("player1");

        assertNull(game.getWinner());
    }

    /**
     * Tests that getting a game by player name returns the correct game.
     */
    @Test
    void testGetGameByPlayerReturnsCorrectGame() {
        TicTacToe game = manager.startGame("player1");

        TicTacToe found = manager.getGameByPlayer("player1");

        assertEquals(game.getGameId(), found.getGameId());
    }

    /**
     * Tests that getting a game by an unknown player returns {@code null}.
     */
    @Test
    void testGetGameByPlayerReturnsNullIfNotFound() {
        assertNull(manager.getGameByPlayer("unknown"));
    }

    /**
     * Tests that removing a game actually removes it when enough moves have been made.
     */
    @Test
    void testRemoveGameActuallyRemovesIfEligible() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0);
        game.makeMove("player2", 1); // 2 moves, now eligible

        manager.removeGame(game.getGameId());

        assertNull(manager.getGame(game.getGameId()));
    }

    /**
     * Tests that a game is not removed if not enough moves have been made.
     */
    @Test
    void testRemoveGameDoesNotRemoveIfNotEnoughMoves() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0); // Only 1 move

        manager.removeGame(game.getGameId());

        assertNotNull(manager.getGame(game.getGameId()));
    }

    /**
     * Tests that checking game removal returns {@code false} if no game is found.
     */
    @Test
    void testGameRemovalCheckReturnsFalseIfNoGame() {
        assertFalse(manager.gameRemovalCheck("invalid-id"));
    }

    /**
     * Tests that game removal check returns {@code true} when a game has enough moves to be eligible for removal.
     */
    @Test
    void testGameRemovalCheckReturnsTrueIfEnoughMoves() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0);
        game.makeMove("player2", 1);

        assertTrue(manager.gameRemovalCheck(game.getGameId()));
    }

    /**
     * Tests that game removal check returns {@code false} when not enough moves have been made.
     */
    @Test
    void testGameRemovalCheckReturnsFalseIfNotEnoughMoves() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0);

        assertFalse(manager.gameRemovalCheck(game.getGameId()));
    }
}
