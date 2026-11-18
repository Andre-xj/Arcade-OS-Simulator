package game.snake;

/**
 * C2 / B3: Callback interface that lets the manual simulator (SegmentationPanel)
 * listen for a "review in simulator" action from the snake game's end screen.
 *
 * When the snake game's Game Over / Level Clear card shows the "REVIEW IN SIM"
 * button, activating it calls this listener so the manual simulator can jump
 * directly to the segment, offset, and limit that caused the last fault.
 */
public interface SnakeReviewListener {

    /**
     * Called when the player activates "REVIEW IN SIM" on the end screen.
     *
     * @param segmentNumber  1-based segment number (matches the table's #N column)
     * @param base           real base address of the faulted segment
     * @param limit          limit (size) of the faulted segment
     * @param offset         the offset that caused the fault (may be >= limit)
     */
    void reviewInSimulator(int segmentNumber, int base, int limit, int offset);
}
