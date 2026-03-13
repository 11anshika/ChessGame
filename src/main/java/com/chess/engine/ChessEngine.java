package com.chess.engine;

import com.chess.model.*;
import java.util.*;

/**
 * FIDE Laws of Chess – Complete Engine
 * Art 1    – Objectives, alternating play, white moves first
 * Art 2    – Initial board position
 * Art 3.1  – Cannot land on own piece; capture removes opponent piece
 * Art 3.2  – Bishop: diagonals only
 * Art 3.3  – Rook: file/rank only
 * Art 3.4  – Queen: file, rank, diagonal
 * Art 3.5  – Sliding pieces cannot jump over intervening pieces
 * Art 3.6  – Knight: nearest squares not on same rank/file/diagonal
 * Art 3.7a – Pawn: one square forward (unoccupied)
 * Art 3.7b – Pawn: two squares from starting rank (both unoccupied)
 * Art 3.7c – Pawn: diagonal capture
 * Art 3.7d – En passant: only legal on the immediately following move
 * Art 3.7e – Pawn promotion: must promote to Q/R/B/N; immediate effect
 * Art 3.8  – King normal moves + castling
 * Art 3.8b – Castling rights lost if king/rook moves; prevented if check/attacked square
 * Art 3.9  – No move may leave/expose own king in check
 * Art 5.1  – Checkmate
 * Art 5.2a – Stalemate
 * Art 5.2b/9.6 – Dead position / insufficient material
 * Art 9.2  – Threefold repetition
 * Art 9.3  – Fifty-move rule
 * Appendix C – Standard Algebraic Notation
 */
public class ChessEngine {

    public static Piece[][] createInitialBoard() {
        Piece[][] b = new Piece[8][8];
        b[0][0] = new Piece(PieceType.ROOK,   Color.BLACK);
        b[0][1] = new Piece(PieceType.KNIGHT, Color.BLACK);
        b[0][2] = new Piece(PieceType.BISHOP, Color.BLACK);
        b[0][3] = new Piece(PieceType.QUEEN,  Color.BLACK);
        b[0][4] = new Piece(PieceType.KING,   Color.BLACK);
        b[0][5] = new Piece(PieceType.BISHOP, Color.BLACK);
        b[0][6] = new Piece(PieceType.KNIGHT, Color.BLACK);
        b[0][7] = new Piece(PieceType.ROOK,   Color.BLACK);
        for (int c = 0; c < 8; c++) b[1][c] = new Piece(PieceType.PAWN, Color.BLACK);
        b[7][0] = new Piece(PieceType.ROOK,   Color.WHITE);
        b[7][1] = new Piece(PieceType.KNIGHT, Color.WHITE);
        b[7][2] = new Piece(PieceType.BISHOP, Color.WHITE);
        b[7][3] = new Piece(PieceType.QUEEN,  Color.WHITE);
        b[7][4] = new Piece(PieceType.KING,   Color.WHITE);
        b[7][5] = new Piece(PieceType.BISHOP, Color.WHITE);
        b[7][6] = new Piece(PieceType.KNIGHT, Color.WHITE);
        b[7][7] = new Piece(PieceType.ROOK,   Color.WHITE);
        for (int c = 0; c < 8; c++) b[6][c] = new Piece(PieceType.PAWN, Color.WHITE);
        return b;
    }

