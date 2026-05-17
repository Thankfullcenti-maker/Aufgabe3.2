package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Snake;
import de.hsbi.lockgame.ui.GamePanel;

public final class GameEngine {

    private GameState currentGameState;
    private GamePanel gamePanel;

    public GameEngine(Level level) {
        // Initialisiert den ersten Zustand des Spiels
        // Die Schlange startet an der im Level definierten Startposition
        // Verpackt den Startpunkt des Levels in eine Liste für die Schlange
        this.currentGameState = new GameState(
            level,
            new Snake(java.util.List.of(level.snakeStart())),
            level.pins(),
            GameState.Status.RUNNING,
            Direction.NONE
        );

    }

    public GameState state() {
        return this.currentGameState;
    }

    public void setGamePanel(GamePanel panel) {
        this.gamePanel = panel;
    }

    public void update(Direction d) {
        // Richtungsupdate: Erzeugt einen neuen Zustand mit der aktualisierten Richtung
        if (this.currentGameState.status().isRunning()) {
            this.currentGameState = new GameState(
                this.currentGameState.level(),
                this.currentGameState.snake(),
                this.currentGameState.pins(),
                this.currentGameState.status(),
                d
            );
            notifyObserver();
        }
    }

    public void tick() {
        // Berechnet den nächsten Spielschritt über das Datenmodell
        this.currentGameState = this.currentGameState.tick();
        notifyObserver();
    }

    private void notifyObserver() {
        if (this.gamePanel != null) {
            this.gamePanel.update(this.currentGameState);
            this.gamePanel.repaint();
        }
    }
}
