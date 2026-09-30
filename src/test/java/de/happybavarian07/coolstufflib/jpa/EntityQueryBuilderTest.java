package de.happybavarian07.coolstufflib.jpa;

import de.happybavarian07.coolstufflib.jpa.annotations.Column;
import de.happybavarian07.coolstufflib.jpa.annotations.Id;
import de.happybavarian07.coolstufflib.jpa.annotations.PostLoad;
import de.happybavarian07.coolstufflib.jpa.annotations.Table;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.utils.EntityQueryBuilder;
import de.happybavarian07.coolstufflib.jpa.utils.RepositoryProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntityQueryBuilderTest {
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID THIRD = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FOURTH = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Table(name = "notes")
    public static class Note {
        @Id
        private UUID id;
        @Column(name = "title")
        private String title;
        @Column(name = "views")
        private int views;
        private String loadedBy;

        @PostLoad
        void onLoad() {
            loadedBy = "postLoad";
        }
    }

    @Table(name = "")
    public static class Widget {
        @Id
        @Column(name = "widget_id")
        private int widgetId;
        @Column(name = "label")
        private String label;
    }

    public static class Unkeyed {
        @Column(name = "label")
        private String label;
    }

    public interface NoteRepo extends Repository<Note, UUID> {
    }

    private Connection connection;
    private SQLExecutor executor;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        RepositoryController controller = mock(RepositoryController.class);
        when(controller.getConnection("default")).thenReturn(connection);
        executor = new SQLExecutor(controller, null);
        executor.setDefaultConnection("default");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE notes (id TEXT PRIMARY KEY, title TEXT, views INT)");
            statement.executeUpdate("INSERT INTO notes VALUES ('" + FIRST + "', 'first', 1)");
            statement.executeUpdate("INSERT INTO notes VALUES ('" + SECOND + "', 'second', 2)");
            statement.executeUpdate("INSERT INTO notes VALUES ('" + THIRD + "', 'third', 3)");
            statement.executeUpdate("INSERT INTO notes VALUES ('" + FOURTH + "', 'fourth', 4)");
        }
    }

    @Test
    void queryBuilderAndRepositoryAgreeOnTheSameRow() {
        List<Note> fromRepository = new ArrayList<>();
        RepositoryProxy.create(NoteRepo.class, "", executor, null).findAll().forEach(fromRepository::add);

        Note fromBuilder = builder().where("title", "=", "first").findFirst().orElseThrow();

        assertEquals(4, fromRepository.size());
        Note reference = fromRepository.stream()
                .filter(note -> "first".equals(note.title))
                .findFirst()
                .orElseThrow();
        assertEquals(reference.id, fromBuilder.id);
        assertEquals(reference.title, fromBuilder.title);
        assertEquals(reference.views, fromBuilder.views);
    }

    @Test
    void idDeclaredOnlyWithIdIsPopulated() {
        Note note = builder().where("id", "=", FIRST).findFirst().orElseThrow();
        assertEquals(FIRST, note.id);
        assertEquals("first", note.title);
        assertEquals(1, note.views);
    }

    @Test
    void postLoadCallbackRunsForQueryBuilderResults() {
        assertEquals("postLoad", builder().findFirst().orElseThrow().loadedBy);
    }

    @Test
    void findAllAppliesLimitAndOffset() {
        assertEquals(List.of(FIRST), idsOf(builder().orderBy("views").limit(1).findAll()));
        assertEquals(List.of(SECOND, THIRD), idsOf(builder().orderBy("views").limit(2).offset(1).findAll()));
        assertEquals(4, builder().findAll().size());
    }

    @Test
    void countIgnoresLimitAndOffset() {
        assertEquals(4, builder().count());
        assertEquals(4, builder().limit(1).count());
        assertEquals(2, builder().where("views", ">", 2).limit(1).offset(1).count());
        assertTrue(builder().limit(1).exists());
    }

    @Test
    void deleteWithLimitRemovesOnlyThePagedRows() {
        assertEquals(2, builder().orderBy("views").limit(2).delete());
        assertEquals(2, builder().count());
        assertEquals(List.of(THIRD, FOURTH), idsOf(builder().orderBy("views").findAll()));
    }

    @Test
    void deleteWithOffsetRemovesTheRequestedWindowOnly() {
        assertEquals(1, builder().orderBy("views").limit(1).offset(1).delete());
        assertEquals(List.of(FIRST, THIRD, FOURTH), idsOf(builder().orderBy("views").findAll()));
    }

    @Test
    void deleteWithLimitKeepsTheConditions() {
        assertEquals(1, builder().where("views", ">=", 3).limit(1).orderByDesc("views").delete());
        assertEquals(List.of(FIRST, SECOND, THIRD), idsOf(builder().orderBy("views").findAll()));
    }

    @Test
    void deleteWithoutLimitRemovesEveryMatchingRow() {
        assertEquals(2, builder().where("views", ">=", 3).delete());
        assertEquals(2, builder().count());
    }

    @Test
    void deleteWithLimitRefusesToRunWithoutAnIdColumn() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE unkeyed (label TEXT)");
            statement.executeUpdate("INSERT INTO unkeyed VALUES ('keep me')");
        }
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new EntityQueryBuilder<>(Unkeyed.class, executor, "").limit(1).delete());
        assertTrue(thrown.getMessage().contains("Unkeyed"));
        assertEquals(1, countRows("unkeyed"));
    }

    @Test
    void findFirstDoesNotLeakItsLimitIntoLaterQueries() {
        EntityQueryBuilder<Note> builder = builder();
        assertEquals(FIRST, builder.orderBy("views").findFirst().orElseThrow().id);
        assertEquals(4, builder.findAll().size());
    }

    @Test
    void findFirstHonoursOffsetWithoutMutatingTheBuilder() {
        EntityQueryBuilder<Note> builder = builder().orderBy("views").limit(4).offset(1);
        assertEquals(SECOND, builder.findFirst().orElseThrow().id);
        assertEquals(List.of(SECOND, THIRD, FOURTH), idsOf(builder.findAll()));
    }

    @Test
    void unknownFieldNameIsReportedWithTheEntityAndItsFields() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder().where("titel", "=", "first"));
        assertTrue(thrown.getMessage().contains("titel"));
        assertTrue(thrown.getMessage().contains(Note.class.getName()));
        assertTrue(thrown.getMessage().contains("title"));
    }

    @Test
    void emptyTableNameFallsBackToTheClassName() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE widget (widget_id INT PRIMARY KEY, label TEXT)");
            statement.executeUpdate("INSERT INTO widget VALUES (7, 'gear')");
        }
        Widget widget = new EntityQueryBuilder<>(Widget.class, executor, "").findFirst().orElseThrow();
        assertEquals(7, widget.widgetId);
        assertEquals("gear", widget.label);
    }

    @Test
    void groupedConditionsAndOrderingStillWork() {
        List<Note> notes = builder().where("views", ">", 1)
                .and(group -> group.where("views", "<", 4))
                .or(group -> group.where("title", "=", "fourth"))
                .orderByDesc("views")
                .findAll();
        assertEquals(List.of(FOURTH, THIRD, SECOND), idsOf(notes));
    }

    private EntityQueryBuilder<Note> builder() {
        return new EntityQueryBuilder<>(Note.class, executor, "");
    }

    private List<UUID> idsOf(List<Note> notes) {
        List<UUID> ids = new ArrayList<>();
        for (Note note : notes) {
            ids.add(note.id);
        }
        return ids;
    }

    private int countRows(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
