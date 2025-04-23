package com.mohammad.tictactoewebsocket.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

@Controller
public class TicTacToeController {


    @GetMapping("/game")
    public ModelAndView game() {
        ModelAndView modelAndView = new ModelAndView("game"); // game.html
        String[][] board = new String[3][3];
        Arrays.stream(board).forEach(row -> Arrays.fill(row, " "));
        modelAndView.addObject("board", board);
        return modelAndView;
    }
}
