package com.github.itsoo.plugin;

import com.github.itsoo.plugin.util.PageUtil;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * 分页插件
 *
 * @author zxy
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class PagePlugin implements Interceptor {

    /**
     * 数据库方言
     */
    private static String dialect = "";
    /**
     * 需要拦截的Mapper
     */
    private static String regexp = "";

    /**
     * 分页拦截方法
     *
     * @param ivk Invocation
     * @return Object
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation ivk) throws Throwable {
        if (ivk.getTarget() instanceof StatementHandler) {
            StatementHandler handler = (StatementHandler) ivk.getTarget();
            MetaObject metaStatementHandler = SystemMetaObject.forObject(handler);
            MappedStatement statement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
            if (statement.getId().matches(regexp)) {
                BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
                Object obj = boundSql.getParameterObject();
                if (null == obj) {
                    throw new NullPointerException("空指针：boundSql.getParameterObject() 未实例化");
                }
                if (!PageInfo.isFalsePage(obj)) {
                    Connection connection = (Connection) ivk.getArgs()[0];
                    String sql = boundSql.getSql();
                    int count = PageUtil.queryTotalCount(sql, connection, statement, boundSql, obj);
                    Map pageInfo = PageUtil.initPageInfo(obj, count);
                    // 分页查询
                    if (count > 0) {
                        String pageSql = PageUtil.generatePageSql(dialect, sql, pageInfo);
                        ReflectHelper.setValueByFieldName(boundSql, "sql", pageSql);
                    }
                }
            }
        }
        return ivk.proceed();
    }

    /**
     * 注入插件
     *
     * @param arg0 Object
     * @return Object
     */
    @Override
    public Object plugin(Object arg0) {
        return Plugin.wrap(arg0, this);
    }

    /**
     * 注入属性
     *
     * @param p Properties
     */
    @Override
    public void setProperties(Properties p) {
        dialect = p.getProperty("dialect");
        if (dialect == null || "".equals(dialect.trim())) {
            throw new RuntimeException("注入属性异常：找不到必要的 dialect 属性 (该属性用于指定数据库方言)");
        }
        regexp = p.getProperty("regexp");
        if (regexp == null || "".equals(regexp.trim())) {
            throw new RuntimeException("注入属性异常：找不到必要的 regexp 属性 (该属性用于指定拦截SQL正则)");
        }
        // 设置pageSize初始值
        String pageSize = p.getProperty("pageSize");
        if (pageSize != null && !"".equals(pageSize.trim())) {
            PageUtil.pageSize = Integer.parseInt(pageSize);
        }
    }
}
