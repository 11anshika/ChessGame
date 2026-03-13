package com.chess.model;

public class Move {
    private int fromRow, fromCol, toRow, toCol;
    private String promotion; // for pawn promotion: "QUEEN","ROOK","BISHOP","KNIGHT"

    public Move() {}

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    public int getFromRow() { return fromRow; }
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }
    public int getFromCol() { return fromCol; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }
    public int getToRow() { return toRow; }
    public void setToRow(int toRow) { this.toRow = toRow; }
    public int getToCol() { return toCol; }
    public void setToCol(int toCol) { this.toCol = toCol; }
    public String getPromotion() { return promotion; }
    public void setPromotion(String promotion) { this.promotion = promotion; }
}
