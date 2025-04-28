package com.mohammad.tictactoewebsocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for starting the Tic-Tac-Toe games.
 */
@Controller
public class HomeController {

    @GetMapping("/index")
    public String home() {
        return "index";
    }

}
