package dev.plainround.orders.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** Empties every table between tests that commit real transactions. */
public final class DatabaseReset {

    private DatabaseReset() {}

    /**
     * Truncates all application tables and restarts their identity columns.
     *
     * @param jdbc the template on the test database
     */
    public static void truncate(JdbcTemplate jdbc) {
        jdbc.execute(
                "TRUNCATE order_lines, orders_table, customers, stock_items, audit_records RESTART"
                        + " IDENTITY CASCADE");
    }

    /**
     * Counts the rows of a table, independent of JPA and its caches.
     *
     * @param jdbc the template on the test database
     * @param table the table name (a constant of the test, never user input)
     * @return the row count
     */
    public static long count(JdbcTemplate jdbc, String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }
}
