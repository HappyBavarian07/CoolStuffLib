package de.happybavarian07.coolstufflib.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Runs the given SQL instead of deriving a query from the method name.</p>
 *
 * <ul>
 *   <li>{@code {table}} is replaced with the repository's table name, including the database prefix.</li>
 *   <li>Parameters: {@code ?} takes the method arguments in order, {@code ?1}, {@code ?2}, ... pick an argument by
 *       position (1-based).</li>
 *   <li>{@code SELECT} (or {@code WITH}) queries return the entity, {@code Optional<Entity>}, {@code List<Entity>}, or a
 *       single value such as {@code long}, {@code int} or {@code String} (first column of the first row).
 *       Rows are mapped to the entity by column name.</li>
 *   <li>Every other statement is run as an update and returns the number of changed rows as {@code int}/{@code long},
 *       {@code true} if any row changed as {@code boolean}, or nothing as {@code void}. The repository's entity cache is
 *       cleared afterwards.</li>
 * </ul>
 *
 * <pre>{@code
 * public interface PlayerStatsRepository extends Repository<PlayerStats, String> {
 *     @Query("SELECT * FROM {table} WHERE coins >= ? ORDER BY coins DESC")
 *     List<PlayerStats> richPlayers(int minCoins);
 *
 *     @Query("SELECT SUM(coins) FROM {table}")
 *     long totalCoins();
 *
 *     @Query("UPDATE {table} SET coins = coins + ?2 WHERE uuid = ?1")
 *     int addCoins(String uuid, int amount);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {
    /**
     * @return the SQL to run
     */
    String value();

    /**
     * @return ignored; queries are always SQL
     * @deprecated JPQL was never supported. Every query is SQL.
     */
    @Deprecated
    boolean nativeQuery() default true;
}
