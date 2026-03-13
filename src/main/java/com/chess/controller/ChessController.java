package com.chess.controller;

import com.chess.model.*;
import com.chess.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chess")
@CrossOrigin(origins = "*")
public class ChessController {

    @Autowired
    private GameService gameService;

    @PostMapping("/new")
    public ResponseEntity<GameState> newGame() {
        return ResponseEntity.ok(gameService.newGame());
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameState> getGame(@PathVariable String gameId) {
        GameState s = gameService.getGame(gameId);
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }

    @GetMapping("/{gameId}/moves/{row}/{col}")
    public ResponseEntity<GameState> getValidMoves(@PathVariable String gameId,
                                                    @PathVariable int row, @PathVariable int col) {
        GameState s = gameService.getValidMoves(gameId, row, col);
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<GameState> makeMove(@PathVariable String gameId, @RequestBody Move move) {
        GameState s = gameService.makeMove(gameId, move);
        return s == null ? ResponseEntity.badRequest().build() : ResponseEntity.ok(s);
    }

    @PostMapping("/{gameId}/draw/offer")
    public ResponseEntity<GameState> offerDraw(@PathVariable String gameId,
                                                @RequestBody Map<String,String> body) {
        GameState s = gameService.offerDraw(gameId, body.getOrDefault("color","WHITE"));
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }

    @PostMapping("/{gameId}/draw/accept")
    public ResponseEntity<GameState> acceptDraw(@PathVariable String gameId,
                                                 @RequestBody Map<String,String> body) {
        GameState s = gameService.acceptDraw(gameId, body.getOrDefault("color","WHITE"));
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }

    @PostMapping("/{gameId}/resign")
    public ResponseEntity<GameState> resign(@PathVariable String gameId,
                                             @RequestBody Map<String,String> body) {
        GameState s = gameService.resign(gameId, body.getOrDefault("color","WHITE"));
        return s == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(s);
    }
}
