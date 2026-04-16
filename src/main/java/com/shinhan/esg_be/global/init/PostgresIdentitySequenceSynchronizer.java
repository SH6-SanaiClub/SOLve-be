package com.shinhan.esg_be.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PostgresIdentitySequenceSynchronizer implements CommandLineRunner {

    private static final String TARGET_SCHEMA = "public";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        if (!isPostgreSql()) {
            return;
        }

        List<SequenceTarget> sequenceTargets = findSequenceTargets();
        int synchronizedCount = 0;

        for (SequenceTarget sequenceTarget : sequenceTargets) {
            String sequenceName = findSequenceName(sequenceTarget);
            if (sequenceName == null || sequenceName.isBlank()) {
                continue;
            }

            Long maxId = findMaxId(sequenceTarget);
            if (maxId == null || maxId <= 0) {
                continue;
            }

            SequenceReference sequenceReference = parseSequenceReference(sequenceName);
            Long currentSequenceValue = findCurrentSequenceValue(sequenceReference);
            if (currentSequenceValue != null && currentSequenceValue >= maxId) {
                continue;
            }

            syncSequence(sequenceName, maxId);
            synchronizedCount++;
            log.warn(
                    "Synchronized PostgreSQL identity sequence. table={}.{} sequence={} previousValue={} newValue={}",
                    sequenceTarget.tableName(),
                    sequenceTarget.columnName(),
                    sequenceName,
                    currentSequenceValue,
                    maxId
            );
        }

        if (synchronizedCount > 0) {
            log.info("Synchronized {} PostgreSQL identity sequences on startup.", synchronizedCount);
        }
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException exception) {
            log.warn("Failed to inspect database metadata for sequence synchronization.", exception);
            return false;
        }
    }

    private List<SequenceTarget> findSequenceTargets() {
        return jdbcTemplate.query(
                """
                        select cols.table_name, cols.column_name
                        from information_schema.columns cols
                        join information_schema.key_column_usage kcu
                          on cols.table_schema = kcu.table_schema
                         and cols.table_name = kcu.table_name
                         and cols.column_name = kcu.column_name
                        join information_schema.table_constraints tc
                          on kcu.constraint_schema = tc.constraint_schema
                         and kcu.constraint_name = tc.constraint_name
                         and kcu.table_name = tc.table_name
                        where cols.table_schema = ?
                          and tc.constraint_type = 'PRIMARY KEY'
                          and (cols.column_default like 'nextval(%' or cols.is_identity = 'YES')
                        order by cols.table_name
                        """,
                (resultSet, rowNum) -> new SequenceTarget(
                        resultSet.getString("table_name"),
                        resultSet.getString("column_name")
                ),
                TARGET_SCHEMA
        );
    }

    private String findSequenceName(SequenceTarget sequenceTarget) {
        return jdbcTemplate.queryForObject(
                "select pg_get_serial_sequence(?, ?)",
                String.class,
                TARGET_SCHEMA + "." + sequenceTarget.tableName(),
                sequenceTarget.columnName()
        );
    }

    private Long findMaxId(SequenceTarget sequenceTarget) {
        validateIdentifier(sequenceTarget.tableName());
        validateIdentifier(sequenceTarget.columnName());

        String sql = "select coalesce(max(" + quoteIdentifier(sequenceTarget.columnName()) + "), 0) from "
                + quoteQualifiedName(TARGET_SCHEMA, sequenceTarget.tableName());

        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private SequenceReference parseSequenceReference(String sequenceName) {
        String[] sequenceParts = sequenceName.split("\\.");
        if (sequenceParts.length == 1) {
            validateIdentifier(sequenceParts[0]);
            return new SequenceReference(TARGET_SCHEMA, sequenceParts[0]);
        }

        if (sequenceParts.length != 2) {
            throw new IllegalArgumentException("Unsupported sequence name: " + sequenceName);
        }

        validateIdentifier(sequenceParts[0]);
        validateIdentifier(sequenceParts[1]);
        return new SequenceReference(sequenceParts[0], sequenceParts[1]);
    }

    private Long findCurrentSequenceValue(SequenceReference sequenceReference) {
        return jdbcTemplate.queryForObject(
                """
                        select coalesce(last_value, 0)
                        from pg_sequences
                        where schemaname = ?
                          and sequencename = ?
                        """,
                Long.class,
                sequenceReference.schemaName(),
                sequenceReference.sequenceName()
        );
    }

    private void syncSequence(String sequenceName, Long maxId) {
        jdbcTemplate.queryForObject(
                "select setval(to_regclass(?), ?, true)",
                Long.class,
                sequenceName,
                maxId
        );
    }

    private void validateIdentifier(String identifier) {
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + identifier);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    private String quoteQualifiedName(String schemaName, String tableName) {
        validateIdentifier(schemaName);
        validateIdentifier(tableName);
        return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    private record SequenceTarget(String tableName, String columnName) {
    }

    private record SequenceReference(String schemaName, String sequenceName) {
    }
}
