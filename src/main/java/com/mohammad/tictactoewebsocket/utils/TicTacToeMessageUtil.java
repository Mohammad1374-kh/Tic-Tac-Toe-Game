package com.mohammad.tictactoewebsocket.utils;

import com.mohammad.tictactoewebsocket.model.TicTacToe;
import com.mohammad.tictactoewebsocket.model.dto.TicTacToeMessage;
/**
 * Utility class for converting {@link TicTacToe} game objects into {@link TicTacToeMessage} objects.
 * <p>
 * This class provides helper methods to transform the state of a Tic-Tac-Toe game
 * into a message format suitable for communication over WebSocket or other messaging systems.
 * </p>
 */
public class TicTacToeMessageUtil {

    /**
     * Converts a {@link TicTacToe} game instance into a {@link TicTacToeMessage}.
     * <p>
     * Copies relevant fields such as game ID, players, board state, turn, game state,
     * and winner from the game into the message.
     * </p>
     *
     * @param game the {@link TicTacToe} game instance to convert
     * @return a {@link TicTacToeMessage} containing the current state of the game
     */
    public static TicTacToeMessage fromGame(TicTacToe game) {
        TicTacToeMessage message = new TicTacToeMessage();
        message.setGameId(game.getGameId());
        message.setPlayer1(game.getPlayer1());
        message.setPlayer2(game.getPlayer2());
        message.setBoard(game.getBoard());
        message.setTurn(game.getTurn());
        message.setGameState(game.getGameState());
        message.setWinner(game.getWinner());
        return message;
    }
}
