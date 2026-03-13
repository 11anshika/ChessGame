package com.chess.model;

import java.util.List;

public class GameState {
    private String gameId;
    private Piece[][] board;
    private Color currentTurn;
    private String status;
    private List<String> moveHistory;
    private int[] enPassantTarget;
    private boolean whiteCanCastleKing;
    private boolean whiteCanCastleQueen;
    private boolean blackCanCastleKing;
    private boolean blackCanCastleQueen;
    private String lastMove;
    private List<int[]> validMovesForSelected;
    private String winner;
    private boolean drawOfferedByWhite;
    private boolean drawOfferedByBlack;
    private int halfMoveClock;
    private int fullMoveNumber;

    public GameState() {}

    public String getGameId() { return gameId; }
    public void setGameId(String v) { this.gameId = v; }
    public Piece[][] getBoard() { return board; }
    public void setBoard(Piece[][] v) { this.board = v; }
    public Color getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(Color v) { this.currentTurn = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public List<String> getMoveHistory() { return moveHistory; }
    public void setMoveHistory(List<String> v) { this.moveHistory = v; }
    public int[] getEnPassantTarget() { return enPassantTarget; }
    public void setEnPassantTarget(int[] v) { this.enPassantTarget = v; }
    public boolean isWhiteCanCastleKing() { return whiteCanCastleKing; }
    public void setWhiteCanCastleKing(boolean v) { this.whiteCanCastleKing = v; }
    public boolean isWhiteCanCastleQueen() { return whiteCanCastleQueen; }
    public void setWhiteCanCastleQueen(boolean v) { this.whiteCanCastleQueen = v; }
    public boolean isBlackCanCastleKing() { return blackCanCastleKing; }
    public void setBlackCanCastleKing(boolean v) { this.blackCanCastleKing = v; }
    public boolean isBlackCanCastleQueen() { return blackCanCastleQueen; }
    public void setBlackCanCastleQueen(boolean v) { this.blackCanCastleQueen = v; }
    public String getLastMove() { return lastMove; }
    public void setLastMove(String v) { this.lastMove = v; }
    public List<int[]> getValidMovesForSelected() { return validMovesForSelected; }
    public void setValidMovesForSelected(List<int[]> v) { this.validMovesForSelected = v; }
    public String getWinner() { return winner; }
    public void setWinner(String v) { this.winner = v; }
    public boolean isDrawOfferedByWhite() { return drawOfferedByWhite; }
    public void setDrawOfferedByWhite(boolean v) { this.drawOfferedByWhite = v; }
    public boolean isDrawOfferedByBlack() { return drawOfferedByBlack; }
    public void setDrawOfferedByBlack(boolean v) { this.drawOfferedByBlack = v; }
    public int getHalfMoveClock() { return halfMoveClock; }
    public void setHalfMoveClock(int v) { this.halfMoveClock = v; }
    public int getFullMoveNumber() { return fullMoveNumber; }
    public void setFullMoveNumber(int v) { this.fullMoveNumber = v; }
}
