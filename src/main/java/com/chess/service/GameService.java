package com.chess.service;

import com.chess.engine.ChessEngine;
import com.chess.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game session manager.
 *
 * FIDE rules tracked here:
 *  Art 3.8b  – castling rights (king/rook moved or rook captured)
 *  Art 5.2a  – stalemate
 *  Art 5.2b/9.6 – dead position (insufficient material)
 *  Art 5.1   – checkmate
 *  Art 9.1   – draw by agreement
 *  Art 9.2   – threefold repetition
 *  Art 9.3   – fifty-move rule (half-move clock)
 *  Art 3.7d  – en passant target tracking
 *  Appendix C – SAN move history
 */
@Service
public class GameService {

    private final Map<String, GameData> games = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────── Internal state ──

    private static class GameData {
        Piece[][] board;
        Color currentTurn;
        // En-passant target square (Art 3.7d): the square the capturing pawn lands on
        int[] enPassantTarget;
        // Castling rights (Art 3.8b)
        boolean wCK = true, wCQ = true, bCK = true, bCQ = true;
        // Move history in SAN
        List<String> moveHistory = new ArrayList<>();
        // Position keys for threefold repetition (Art 9.2)
        Map<String, Integer> positionCount = new HashMap<>();
        // Half-move clock for 50-move rule (Art 9.3)
        int halfMoveClock = 0;
        // Full move number
        int fullMoveNumber = 1;
        String status = "ACTIVE";
        String winner = null;
        // Draw offer tracking (Art 9.1)
        boolean drawOfferedByWhite = false;
        boolean drawOfferedByBlack = false;
    }

    // ─────────────────────────────────────────────── Public API methods ──

