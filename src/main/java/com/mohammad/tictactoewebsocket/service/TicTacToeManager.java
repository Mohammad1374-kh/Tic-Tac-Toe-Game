package com.mohammad.tictactoewebsocket.service;

import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component class responsible for managing all active Tic-Tac-Toe games.
 * It handles creating, finding, and removing games, as well as setting winners in case of player departures.
 */
@Component
public class TicTacToeManager {

    private final Map<String, TicTacToe> games = new ConcurrentHashMap<>();
    protected final Map<String, String> waitingPlayers = new ConcurrentHashMap<>();

    /**
     * Allows a player to join an existing ongoing game.
     *
     * @param player the player's identifier
     * @return the joined game, or null if no game available
     */
    public synchronized TicTacToe joinGame(String player) {
        return games.values().stream()
                .filter(game -> (player.equals(game.getPlayer1()) || player.equals(game.getPlayer2()))
                        && (game.getGameState() == GameState.PLAYER1_TURN || game.getGameState() == GameState.PLAYER2_TURN))
                .findFirst()
                .orElse(null);
    }

    /**
     * Starts a new game if no other waiting players exist, otherwise joins a waiting game.
     *
     * @param player the player's identifier
     * @return the started or joined game
     */
    public synchronized TicTacToe startGame(String player) {
        Optional<TicTacToe> existingGame = games.values().stream()
                .filter(game -> player.equals(game.getPlayer1()) && game.getPlayer2() == null)
                .findFirst();

        if (existingGame.isPresent()) {
            // Remove old incomplete game
            games.remove(existingGame.get().getGameId());
            waitingPlayers.remove(player);
        } else {
            // Try to find another waiting player
            for (TicTacToe game : games.values()) {
                if (game.getPlayer1() != null && game.getPlayer2() == null) {
                    game.setPlayer2(player);
                    game.setGameState(GameState.PLAYER1_TURN);
                    return game;
                }
            }
        }

        // If no existing games to join, create a new one
        TicTacToe newGame = new TicTacToe(player, null);
        games.put(newGame.getGameId(), newGame);
        waitingPlayers.put(player, newGame.getGameId());
        return newGame;
    }

    /**
     * Removes a player and their associated game from active games.
     *
     * @param player the player's identifier
     */
    public synchronized void leaveGame(String player) {
        TicTacToe game = getGameByPlayer(player);
        if (game != null) {
            games.remove(game.getGameId());
            waitingPlayers.remove(game.getPlayer1());
            if (game.getPlayer2() != null) {
                waitingPlayers.remove(game.getPlayer2());
            }
        }
    }

    /**
     * Sets the opponent of the leaving player as the winner if a player leaves mid-game.(moves made > 1)
     *
     * @param player the player who left
     */
    public synchronized void setWinnerByPlayerLeft(String player) {
        TicTacToe game = getGameByPlayer(player);
        if (game != null) {
            String opponent = player.equals(game.getPlayer1()) ? game.getPlayer2() : game.getPlayer1();
            if (opponent != null) {
                game.setWinner(opponent);
                game.setGameState(opponent.equals(game.getPlayer1()) ? GameState.PLAYER1_WON : GameState.PLAYER2_WON);
            }
        }
    }

    /**
     * Retrieves a game by its unique ID.
     *
     * @param gameId the game ID
     * @return the game instance, or null if not found
     */
    public TicTacToe getGame(String gameId) {
        return games.get(gameId);
    }

    /**
     * Retrieves a game by a player involved in it.
     *
     * @param player the player's identifier
     * @return the game instance, or null if not found
     */
    public TicTacToe getGameByPlayer(String player) {
        return games.values().stream()
                .filter(game -> player.equals(game.getPlayer1()) || player.equals(game.getPlayer2()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes a game if it has more than one move made (otherwise it is kept).
     *
     * @param gameId the game ID
     */
    public void removeGame(String gameId) {
        if (gameRemovalCheck(gameId)) {
            games.remove(gameId);
        }
    }

    /**
     * Checks whether a game can be removed (only after more than 1 move has been made).
     *
     * @param gameId the game ID
     * @return true if the game can be removed, false otherwise
     */
    public boolean gameRemovalCheck(String gameId) {
        TicTacToe game = games.get(gameId);
        if (game == null) return false;

        long movesMade = Arrays.stream(game.getBoard())
                .flatMap(Arrays::stream)
                .filter(cell -> !cell.equals(" "))
                .count();

        return movesMade > 1;
    }

}