    public static Piece[][] copyBoard(Piece[][] board) {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != null) copy[r][c] = board[r][c].copy();
        return copy;
    }

    public static boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    public static List<int[]> getPseudoLegalMoves(Piece[][] board, int row, int col,
                                                   int[] ep, boolean ck, boolean cq) {
        List<int[]> moves = new ArrayList<>();
        Piece piece = board[row][col];
        if (piece == null) return moves;
        Color color = piece.getColor();
        switch (piece.getType()) {
            case PAWN   -> pawnMoves(board, row, col, color, moves, ep);
            case KNIGHT -> knightMoves(board, row, col, color, moves);
            case BISHOP -> sliding(board, row, col, color, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
            case ROOK   -> sliding(board, row, col, color, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
            case QUEEN  -> {
                sliding(board, row, col, color, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
                sliding(board, row, col, color, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
            }
            case KING   -> kingMoves(board, row, col, color, moves, ck, cq);
        }
        return moves;
    }

    private static void pawnMoves(Piece[][] board, int row, int col, Color color,
                                  List<int[]> moves, int[] ep) {
        int dir = (color == Color.WHITE) ? -1 : 1;
        int startRow = (color == Color.WHITE) ? 6 : 1;
        int nr = row + dir;
        if (inBounds(nr, col) && board[nr][col] == null) {
            moves.add(new int[]{nr, col});
            int nr2 = row + 2*dir;
            if (row == startRow && inBounds(nr2, col) && board[nr2][col] == null)
                moves.add(new int[]{nr2, col});
        }
        for (int dc : new int[]{-1, 1}) {
            int nc = col + dc;
            if (!inBounds(nr, nc)) continue;
            if (board[nr][nc] != null && board[nr][nc].getColor() != color)
                moves.add(new int[]{nr, nc});
            if (ep != null && ep[0] == nr && ep[1] == nc)
                moves.add(new int[]{nr, nc});
        }
    }

    private static void knightMoves(Piece[][] board, int row, int col, Color color, List<int[]> moves) {
        for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int nr = row+d[0], nc = col+d[1];
            if (inBounds(nr,nc) && (board[nr][nc]==null || board[nr][nc].getColor()!=color))
                moves.add(new int[]{nr,nc});
        }
    }

    private static void sliding(Piece[][] board, int row, int col, Color color,
                                 List<int[]> moves, int[][] dirs) {
        for (int[] dir : dirs) {
            int nr = row+dir[0], nc = col+dir[1];
            while (inBounds(nr, nc)) {
                if (board[nr][nc] == null) {
                    moves.add(new int[]{nr, nc});
                } else {
                    if (board[nr][nc].getColor() != color) moves.add(new int[]{nr, nc});
                    break;
                }
                nr += dir[0]; nc += dir[1];
            }
        }
    }

    private static void kingMoves(Piece[][] board, int row, int col, Color color,
                                   List<int[]> moves, boolean ck, boolean cq) {
        for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr = row+d[0], nc = col+d[1];
            if (inBounds(nr,nc) && (board[nr][nc]==null || board[nr][nc].getColor()!=color))
                moves.add(new int[]{nr, nc});
        }
        // Art 3.8 – Castling: squares between king and rook must be unoccupied
        if (ck && board[row][5]==null && board[row][6]==null) moves.add(new int[]{row,6});
        if (cq && board[row][1]==null && board[row][2]==null && board[row][3]==null) moves.add(new int[]{row,2});
    }

    public static boolean isSquareAttacked(Piece[][] board, int row, int col, Color byColor) {
        for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
            int nr=row+d[0], nc=col+d[1];
            if (inBounds(nr,nc) && isPiece(board,nr,nc,byColor,PieceType.KNIGHT)) return true;
        }
        for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            int nr=row+dir[0], nc=col+dir[1];
            while (inBounds(nr,nc)) {
                if (board[nr][nc]!=null) {
                    if (board[nr][nc].getColor()==byColor &&
                        (board[nr][nc].getType()==PieceType.ROOK || board[nr][nc].getType()==PieceType.QUEEN)) return true;
                    break;
                }
                nr+=dir[0]; nc+=dir[1];
            }
        }
        for (int[] dir : new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}) {
            int nr=row+dir[0], nc=col+dir[1];
            while (inBounds(nr,nc)) {
                if (board[nr][nc]!=null) {
                    if (board[nr][nc].getColor()==byColor &&
                        (board[nr][nc].getType()==PieceType.BISHOP || board[nr][nc].getType()==PieceType.QUEEN)) return true;
                    break;
                }
                nr+=dir[0]; nc+=dir[1];
            }
        }
        int pawnRow = (byColor==Color.WHITE) ? row+1 : row-1;
        for (int dc : new int[]{-1,1}) {
            int nc=col+dc;
            if (inBounds(pawnRow,nc) && isPiece(board,pawnRow,nc,byColor,PieceType.PAWN)) return true;
        }
        for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr=row+d[0], nc=col+d[1];
            if (inBounds(nr,nc) && isPiece(board,nr,nc,byColor,PieceType.KING)) return true;
        }
        return false;
    }

    private static boolean isPiece(Piece[][] b, int r, int c, Color color, PieceType type) {
        return b[r][c]!=null && b[r][c].getColor()==color && b[r][c].getType()==type;
    }

    public static int[] findKing(Piece[][] board, Color color) {
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) if (isPiece(board,r,c,color,PieceType.KING)) return new int[]{r,c};
        return null;
    }

    public static boolean isInCheck(Piece[][] board, Color color) {
        int[] king = findKing(board, color);
        return king!=null && isSquareAttacked(board, king[0], king[1], color.opposite());
    }

    public static List<int[]> getLegalMoves(Piece[][] board, int row, int col,
                                            int[] ep, boolean ck, boolean cq) {
        Piece piece = board[row][col];
        if (piece==null) return new ArrayList<>();
        Color color = piece.getColor();
        List<int[]> legal = new ArrayList<>();
        for (int[] move : getPseudoLegalMoves(board,row,col,ep,ck,cq)) {
            int toR=move[0], toC=move[1];
            // Art 3.8b2 – castling: not while in check, not through attacked square
            if (piece.getType()==PieceType.KING && Math.abs(toC-col)==2) {
                if (isInCheck(board, color)) continue;
                int midCol = (col+toC)/2;
                Piece[][] sim = copyBoard(board);
                sim[row][midCol] = sim[row][col]; sim[row][col] = null;
                if (isSquareAttacked(sim, row, midCol, color.opposite())) continue;
            }
            // Art 3.9 – own king must not be in check after move
            Piece[][] sim = copyBoard(board);
            applyMoveToBoard(sim, row, col, toR, toC, ep, null);
            if (!isInCheck(sim, color)) legal.add(move);
        }
        return legal;
    }

    public static void applyMoveToBoard(Piece[][] board, int fromR, int fromC, int toR, int toC,
                                        int[] ep, String promotion) {
        Piece piece = board[fromR][fromC];
        if (piece==null) return;
        // En passant capture
        if (piece.getType()==PieceType.PAWN && ep!=null && toR==ep[0] && toC==ep[1]) {
            int captureRow = (piece.getColor()==Color.WHITE) ? toR+1 : toR-1;
            board[captureRow][toC] = null;
        }
        // Castling: move the rook
        if (piece.getType()==PieceType.KING && Math.abs(toC-fromC)==2) {
            if (toC==6) { board[fromR][5]=board[fromR][7]; board[fromR][7]=null; if(board[fromR][5]!=null) board[fromR][5].setHasMoved(true); }
            else        { board[fromR][3]=board[fromR][0]; board[fromR][0]=null; if(board[fromR][3]!=null) board[fromR][3].setHasMoved(true); }
        }
        board[toR][toC] = piece; board[fromR][fromC] = null; piece.setHasMoved(true);
        // Art 3.7e – pawn promotion
        if (piece.getType()==PieceType.PAWN && (toR==0||toR==7)) {
            PieceType pt = PieceType.QUEEN;
            if (promotion!=null) { try { pt=PieceType.valueOf(promotion.toUpperCase()); } catch(Exception ignored){} }
            board[toR][toC] = new Piece(pt, piece.getColor());
            board[toR][toC].setHasMoved(true);
        }
    }

    public static boolean hasAnyLegalMoves(Piece[][] board, Color color, int[] ep, boolean ck, boolean cq) {
        for (int r=0;r<8;r++) for (int c=0;c<8;c++)
            if (board[r][c]!=null && board[r][c].getColor()==color)
                if (!getLegalMoves(board,r,c,ep,ck,cq).isEmpty()) return true;
        return false;
    }

    public static boolean isDeadPosition(Piece[][] board) {
        List<Piece> pieces = new ArrayList<>();
        List<int[]> sqs    = new ArrayList<>();
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) if(board[r][c]!=null){pieces.add(board[r][c]);sqs.add(new int[]{r,c});}
        long w = pieces.stream().filter(p->p.getColor()==Color.WHITE).count();
        long b = pieces.stream().filter(p->p.getColor()==Color.BLACK).count();
        if (w==1&&b==1) return true;
        if ((w==2&&b==1)||(w==1&&b==2)) {
            boolean minor = pieces.stream().filter(p->p.getType()!=PieceType.KING)
                .allMatch(p->p.getType()==PieceType.BISHOP||p.getType()==PieceType.KNIGHT);
            if (minor) return true;
        }
        if (w==2&&b==2) {
            List<Integer> bsq = new ArrayList<>(); boolean only=true;
            for (int i=0;i<pieces.size();i++) {
                Piece p=pieces.get(i); if(p.getType()==PieceType.KING) continue;
                if(p.getType()!=PieceType.BISHOP){only=false;break;}
                bsq.add((sqs.get(i)[0]+sqs.get(i)[1])%2);
            }
            if (only&&bsq.size()==2&&bsq.get(0).equals(bsq.get(1))) return true;
        }
        return false;
    }

    public static String positionKey(Piece[][] board, Color turn,
                                     boolean wck, boolean wcq, boolean bck, boolean bcq, int[] ep) {
        StringBuilder sb = new StringBuilder();
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
            Piece p=board[r][c];
            sb.append(p==null?".." : ""+p.getColor().name().charAt(0)+p.getType().name().charAt(0));
        }
        sb.append(turn==Color.WHITE?'W':'B');
        sb.append(wck?'K':'-').append(wcq?'Q':'-').append(bck?'k':'-').append(bcq?'q':'-');
        sb.append(ep!=null?(char)('a'+ep[1]):'-');
        return sb.toString();
    }

    public static String toSAN(Piece[][] before, int fromR, int fromC, int toR, int toC,
                                String promotion, int[] ep, boolean ckB, boolean cqB,
                                Piece[][] after, Color nextTurn, int[] nextEp, boolean nck, boolean ncq) {
        Piece piece = before[fromR][fromC];
        if (piece==null) return "??";
        boolean isCapture = (before[toR][toC]!=null)
            || (piece.getType()==PieceType.PAWN && ep!=null && toR==ep[0] && toC==ep[1]);
        if (piece.getType()==PieceType.KING && Math.abs(toC-fromC)==2)
            return ((toC==6)?"O-O":"O-O-O") + checkSuffix(after,nextTurn,nextEp,nck,ncq);
        StringBuilder san = new StringBuilder();
        if (piece.getType()!=PieceType.PAWN) san.append(pieceChar(piece.getType()));
        if (piece.getType()==PieceType.PAWN) { if(isCapture) san.append((char)('a'+fromC)); }
        else disambiguate(san,before,fromR,fromC,toR,toC,piece,ep,ckB,cqB);
        if (isCapture) san.append('x');
        san.append((char)('a'+toC)).append((char)('8'-toR));
        if (piece.getType()==PieceType.PAWN&&(toR==0||toR==7)) {
            san.append('=');
            san.append(promotion!=null?pieceChar(PieceType.valueOf(promotion.toUpperCase())):'Q');
        }
        if (piece.getType()==PieceType.PAWN&&isCapture&&before[toR][toC]==null) san.append(" e.p.");
        san.append(checkSuffix(after,nextTurn,nextEp,nck,ncq));
        return san.toString();
    }

    private static void disambiguate(StringBuilder san, Piece[][] board,
                                     int fromR, int fromC, int toR, int toC,
                                     Piece piece, int[] ep, boolean ck, boolean cq) {
        List<int[]> amb = new ArrayList<>();
        for (int r=0;r<8;r++) for (int c=0;c<8;c++) {
            if(r==fromR&&c==fromC) continue;
            Piece o=board[r][c];
            if(o==null||o.getType()!=piece.getType()||o.getColor()!=piece.getColor()) continue;
            if(getLegalMoves(board,r,c,ep,ck,cq).stream().anyMatch(m->m[0]==toR&&m[1]==toC)) amb.add(new int[]{r,c});
        }
        if (amb.isEmpty()) return;
        boolean sf=amb.stream().anyMatch(a->a[1]==fromC), sr=amb.stream().anyMatch(a->a[0]==fromR);
        if (!sf) san.append((char)('a'+fromC));
        else if (!sr) san.append((char)('8'-fromR));
        else { san.append((char)('a'+fromC)); san.append((char)('8'-fromR)); }
    }

    private static String checkSuffix(Piece[][] board, Color turn, int[] ep, boolean ck, boolean cq) {
        if (!isInCheck(board,turn)) return "";
        return hasAnyLegalMoves(board,turn,ep,ck,cq)?"+":"#";
    }

    public static char pieceChar(PieceType t) {
        return switch(t){case KING->'K';case QUEEN->'Q';case ROOK->'R';case BISHOP->'B';case KNIGHT->'N';default->' ';};
    }

    public static String toCoord(int row, int col) { return ""+(char)('a'+col)+(8-row); }
}