    public GameState newGame() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        GameData d = new GameData();
        d.board = ChessEngine.createInitialBoard();
        d.currentTurn = Color.WHITE;
        recordPosition(d);
        games.put(id, d);
        return buildState(id, d);
    }

    public GameState getGame(String gameId) {
        GameData d = games.get(gameId);
        return d == null ? null : buildState(gameId, d);
    }

    public GameState getValidMoves(String gameId, int row, int col) {
        GameData d = games.get(gameId);
        if (d == null) return null;
        GameState state = buildState(gameId, d);
        Piece piece = d.board[row][col];
        if (piece == null || piece.getColor() != d.currentTurn) {
            state.setValidMovesForSelected(new ArrayList<>());
            return state;
        }
        List<int[]> moves = ChessEngine.getLegalMoves(d.board, row, col, d.enPassantTarget, ck(d), cq(d));
        state.setValidMovesForSelected(moves);
        return state;
    }

    /**
     * Execute a move.
     * Returns updated state, or null if gameId unknown.
     * Returns current state unchanged if move is illegal.
     */
    public GameState makeMove(String gameId, Move move) {
        GameData d = games.get(gameId);
        if (d == null) return null;
        if (!d.status.equals("ACTIVE") && !d.status.equals("CHECK")) return buildState(gameId, d);

        int fr = move.getFromRow(), fc = move.getFromCol();
        int tr = move.getToRow(),   tc = move.getToCol();
        Piece piece = d.board[fr][fc];
        if (piece == null || piece.getColor() != d.currentTurn) return buildState(gameId, d);

        List<int[]> legal = ChessEngine.getLegalMoves(d.board, fr, fc, d.enPassantTarget, ck(d), cq(d));
        if (legal.stream().noneMatch(m -> m[0] == tr && m[1] == tc)) return buildState(gameId, d);

        // ── Take a snapshot BEFORE move for SAN generation ──────────────
        Piece[][] boardBefore = ChessEngine.copyBoard(d.board);
        int[] epBefore  = d.enPassantTarget;
        boolean ckBefore = ck(d), cqBefore = cq(d);

        // ── Compute new en-passant target (Art 3.7b/d) ──────────────────
        int[] newEp = null;
        if (piece.getType() == PieceType.PAWN && Math.abs(tr - fr) == 2)
            newEp = new int[]{(fr + tr) / 2, fc};

        // ── Apply the move ───────────────────────────────────────────────
        ChessEngine.applyMoveToBoard(d.board, fr, fc, tr, tc, d.enPassantTarget, move.getPromotion());
        d.enPassantTarget = newEp;

        // ── Update castling rights (Art 3.8b1) ──────────────────────────
        if (piece.getType() == PieceType.KING) {
            if (d.currentTurn == Color.WHITE) { d.wCK = false; d.wCQ = false; }
            else                              { d.bCK = false; d.bCQ = false; }
        }
        if (piece.getType() == PieceType.ROOK) {
            if (fr == 7 && fc == 7) d.wCK = false;
            if (fr == 7 && fc == 0) d.wCQ = false;
            if (fr == 0 && fc == 7) d.bCK = false;
            if (fr == 0 && fc == 0) d.bCQ = false;
        }
        // If a rook is captured its right is also lost
        if (tr == 7 && tc == 7) d.wCK = false;
        if (tr == 7 && tc == 0) d.wCQ = false;
        if (tr == 0 && tc == 7) d.bCK = false;
        if (tr == 0 && tc == 0) d.bCQ = false;

        // ── Half-move clock (Art 9.3) ────────────────────────────────────
        boolean isCapture = boardBefore[tr][tc] != null
            || (piece.getType() == PieceType.PAWN && epBefore != null && tr == epBefore[0] && tc == epBefore[1]);
        if (piece.getType() == PieceType.PAWN || isCapture)
            d.halfMoveClock = 0;
        else
            d.halfMoveClock++;

        // ── Full-move counter ────────────────────────────────────────────
        if (d.currentTurn == Color.BLACK) d.fullMoveNumber++;

        // ── SAN notation (Appendix C) ────────────────────────────────────
        Color nextTurn = d.currentTurn.opposite();
        boolean nextCK = (nextTurn == Color.WHITE) ? d.wCK : d.bCK;
        boolean nextCQ = (nextTurn == Color.WHITE) ? d.wCQ : d.bCQ;

        String san = ChessEngine.toSAN(
            boardBefore, fr, fc, tr, tc, move.getPromotion(),
            epBefore, ckBefore, cqBefore,
            d.board, nextTurn, d.enPassantTarget, nextCK, nextCQ
        );
        d.moveHistory.add(san);

        // ── Switch turn ──────────────────────────────────────────────────
        d.currentTurn = nextTurn;
        // A draw offer made last turn is invalidated once opponent moves (Art 9.1)
        if (d.currentTurn == Color.WHITE) d.drawOfferedByBlack = false;
        else                              d.drawOfferedByWhite = false;

        // ── Evaluate game status ─────────────────────────────────────────
        evaluateStatus(d);

        return buildState(gameId, d);
    }

    /**
     * Art 9.1 – Offer a draw. The offer is valid until opponent accepts/rejects/moves.
     */
    public GameState offerDraw(String gameId, String colorStr) {
        GameData d = games.get(gameId);
        if (d == null) return null;
        if (colorStr.equalsIgnoreCase("WHITE")) d.drawOfferedByWhite = true;
        else                                     d.drawOfferedByBlack = true;
        return buildState(gameId, d);
    }

    /**
     * Art 9.1 – Accept a pending draw offer.
     */
    public GameState acceptDraw(String gameId, String colorStr) {
        GameData d = games.get(gameId);
        if (d == null) return null;
        boolean whiteAccepts = colorStr.equalsIgnoreCase("WHITE");
        boolean offerPending = whiteAccepts ? d.drawOfferedByBlack : d.drawOfferedByWhite;
        if (offerPending) {
            d.status = "DRAW_AGREEMENT";
            d.drawOfferedByWhite = false;
            d.drawOfferedByBlack = false;
        }
        return buildState(gameId, d);
    }

    /**
     * Art 5.1b – Resign.
     */
    public GameState resign(String gameId, String colorStr) {
        GameData d = games.get(gameId);
        if (d == null) return null;
        d.status   = "RESIGNED";
        d.winner   = colorStr.equalsIgnoreCase("WHITE") ? "BLACK" : "WHITE";
        return buildState(gameId, d);
    }

    // ──────────────────────────────────────────────────── Status engine ──

    private void evaluateStatus(GameData d) {
        Color next = d.currentTurn;
        boolean nextCK = ck(d), nextCQ = cq(d);

        boolean inCheck  = ChessEngine.isInCheck(d.board, next);
        boolean hasLegal = ChessEngine.hasAnyLegalMoves(d.board, next, d.enPassantTarget, nextCK, nextCQ);

        // Art 5.1 – checkmate
        if (!hasLegal && inCheck) {
            d.status = "CHECKMATE";
            d.winner = next.opposite().name();
            return;
        }
        // Art 5.2a – stalemate
        if (!hasLegal) {
            d.status = "STALEMATE";
            return;
        }
        // Art 5.2b / 9.6 – dead position
        if (ChessEngine.isDeadPosition(d.board)) {
            d.status = "DRAW_INSUFFICIENT_MATERIAL";
            return;
        }
        // Art 9.3 – fifty-move rule
        if (d.halfMoveClock >= 100) { // 50 moves each = 100 half-moves
            d.status = "DRAW_FIFTY_MOVES";
            return;
        }
        // Art 9.2 – threefold repetition
        recordPosition(d);
        String key = ChessEngine.positionKey(d.board, d.currentTurn, d.wCK, d.wCQ, d.bCK, d.bCQ, d.enPassantTarget);
        if (d.positionCount.getOrDefault(key, 0) >= 3) {
            d.status = "DRAW_THREEFOLD";
            return;
        }
        // Active or check
        d.status = inCheck ? "CHECK" : "ACTIVE";
    }

    private void recordPosition(GameData d) {
        String key = ChessEngine.positionKey(d.board, d.currentTurn, d.wCK, d.wCQ, d.bCK, d.bCQ, d.enPassantTarget);
        d.positionCount.merge(key, 1, Integer::sum);
    }

    // ──────────────────────────────────────────────── Helper methods ──

    private boolean ck(GameData d) { return d.currentTurn == Color.WHITE ? d.wCK : d.bCK; }
    private boolean cq(GameData d) { return d.currentTurn == Color.WHITE ? d.wCQ : d.bCQ; }

    private GameState buildState(String gameId, GameData d) {
        GameState s = new GameState();
        s.setGameId(gameId);
        s.setBoard(ChessEngine.copyBoard(d.board));
        s.setCurrentTurn(d.currentTurn);
        s.setStatus(d.status);
        s.setMoveHistory(new ArrayList<>(d.moveHistory));
        s.setEnPassantTarget(d.enPassantTarget);
        s.setWhiteCanCastleKing(d.wCK);
        s.setWhiteCanCastleQueen(d.wCQ);
        s.setBlackCanCastleKing(d.bCK);
        s.setBlackCanCastleQueen(d.bCQ);
        s.setWinner(d.winner);
        s.setDrawOfferedByWhite(d.drawOfferedByWhite);
        s.setDrawOfferedByBlack(d.drawOfferedByBlack);
        s.setHalfMoveClock(d.halfMoveClock);
        s.setFullMoveNumber(d.fullMoveNumber);
        if (!d.moveHistory.isEmpty())
            s.setLastMove(d.moveHistory.get(d.moveHistory.size() - 1));
        return s;
    }
}
