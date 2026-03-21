package com.knowledge.mapper.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MyBatis TypeHandler for PostgreSQL text[] array type.
 * Converts between String "{a,b,c}" and List<String>.
 */
@MappedTypes(List.class)
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setString(i, "{}");
        } else {
            StringBuilder sb = new StringBuilder("{");
            for (int j = 0; j < parameter.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\"").append(escape(parameter.get(j))).append("\"");
            }
            sb.append("}");
            ps.setString(i, sb.toString());
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseArray(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseArray(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseArray(cs.getString(columnIndex));
    }

    private List<String> parseArray(String s) {
        List<String> list = new ArrayList<>();
        if (s == null || s.isEmpty() || s.equals("{}") || s.equals("{}")) {
            return list;
        }
        // Remove outer { }
        String inner = s.substring(1, s.length() - 1);
        if (inner.isEmpty()) return list;
        // Split by comma not inside quotes
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                list.add(unescape(current.toString().trim()));
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) {
            list.add(unescape(last));
        }
        return list;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
