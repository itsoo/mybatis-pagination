package com.github.itsoo.plugin;

import com.github.itsoo.plugin.toolkit.PageUtils;
import com.github.itsoo.plugin.toolkit.ReflectHelper;
import com.github.itsoo.plugin.toolkit.SqlGenerator;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.PropertyException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.github.itsoo.plugin.PageInfo.*;

/**
 * 分页插件
 *
 * @author zxy
 */
@Component
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
public class PagePlugin implements Interceptor {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 数据库方言
     */
    private static String dialect;

    /**
     * 需要拦截的 Mapper
     */
    private static String regexp;

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        dialect = properties.getProperty(DIALECT);
        if (PageUtils.isEmpty(dialect)) {
            log.error("必要的属性为空", new PropertyException("dialect 属性用于指定数据库方言"));
        }

        regexp = properties.getProperty(REGEXP);
        if (PageUtils.isEmpty(regexp)) {
            log.error("必要的属性为空", new PropertyException("regexp 属性用于指定 SQL 拦截表达式"));
        }

        String pageSize = properties.getProperty(PAGE_SIZE);
        if (!PageUtils.isEmpty(pageSize)) {
            defaultPageSize = PageUtils.toNumber(pageSize);
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (invocation.getTarget() instanceof StatementHandler) {
            StatementHandler handler = (StatementHandler) invocation.getTarget();

            if (getMappedStatement(handler).getId().matches(regexp)) {
                BoundSql boundSql = getBoundSql(handler);
                Object param = PageUtils.getOrDefault(boundSql.getParameterObject(), getInstance());

                if (!isFalsePage(param)) {
                    String countSql = SqlGenerator.generateCountSql(boundSql.getSql());
                    int count = queryTotalCount(handler, countSql, (Connection) invocation.getArgs()[0], param);
                    Map<String, Object> pageInfo = handlePageInfo(param, count);

                    int pageSize = (int) pageInfo.get(PAGE_SIZE);
                    Map<String, Object> condition = count <= pageSize ? null : pageInfo;

                    String pageSql = SqlGenerator.generatePageSql(dialect, boundSql.getSql(), condition);
                    ReflectHelper.setFieldValue(boundSql, "sql", pageSql);
                }
            }
        }

        return invocation.proceed();
    }

    /**
     * 查询总记录数
     *
     * @param handler    StatementHandler
     * @param sql        String
     * @param connection Connection
     * @param obj        Object
     * @return int
     * @throws Throwable Throwable
     */
    private int queryTotalCount(StatementHandler handler, String sql, Connection connection, Object obj)
            throws Throwable {
        BoundSql boundSql = getBoundSql(handler);

        Configuration configuration = getMappedStatement(handler).getConfiguration();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        BoundSql countBoundSql = new BoundSql(configuration, sql, parameterMappings, obj);

        ReflectHelper.copyObjectField(boundSql, countBoundSql, "metaParameters");
        ReflectHelper.copyObjectField(boundSql, countBoundSql, "additionalParameters");

        try (PreparedStatement countStatement = connection.prepareStatement(sql)) {
            handler.getParameterHandler().setParameters(countStatement);

            try (ResultSet rs = countStatement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 设置分页相关信息
     *
     * @param param Object
     * @param count int
     * @return Map
     * @throws Throwable Throwable
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePageInfo(Object param, int count) throws Throwable {
        if (param instanceof Map) {
            Map<String, Object> pageInfo = fixedTotalCount(new HashMap(((Map) param)), count);
            ((Map) param).put(PAGE, pageInfo);

            return pageInfo;
        }

        if (ReflectHelper.getObjectField(param, PAGE) != null) {
            Map<String, Object> pageInfo = fixedTotalCount((Map) ReflectHelper.getFieldValue(param, PAGE), count);
            ReflectHelper.setFieldValue(param, PAGE, pageInfo);

            return pageInfo;
        }

        throw new NoSuchFieldException("查询参数必须为 Map 类型或包含 Map 类型的 page 属性的实例");
    }

    private Map<String, Object> fixedTotalCount(Map<String, Object> pageInfo, int count) {
        pageInfo = PageUtils.getOrDefault(pageInfo, getInstance());
        pageInfo.put(TOTAL_COUNT, count);

        return getInstance(pageInfo);
    }

    private MappedStatement getMappedStatement(StatementHandler handler) {
        return (MappedStatement) SystemMetaObject.forObject(handler).getValue("delegate.mappedStatement");
    }

    private BoundSql getBoundSql(StatementHandler handler) {
        return (BoundSql) SystemMetaObject.forObject(handler).getValue("delegate.boundSql");
    }
}
