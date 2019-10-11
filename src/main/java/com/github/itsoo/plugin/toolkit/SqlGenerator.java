package com.github.itsoo.plugin.toolkit;

import javax.xml.bind.PropertyException;
import java.util.Map;

import static com.github.itsoo.plugin.PageInfo.*;

/**
 * SqlGenerator
 *
 * @author zxy
 */
public class SqlGenerator {

    /**
     * 组织查询总记录数 SQL
     *
     * @param sql String
     * @return count sql
     */
    public static String generateCountSql(String sql) {
        return String.format("SELECT count(*) FROM (%s) tmp_count", sql);
    }

    /**
     * 根据数据库方言组织分页 SQL
     *
     * @param dialect  String
     * @param sql      String
     * @param pageInfo Map
     * @return page sql
     * @throws PropertyException PropertyException
     */
    public static String generatePageSql(String dialect, String sql, Map<String, Object> pageInfo)
            throws PropertyException {
        if (pageInfo != null) {
            int startRows = getStartRows(pageInfo);

            if (PageUtils.isEquals(Dialect.MYSQL, dialect)) {
                int pageSize = PageUtils.toNumber(pageInfo.get(PAGE_SIZE));

                return String.format("%s LIMIT %d, %d", sql, startRows, pageSize);
            } else if (PageUtils.isEquals(Dialect.ORACLE, dialect)) {
                String pageSql = "SELECT * FROM (SELECT tmp_0.*, rownum rn FROM (%s) tmp_0) WHERE rn > %d AND rn <= %d";

                return String.format(pageSql, sql, startRows, getEndRows(pageInfo));
            }

            throw new PropertyException("未被分页组件支持的数据库: " + dialect);
        }

        return sql;
    }
}
