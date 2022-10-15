package nextstep.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void execute(final String sql) throws DataAccessException {
        final var conn = DataSourceUtils.getConnection(dataSource);
        try(
                final var pstmt = conn.prepareStatement(sql)
        ) {
            log.debug("query : {}", sql);

            pstmt.execute();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException(e);
        }
    }

    public void update(final String sql, final Object... args) {
        final var conn = DataSourceUtils.getConnection(dataSource);
        try(
                final var pstmt = conn.prepareStatement(sql)
        ) {
            log.debug("query : {}", sql);

            setPstmt(pstmt, args);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException(e);
        }
    }

    public <T> T queryForObject(final String sql, final RowMapper<T> rowMapper, final Object... args) throws DataAccessException {
        final List<T> results = query(sql, rowMapper, args);
        return DataAccessUtils.nullableSingleResult(results);
    }

    public <T> List<T> query(final String sql, final RowMapper<T> rowMapper, final Object... args) throws DataAccessException {
        final var conn = DataSourceUtils.getConnection(dataSource);
        try(
                final var pstmt = conn.prepareStatement(sql)
        ) {
            log.debug("query : {}", sql);

            setPstmt(pstmt, args);
            final ResultSet rs = pstmt.executeQuery();

            return extractData(rowMapper, rs);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException(e);
        }
    }

    private <T> List<T> extractData(final RowMapper<T> rowMapper, final ResultSet rs) throws SQLException {
        final List<T> results = new ArrayList<>();
        int rowNum = 0;

        while (rs.next()) {
            results.add(rowMapper.mapRow(rs, rowNum++));
        }
        return results;
    }

    private void setPstmt(final PreparedStatement pstmt, final Object... args) throws SQLException {
        if (hasArgs(args)) {
            setValues(pstmt, args);
        }
    }

    private boolean hasArgs(final Object... args) {
        return args != null;
    }

    private void setValues(final PreparedStatement pstmt, final Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            final var arg = args[i];
            pstmt.setObject(i + 1, arg);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
