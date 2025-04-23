package com.mohammad.tictactoewebsocket.service;

import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TicTacToeManager {

    private final Map<String, TicTacToe> games = new ConcurrentHashMap<>();
    protected final Map<String, String> waitingPlayers = new ConcurrentHashMap<>();

    public synchronized TicTacToe joinGame(String player) {
        return games.values().stream()
                .filter(game -> (player.equals(game.getPlayer1()) || player.equals(game.getPlayer2()))
                        && (game.getGameState() == GameState.PLAYER1_TURN || game.getGameState() == GameState.PLAYER2_TURN))
                .findFirst()
                .orElse(null);
    }

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

    public TicTacToe getGame(String gameId) {
        return games.get(gameId);
    }

    public TicTacToe getGameByPlayer(String player) {
        return games.values().stream()
                .filter(game -> player.equals(game.getPlayer1()) || player.equals(game.getPlayer2()))
                .findFirst()
                .orElse(null);
    }

    public void removeGame(String gameId) {
        if (gameRemovalCheck(gameId)) {
            games.remove(gameId);
        }
    }

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
