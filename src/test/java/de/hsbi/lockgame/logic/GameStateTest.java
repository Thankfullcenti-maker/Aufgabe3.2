package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameState Struktur- und Logiktests")
class GameStateTest {

    private Level testLevel;
    private Snake startSnake;
    private Pin testPin;
    private List<Pin> pinsFixture;

    @BeforeEach
    void setUp() {
        Position startPos = new Position(2, 2);
        CellType[][] cells = new CellType[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cells[i][j] = CellType.EMPTY;
            }
        }
        testPin = new Pin(new Position(2, 1), de.hsbi.lockgame.model.Pin.State.LOW, Direction.UP);
        pinsFixture = List.of(testPin);
        testLevel = new Level(5, 5, cells, pinsFixture, startPos);
        startSnake = new Snake(List.of(startPos));
    }

    @Test
    @DisplayName("1. Initialzustand: Werte müssen korrekt gesetzt sein")
    void testInitialState() {
        GameState state = new GameState(testLevel, startSnake, pinsFixture, GameState.Status.RUNNING, Direction.NONE);
        assertAll(
            () -> assertEquals(GameState.Status.RUNNING, state.status()),
            () -> assertEquals(Direction.NONE, state.pendingDirection()),
            () -> assertEquals(startSnake, state.snake()),
            () -> assertEquals(1, state.pins().size())
        );
    }

    @Test
    @DisplayName("2. Bewegung: Ohne Richtung (NONE) darf sich nichts verändern")
    void testTickWithoutDirectionChangesNothing() {
        GameState state = new GameState(testLevel, startSnake, pinsFixture, GameState.Status.RUNNING, Direction.NONE);
        GameState nextState = state.tick();
        assertEquals(state.snake().head().x(), nextState.snake().head().x());
        assertEquals(state.snake().head().y(), nextState.snake().head().y());
    }

    @Test
    @DisplayName("3. Bewegung: Einfache Vorwärtsbewegung verändert Schlangenposition")
    void testSimpleMovement() {
        GameState state = new GameState(testLevel, startSnake, pinsFixture, GameState.Status.RUNNING, Direction.RIGHT);
        GameState nextState = state.tick();
        assertEquals(3, nextState.snake().head().x());
        assertEquals(2, nextState.snake().head().y());
    }

    @Test
    @DisplayName("4. Verlust: Verlassen des Spielfelds führt zu LOST_OUT_OF_BOUNDS")
    void testOutOfBoundsLoss() {
        Snake edgeSnake = new Snake(List.of(new Position(0, 2)));
        GameState state = new GameState(testLevel, edgeSnake, pinsFixture, GameState.Status.RUNNING, Direction.LEFT);
        GameState nextState = state.tick();
        assertEquals(GameState.Status.LOST_OUT_OF_BOUNDS, nextState.status());
    }

    @Test
    @DisplayName("5. Blockade: Wandelement stoppt die Bewegung und setzt Richtung zurück")
    void testWallCollisionBlocksMovement() {
        testLevel.cells()[2][1] = CellType.WALL;
        GameState state = new GameState(testLevel, startSnake, pinsFixture, GameState.Status.RUNNING, Direction.UP);
        GameState nextState = state.tick();
        assertEquals(startSnake.head().x(), nextState.snake().head().x());
        assertEquals(startSnake.head().y(), nextState.snake().head().y());
        assertEquals(Direction.NONE, nextState.pendingDirection());
    }

    @Test
    @DisplayName("6. Pin-Aktivierung: Passende Richtung schaltet Pin auf HIGH")
    void testPinActivation() {
        GameState state = new GameState(testLevel, startSnake, pinsFixture, GameState.Status.RUNNING, Direction.UP);
        GameState nextState = state.tick();
        assertEquals(de.hsbi.lockgame.model.Pin.State.HIGH, nextState.pins().get(0).state());
    }

    @Test
    @DisplayName("7. Pin-Blockade: Falsche Richtung aktiviert Pin nicht und blockiert Bewegung")
    void testPinWrongDirectionBlocksAndDoesNotActivate() {
        Pin wrongPin = new Pin(new Position(2, 1), de.hsbi.lockgame.model.Pin.State.LOW, Direction.DOWN);
        GameState state = new GameState(testLevel, startSnake, List.of(wrongPin), GameState.Status.RUNNING, Direction.UP);
        GameState nextState = state.tick();
        assertEquals(de.hsbi.lockgame.model.Pin.State.LOW, nextState.pins().get(0).state());
        assertEquals(startSnake.head().x(), nextState.snake().head().x());
        assertEquals(startSnake.head().y(), nextState.snake().head().y());
    }

    @Test
    @DisplayName("8. Pin-Kollision: Bereits aktivierter Pin blockiert Schlangenbewegung")
    void testAlreadyActivatedPinBlocksMovement() {
        Pin activePin = new Pin(new Position(2, 1), de.hsbi.lockgame.model.Pin.State.HIGH, Direction.UP);
        GameState state = new GameState(testLevel, startSnake, List.of(activePin), GameState.Status.RUNNING, Direction.UP);
        GameState nextState = state.tick();
        assertEquals(startSnake.head().x(), nextState.snake().head().x());
        assertEquals(startSnake.head().y(), nextState.snake().head().y());
    }

    @Test
    @DisplayName("9. Gewinnbedingung: Aktivierung des letzten Pins führt zum Sieg")
    void testGameWonCondition() {
        GameState state = new GameState(testLevel, startSnake, pinsFixture, GameState.Status.RUNNING, Direction.UP);
        GameState nextState = state.tick();
        assertEquals(GameState.Status.WON, nextState.status());
    }

    @Test
    @DisplayName("10. Verlust: Selbstkollision (Schlange beißt sich selbst)")
    void testSelfCollisionLoss() {
        Position headPos = new Position(2, 2);
        List<Position> bodyPoints = List.of(
            headPos,
            new Position(3, 2),
            new Position(3, 1),
            new Position(2, 1)
        );
        Snake longSnake = new Snake(bodyPoints);
        GameState state = new GameState(testLevel, longSnake, pinsFixture, GameState.Status.RUNNING, Direction.UP);
        GameState nextState = state.tick();
        assertEquals(GameState.Status.LOST_SELF_COLLISION, nextState.status());
    }
}
