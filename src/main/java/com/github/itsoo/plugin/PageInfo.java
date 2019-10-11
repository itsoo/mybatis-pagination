package com.github.itsoo.plugin;

import com.github.itsoo.plugin.toolkit.ReflectHelper;

import java.util.HashMap;
import java.util.Map;

import static com.github.itsoo.plugin.toolkit.PageUtils.*;

/**
 * 结果页
 *
 * @author zxy
 */
public final class PageInfo {

    public static final String PAGE_LIST = "pageList";        // 分页属性 "false" 不分页
    public static final String PAGE = "page";                 // 封装 page 属性
    public static final String PAGE_NUM = "pageNum";          // 当前页数
    public static final String PAGE_SIZE = "pageSize";        // 页容量
    public static final String TOTAL_PAGE = "totalPage";      // 总页数
    public static final String TOTAL_COUNT = "totalCount";    // 总记录数
    public static final String DATA_LIST = "dataList";        // 查询结果集
    public static final String HAS_PRE_PAGE = "hasPrePage";   // 有上一页
    public static final String HAS_NEXT_PAGE = "hasNextPage"; // 有下一页

    static final String DIALECT = "dialect";                  // 数据库方言
    static final String REGEXP = "regexp";                    // 拦截器正则
    static Integer defaultPageSize = 10;                      // 默认页容量

    /**
     * 方言枚举
     */
    public interface Dialect {
        String MYSQL = "mysql";
        String ORACLE = "oracle";
    }

    private PageInfo() {}

    public static Map<String, Object> getInstance() {
        Map<String, Object> map = new HashMap<>();
        map.put(PAGE_NUM, 1);
        map.put(PAGE_SIZE, defaultPageSize);
        map.put(TOTAL_PAGE, 0);
        map.put(TOTAL_COUNT, 0);
        map.put(DATA_LIST, null);
        map.put(HAS_PRE_PAGE, false);
        map.put(HAS_NEXT_PAGE, false);

        return map;
    }

    public static Map<String, Object> getInstance(Map<String, Object> map) {
        setPageInfo(map);
        setPageNum(map);
        setPageSize(map);
        map.put(DATA_LIST, null);
        setHasPrePage(map);
        setHasNextPage(map);

        return map;
    }

    public static Map<String, Object> getInstance(Map<String, Object> map, Object dataList) {
        getInstance(map);
        map.put(DATA_LIST, dataList);

        return map;
    }

    /**
     * 得到分页起始行号
     *
     * @return int
     */
    public static int getStartRows(Map<String, Object> map) {
        return (getNumber(map, PAGE_NUM) - 1) * getNumber(map, PAGE_SIZE);
    }

    /**
     * 得到分页结束行号
     *
     * @return int
     */
    public static int getEndRows(Map<String, Object> map) {
        return getNumber(map, PAGE_NUM) * getNumber(map, PAGE_SIZE);
    }

    /**
     * 不分页
     *
     * @param obj Object
     * @return boolean
     */
    public static boolean isFalsePage(Object obj) {
        if (obj instanceof Map) {
            return isFalseValue(((Map) obj).get(PAGE_LIST));
        }

        try {
            return isFalseValue(ReflectHelper.getFieldValue(obj, PAGE_LIST));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            try {
                Map map = (Map) ReflectHelper.getFieldValue(obj, PAGE);
                if (map != null) {
                    return isFalseValue(map.get(PAGE_LIST));
                }
            } catch (Exception ignore) {}
        }

        return false;
    }

    private static void setPageInfo(Map<String, Object> map) {
        String[] keys = {PAGE_NUM, PAGE_SIZE, TOTAL_PAGE, TOTAL_COUNT, DATA_LIST, HAS_PRE_PAGE, HAS_NEXT_PAGE};
        Object[] objs = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            objs[i] = map.get(keys[i]);
        }

        map.putAll(getInstance());

        // 原值转换
        for (int i = 0; i < objs.length; i++) {
            setKeyAndValueOfMap(map, keys[i], objs[i]);
        }
    }

    private static void setKeyAndValueOfMap(Map<String, Object> map, String key, Object value) {
        if (!isEmpty(value)) {
            map.put(key, value);
        }
    }

    private static void setPageNum(Map<String, Object> map) {
        setTotalPage(map);

        Object oPageNum = map.get(PAGE_NUM);
        Object oTotalPage = map.get(TOTAL_PAGE);
        if (isEmpty(oPageNum)) {
            map.put(PAGE_NUM, 1);
        } else {
            int pageNum = toNumber(oPageNum);
            int totalPage = toNumber(oTotalPage);
            map.put(PAGE_NUM, Math.min(Math.max(1, pageNum), totalPage));
        }
    }

    private static void setPageSize(Map<String, Object> map) {
        Object pageSize = map.get(PAGE_SIZE);
        if (isEmpty(pageSize)) {
            map.put(PAGE_SIZE, defaultPageSize);
        } else {
            map.put(PAGE_SIZE, toNumber(pageSize));
        }
    }

    private static void setTotalPage(Map<String, Object> map) {
        Object tcObj = map.get(TOTAL_COUNT);
        Object psObj = map.get(PAGE_SIZE);
        if (!isEmpty(tcObj) && !isEmpty(psObj)) {
            int totalCount = toNumber(tcObj);
            int pageSize = toNumber(psObj);
            map.put(TOTAL_PAGE, (int) Math.ceil(((double) totalCount) / pageSize));

            return;
        }

        map.put(TOTAL_PAGE, 1);
    }

    private static void setHasPrePage(Map<String, Object> map) {
        map.put(HAS_PRE_PAGE, getNumber(map, PAGE_NUM) > 1);
    }

    private static void setHasNextPage(Map<String, Object> map) {
        map.put(HAS_NEXT_PAGE, getNumber(map, PAGE_NUM) < getNumber(map, TOTAL_PAGE));
    }
}
