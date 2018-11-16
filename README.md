# mybatis-pagination

## 简介

该插件适用于以 MyBatis 为持久层的为增强 MyBatis 的 SQL 查询使用，集成本插件简单方便。入参支持 Map 类型和自定义 POJO 类（必要的属性为 Map page，该属性用于提供分页数据的封装）。插件在开发初期的目标即为少代码侵入，方便灵活的配置。适合于 Maven 开发的 MyBatis 工程使用。

## 优点
* 依赖非常少：仅仅 spring-context、mybatis 和 aspectj。
* 代码侵入性极小：仅需一个注解（非必须）和正则表达式。
* 业务逻辑方法前加入 @Page 注解即可得到封装好的 Map 类型的 page。
* 不加入 @Page 注解的可得到物理分页后的集合数据（List 类型返回值）。

## 原理

插件通过实现 MyBatis 的 Interceptor 拦截，来实现物理分页 SQL 的组织。并通过注解来拦截方法入参和返回值，业务处理后改写方法返回值（封装的 PageInfo 类型），PageInfo 中主要属性：pageNum、pageSize、totalPage、totalCount、dataList、hasPrePage、hasNextPage。

### 使用方法
1. 确保你的工程为 Maven 工程，且集成了 Spring 和 MyBatis。

2. 在你的工程 lib 中加入 source 下的 jar 包。

3. 在 pom.xml 中加入引用 jar 配置（当然你也可以编译后打 jar 包直接引入）：
```
<dependency>
    <groupId>com.github.itsoo</groupId>
    <artifactId>mybatis-pagination</artifactId>
    <version>1.0.0</version>
</dependency>
```

4. 在 mybatis-config.xml 文件中加入以下配置：
```
<plugins>
    <plugin interceptor="com.github.itsoo.plugin.PagePlugin">
        <!-- 数据库方言 必须项 -->
        <property name="dialect" value="oracle"/>
        <!-- 拦截分页SQL正则 必须项 -->
        <property name="regexp" value=".*ListPage.*"/>
        <!-- 默认每页条目数（非必须 默认值 10） -->
        <property name="pageSize" value="10"/>
    </plugin>
</plugins>
```

5. 在你需要使用分页的逻辑中进行如下配置：
service 实现层中加入 @Page 注解（注解放置在方法前）例如：
```
@Page
public Object query(Map<String, Object> param) {
    // 你的业务逻辑
}
```

6. 修改在需要分页的 sql 中 id 加入 ListPage 关键字（位置任意）例如：
```
<select id="queryListPage" parameterType="HashMap" resultType="HashMap">
    // 你的SQL语句
</select>
```

## 总结

通过以上几步简单配置即可实现 SQL 物理分页。

### 简单理解为以下几步
* 引入依赖。
* 加入 MyBatis 的配置。
* 引用 ListPage 进行分页查询（Mapper 文件 sql 的 id 中）。

该插件目前适用于关联查询等普通操作，暂未支持特殊查询操作：如 for update 操作（毕竟for update的SQL需要重视）、MySQL 的后置 group by（MySQL 的问题，可自行搜索）等。
