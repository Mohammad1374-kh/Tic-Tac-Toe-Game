package com.mohammad.tictactoewebsocket.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * Controller class for handling HTTP requests related to the Tic-Tac-Toe game page.
 */
@Controller
public class TicTacToeController {

    /**
     * Handles GET requests to the "/game" endpoint.
     *
     * Initializes an empty 3x3 Tic-Tac-Toe board and returns the "game" view.
     *
     * @return a ModelAndView object containing the game view and an empty board
     */
    @GetMapping("/game")
    public ModelAndView game() {
        ModelAndView modelAndView = new ModelAndView("game"); // game.html
        String[][] board = new String[3][3];
        Arrays.stream(board).forEach(row -> Arrays.fill(row, " "));
        modelAndView.addObject("board", board);
        return modelAndView;
    }
}
