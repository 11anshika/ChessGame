package com.chess.model;

public class Piece {
    private PieceType type;
    private Color color;
    private boolean hasMoved;

    public Piece() {}

    public Piece(PieceType type, Color color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public Piece copy() {
        Piece p = new Piece(type, color);
        p.hasMoved = this.hasMoved;
        return p;
    }

    public PieceType getType() { return type; }
    public void setType(PieceType type) { this.type = type; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public boolean isHasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }

    @Override
    public String toString() {
        return color + "_" + type;
    }
}
