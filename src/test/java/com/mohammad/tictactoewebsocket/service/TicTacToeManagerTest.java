package com.mohammad.tictactoewebsocket.service;

import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TicTacToeManagerTest {

    private TicTacToeManager manager;

    @BeforeEach
    void setUp() {
        manager = new TicTacToeManager();
    }

    @Test
    void testStartGameCreatesNewGame() {
        TicTacToe game = manager.startGame("player1");

        assertNotNull(game);
        assertEquals("player1", game.getPlayer1());
        assertNull(game.getPlayer2());
        assertEquals(game, manager.getGame(game.getGameId()));
    }

    @Test
    void testStartGameJoinsExistingGame() {
        TicTacToe first = manager.startGame("player1");
        TicTacToe second = manager.startGame("player2");

        assertEquals(first.getGameId(), second.getGameId());
        assertEquals("player1", second.getPlayer1());
        assertEquals("player2", second.getPlayer2());
        assertEquals(GameState.PLAYER1_TURN, second.getGameState());
    }

    @Test
    void testJoinGameReturnsOngoingGame() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.setGameState(GameState.PLAYER1_TURN);

        TicTacToe joinedGame = manager.joinGame("player1");

        assertNotNull(joinedGame);
        assertEquals(game.getGameId(), joinedGame.getGameId());
    }

    @Test
    void testJoinGameReturnsNullIfNotFound() {
        TicTacToe game = manager.startGame("player1");
        game.setGameState(GameState.PLAYER1_WON);  // not in progress

        TicTacToe joined = manager.joinGame("player1");

        assertNull(joined);
    }

    @Test
    void testLeaveGameRemovesGameAndPlayers() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");

        manager.leaveGame("player1");

        assertNull(manager.getGame(game.getGameId()));
    }

    @Test
    void testSetWinnerByPlayerLeftAssignsWinner() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");

        manager.setWinnerByPlayerLeft("player1");

        assertEquals("player2", game.getWinner());
        assertEquals(GameState.PLAYER2_WON, game.getGameState());
    }

    @Test
    void testSetWinnerByPlayerLeftDoesNothingIfNoOpponent() {
        TicTacToe game = manager.startGame("player1");

        manager.setWinnerByPlayerLeft("player1");

        assertNull(game.getWinner());
    }

    @Test
    void testGetGameByPlayerReturnsCorrectGame() {
        TicTacToe game = manager.startGame("player1");

        TicTacToe found = manager.getGameByPlayer("player1");

        assertEquals(game.getGameId(), found.getGameId());
    }

    @Test
    void testGetGameByPlayerReturnsNullIfNotFound() {
        assertNull(manager.getGameByPlayer("unknown"));
    }

    @Test
    void testRemoveGameActuallyRemovesIfEligible() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0);
        game.makeMove("player2", 1); // 2 moves, now eligible

        manager.removeGame(game.getGameId());

        assertNull(manager.getGame(game.getGameId()));
    }

    @Test
    void testRemoveGameDoesNotRemoveIfNotEnoughMoves() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0); // Only 1 move

        manager.removeGame(game.getGameId());

        assertNotNull(manager.getGame(game.getGameId()));
    }

    @Test
    void testGameRemovalCheckReturnsFalseIfNoGame() {
        assertFalse(manager.gameRemovalCheck("invalid-id"));
    }

    @Test
    void testGameRemovalCheckReturnsTrueIfEnoughMoves() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0);
        game.makeMove("player2", 1);

        assertTrue(manager.gameRemovalCheck(game.getGameId()));
    }

    @Test
    void testGameRemovalCheckReturnsFalseIfNotEnoughMoves() {
        TicTacToe game = manager.startGame("player1");
        game.setPlayer2("player2");
        game.makeMove("player1", 0);

        assertFalse(manager.gameRemovalCheck(game.getGameId()));
    }
}
