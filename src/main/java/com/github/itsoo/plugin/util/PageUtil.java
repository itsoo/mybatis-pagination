package com.github.itsoo.plugin.util;

import com.github.itsoo.plugin.PageInfo;
import com.github.itsoo.plugin.ReflectHelper;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分页工具类
 *
 * @author zxy
 */
@Component
public final class PageUtil {

    /**
     * 每页条目数
     */
    public static int pageSize = 10;

    /**
     * 查询总页数
     *
     * @param sql        String
     * @param connection Connection
     * @param statement  MappedStatement
     * @param boundSql   BoundSql
     * @param obj        Object
     * @return int
     * @throws Throwable
     */
    public static int queryTotalCount(String sql, Connection connection, MappedStatement statement, BoundSql boundSql, Object obj) throws Throwable {
        // 查询总记录数
        String countSql = String.format("SELECT COUNT(1) FROM (%s) TMP_COUNT", sql);
        PreparedStatement countStmt = connection.prepareStatement(countSql);
        BoundSql countBS = new BoundSql(statement.getConfiguration(), countSql, boundSql.getParameterMappings(), obj);
        // 复制 BoundSql 时同时复制其中的 additionalParameters 字段
        Field metaParamsField = ReflectHelper.getFieldByFieldName(boundSql, "metaParameters");
        if (metaParamsField != null) {
            MetaObject mo = (MetaObject) ReflectHelper.getValueByFieldName(boundSql, "metaParameters");
            ReflectHelper.setValueByFieldName(countBS, "metaParameters", mo);
        }
        Field additionalField = ReflectHelper.getFieldByFieldName(boundSql, "additionalParameters");
        if (additionalField != null) {
            Map<String, Object> map = (Map<String, Object>) ReflectHelper.getValueByFieldName(boundSql, "additionalParameters");
            ReflectHelper.setValueByFieldName(countBS, "additionalParameters", map);
        }
        // 设置 sql 参数
        setParameters(countStmt, statement, countBS, obj);
        ResultSet rs = countStmt.executeQuery();
        int count = 0;
        if (rs.next()) {
            count = rs.getInt(1);
        }
        rs.close();
        countStmt.close();
        return count;
    }

    /**
     * 初始化PageInfo
     *
     * @param obj   Object
     * @param count int
     * @return Map
     * @throws Throwable
     */
    public static Map initPageInfo(Object obj, int count) throws Throwable {
        Map pageInfo;
        // 参数为 Map
        if (obj instanceof Map) {
            Map param = (Map) obj;
            Map map = new HashMap(param);
            pageInfo = (Map) map.get("page");
            if (pageInfo == null) {
                map.put("totalCount", count);
                pageInfo = PageInfo.getInstance(map);
            }
            param.put("page", pageInfo);
        } else { // 参数为其它 Bean
            Field pageField = ReflectHelper.getFieldByFieldName(obj, "page");
            if (pageField != null) {
                pageInfo = (Map) ReflectHelper.getValueByFieldName(obj, "page");
                if (pageInfo == null) {
                    pageInfo = new HashMap(7);
                }
                pageInfo.put("totalCount", count);
                pageInfo = PageInfo.getInstance(pageInfo);
                // 反射分页对象
                ReflectHelper.setValueByFieldName(obj, "page", pageInfo);
            } else {
                throw new NoSuchFieldException(obj.getClass().getName());
            }
        }
        return pageInfo;
    }

    /**
     * 根据数据库方言组织分页SQL
     *
     * @param dialect  String
     * @param sql      String
     * @param pageInfo Map
     * @return String
     */
    public static String generatePageSql(String dialect, String sql, Map pageInfo) {
        if (pageInfo != null && (dialect != null && !"".equals(dialect.trim()))) {
            if ("mysql".equalsIgnoreCase(dialect)) {
                return String.format("%s LIMIT %d, %d", sql, PageInfo.getStartRows(pageInfo), pageInfo.get("pageSize"));
            } else if ("oracle".equalsIgnoreCase(dialect)) {
                String pageSql = "SELECT * FROM (SELECT TMP_TB.*, ROWNUM ROW_NUM FROM (%s) TMP_TB) WHERE ROW_NUM > %d AND ROW_NUM <= %d";
                return String.format(pageSql, sql, PageInfo.getStartRows(pageInfo), PageInfo.getEndRows(pageInfo));
            } else {
                System.err.println("组织分页SQL异常：未被分页组件支持的数据库：" + dialect);
                return sql;
            }
        } else {
            return sql;
        }
    }

    /**
     * 设置SQL参数
     *
     * @param ps        PreparedStatement
     * @param statement MappedStatement
     * @param boundSql  BoundSql
     * @param obj       Object
     * @throws SQLException
     */
    private static void setParameters(PreparedStatement ps, MappedStatement statement, BoundSql boundSql, Object obj) throws SQLException {
        ErrorContext.instance().activity("setting parameters").object(statement.getParameterMap().getId());
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings != null) {
            Configuration configuration = statement.getConfiguration();
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            MetaObject metaObject = obj == null ? null : configuration.newMetaObject(obj);
            for (int i = 0; i < parameterMappings.size(); i++) {
                ParameterMapping parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    PropertyTokenizer prop = new PropertyTokenizer(propertyName);
                    if (obj == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(obj.getClass())) {
                        value = obj;
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX) && boundSql.hasAdditionalParameter(prop.getName())) {
                        value = boundSql.getAdditionalParameter(prop.getName());
                        if (value != null) {
                            value = configuration.newMetaObject(value).getValue(propertyName.substring(prop.getName().length()));
                        }
                    } else {
                        value = metaObject == null ? null : metaObject.getValue(propertyName);
                    }
                    TypeHandler typeHandler = parameterMapping.getTypeHandler();
                    if (typeHandler == null) {
                        throw new ExecutorException("TypeHandler为null，找不到参数为 " + propertyName + " 在执行的SQL中，ID为 " + statement.getId());
                    }
                    typeHandler.setParameter(ps, i + 1, value, parameterMapping.getJdbcType());
                }
            }
        }
    }
}
