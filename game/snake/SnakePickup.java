package game.snake;

/** One collectible sitting on a single board cell. */
final class SnakePickup {

    enum Type {
        /** W pellet (yellow "M"): Allocates one cell of heap. Hazard in read-only segments. */
        MALLOC,
        /** R pellet (cyan "R"): Read access pellet. Safe to collect in all segments. */
        READ
    }

    final Type type;
    final int col;
    final int row;

    SnakePickup(Type type, int col, int row) {
        this.type = type;
        this.col = col;
        this.row = row;
    }
}
