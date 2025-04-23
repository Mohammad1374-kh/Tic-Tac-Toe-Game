package com.mohammad.tictactoewebsocket.service;

import com.mohammad.tictactoewebsocket.enumeration.GameState;
import com.mohammad.tictactoewebsocket.model.TicTacToe;
import com.mohammad.tictactoewebsocket.model.dto.TicTacToeMessage;
import com.mohammad.tictactoewebsocket.utils.TicTacToeMessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TicTacToeService {
    private final TicTacToeManager ticTacToeManager;

    @Autowired
    public TicTacToeService(TicTacToeManager ticTacToeManager) {
        this.ticTacToeManager = ticTacToeManager;
    }

    public TicTacToe joinGame(String player) {
        return ticTacToeManager.joinGame(player);
    }

    public TicTacToe startGame(String player) {
        return ticTacToeManager.startGame(player);
    }

    public void leaveGame(String player) {
        ticTacToeManager.leaveGame(player);
    }

    public void handleEarlyLeave(String player, SimpMessagingTemplate messagingTemplate) {
        TicTacToe game = ticTacToeManager.getGameByPlayer(player);
        if (game != null) {
            TicTacToeMessage message = TicTacToeMessageUtil.fromGame(game);
            message.setType("game.earlyLeave");
            message.setContent("Opponent left the game. Since less than 2 moves were made, the game is saved for later.");
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), message);
        }
    }

    public void makeMove(String player, String gameId, int move, SimpMessagingTemplate messagingTemplate) {
        TicTacToe game = ticTacToeManager.getGame(gameId);
        if (game == null || game.isGameOver()) {
            sendErrorMessage(messagingTemplate, gameId, "Game not found or is already over.");
            return;
        }

        if (game.getGameState().equals(GameState.WAITING_FOR_PLAYER)) {
            sendErrorMessage(messagingTemplate, gameId, "Game is waiting for another player to join.");
            return;
        }

        if (game.getTurn().equals(player)) {
            game.makeMove(player, move);
            TicTacToeMessage stateMessage = new TicTacToeMessage(game);
            stateMessage.setType("game.move");
            messagingTemplate.convertAndSend("/topic/game." + gameId, stateMessage);

            if (game.isGameOver()) {
                TicTacToeMessage overMessage = TicTacToeMessageUtil.fromGame(game);
                overMessage.setType("game.gameOver");
                messagingTemplate.convertAndSend("/topic/game." + gameId, overMessage);
                ticTacToeManager.removeGame(gameId);
            }
        }
    }

    public void playerLeft(String player, SimpMessagingTemplate messagingTemplate) {
        TicTacToe game = ticTacToeManager.getGameByPlayer(player);
        if (game != null) {
            ticTacToeManager.setWinnerByPlayerLeft(player);
            TicTacToeMessage message = TicTacToeMessageUtil.fromGame(game);
            message.setType("game.left");
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), message);
            ticTacToeManager.leaveGame(player);
        }
    }

    private void sendErrorMessage(SimpMessagingTemplate messagingTemplate, String gameId, String content) {
        TicTacToeMessage errorMessage = new TicTacToeMessage();
        errorMessage.setType("error");
        errorMessage.setContent(content);
        messagingTemplate.convertAndSend("/topic/game." + gameId, errorMessage);
    }
}
