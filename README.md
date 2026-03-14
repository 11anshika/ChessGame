https://chessgame-nx2x.onrender.com/
# ♟ FIDE Chess — Full Stack Java

A complete chess game implementing the **official FIDE Laws of Chess** (79th FIDE Congress, Dresden 2008).

---

## Requirements
- Java 17+
- Maven 3.6+

## Run
```bash
cd chess
mvn spring-boot:run
# Open http://localhost:8080
```

---

## FIDE Rules Implemented

| Rule | Article | Description |
|------|---------|-------------|
| Objectives | Art 1 | White moves first, alternating turns |
| Board setup | Art 2 | Standard initial position |
| Piece moves | Art 3.2–3.6 | Bishop, Rook, Queen, Knight, King |
| Pawn forward | Art 3.7a | One square forward if unoccupied |
| Pawn double push | Art 3.7b | Two squares from starting rank |
| Pawn capture | Art 3.7c | Diagonal capture of opponent piece |
| En passant | Art 3.7d | Only legal on the immediately following move |
| Pawn promotion | Art 3.7e | Must promote to Q/R/B/N; effect immediate |
| Castling | Art 3.8a | Kingside and queenside |
| Castling rights | Art 3.8b1 | Lost when king or rook moves |
| Castling prevention | Art 3.8b2 | Not while in check, not through attacked square |
| Check filter | Art 3.9 | No move may expose own king |
| Checkmate | Art 5.1 | King in check with no legal move |
| Stalemate | Art 5.2a | No legal moves, king not in check |
| Dead position | Art 5.2b/9.6 | Insufficient material → immediate draw |
| Draw agreement | Art 9.1 | Either player may offer; opponent must accept |
| Threefold repetition | Art 9.2 | Same position (incl. rights+ep) ≥3 times |
| Fifty-move rule | Art 9.3 | 50 moves each without pawn move or capture |
| Resign | Art 5.1b | Either player may resign |
| SAN notation | App. C | Move history in Standard Algebraic Notation |

---

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chess/new` | New game |
| GET | `/api/chess/{id}` | Get game state |
| GET | `/api/chess/{id}/moves/{r}/{c}` | Legal moves for piece |
| POST | `/api/chess/{id}/move` | Make a move |
| POST | `/api/chess/{id}/draw/offer` | Offer draw (Art 9.1) |
| POST | `/api/chess/{id}/draw/accept` | Accept draw offer |
| POST | `/api/chess/{id}/resign` | Resign (Art 5.1b) |

---

## Project Structure
```
chess/
├── pom.xml
└── src/main/java/com/chess/
    ├── ChessApplication.java
    ├── model/         Color, PieceType, Piece, Move, GameState
    ├── engine/        ChessEngine.java  (FIDE rules + SAN)
    ├── service/       GameService.java  (session management)
    └── controller/    ChessController.java  (REST API)
    resources/static/index.html  (UI)
```
