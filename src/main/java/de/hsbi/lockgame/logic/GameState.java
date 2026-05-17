package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;

import java.util.ArrayList;
import java.util.List;

public final class GameState {

    private final Level level;
    private final Snake snake;
    private final List<Pin> pins;
    private final Status status;
    private final Direction pendingDirection;

    public GameState(
        Level level, Snake snake, List<Pin> pins, Status status, Direction pendingDirection) {
        this.level = level;
        this.snake = snake;
        this.pins = List.copyOf(pins); // Unveränderliche Kopie der Liste
        this.status = status;
        this.pendingDirection = pendingDirection;
    }

    // --- Getter ---

    public Level level() {
        return this.level;
    }

    public Snake snake() {
        return this.snake;
    }

    public List<Pin> pins() {
        return this.pins;
    }

    public Status status() {
        return this.status;
    }

    public Direction pendingDirection() {
        return this.pendingDirection;
    }

    // --- Spiellogik ---

    public GameState tick() {
        // Early exit: wenn das Spiel nicht läuft oder keine Blickrichtung gesetzt ist
        if (!this.status.isRunning() || this.pendingDirection == Direction.NONE) {
            return this;
        }

        Position nextHead = this.snake.nextHead(this.pendingDirection);

        // (a) Spielfeld verlassen
        if (!this.level.isInside(nextHead)) {
            return new GameState(this.level, this.snake, this.pins, Status.LOST_OUT_OF_BOUNDS, this.pendingDirection);
        }

        // (b) Wandelement getroffen
        if (this.level.cellAt(nextHead) == CellType.WALL) {
            return new GameState(this.level, this.snake, this.pins, this.status, Direction.NONE);
        }

        // (c) KORREKTUR Selbstkollision: Manueller X/Y-Vergleich für jedes Schlangenglied
        // Da Position kein equals() besitzt, prüfen wir den Körper hier manuell
        for (Position segment : this.snake.body()) {
            if (segment.x() == nextHead.x() && segment.y() == nextHead.y()) {
                return new GameState(this.level, this.snake, this.pins, Status.LOST_SELF_COLLISION, this.pendingDirection);
            }
        }

        java.util.List<Pin> updatedPins = new java.util.ArrayList<>(this.pins);
        boolean movementBlocked = false;

        // Pins prüfen via manueller X/Y-Koordinaten-Abfrage
        for (int i = 0; i < updatedPins.size(); i++) {
            Pin pin = updatedPins.get(i);

            if (pin.position().x() == nextHead.x() && pin.position().y() == nextHead.y()) {
                if (!pin.state().isSet() && this.pendingDirection == pin.activationDirection()) {
                    updatedPins.set(i, pin.withState(de.hsbi.lockgame.model.Pin.State.HIGH));
                }
                movementBlocked = true;
                break;
            }
        }

        // KORREKTUR Siegbedingung: Prüfen, ob nach der Aktivierung alle Pins gelöst sind
        boolean allPinsWon = true;
        for (Pin pin : updatedPins) {
            if (!pin.state().isSet()) {
                allPinsWon = false;
                break;
            }
        }

        if (allPinsWon) {
            return new GameState(this.level, this.snake, updatedPins, Status.WON, this.pendingDirection);
        }

        if (movementBlocked) {
            return new GameState(this.level, this.snake, updatedPins, this.status, Direction.NONE);
        }

        // Reguläre Bewegung
        Snake updatedSnake = this.snake.grow(this.pendingDirection);

        return new GameState(this.level, updatedSnake, updatedPins, this.status, this.pendingDirection);
    }


    // --- Status Enum ---

    public enum Status {
        RUNNING,
        WON,
        LOST_SELF_COLLISION,
        LOST_OUT_OF_BOUNDS;

        public boolean isRunning() {
            return this == RUNNING;
        }
    }
}
