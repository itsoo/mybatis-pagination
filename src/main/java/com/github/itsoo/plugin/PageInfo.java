package com.github.itsoo.plugin;

import com.github.itsoo.plugin.util.PageUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 结果页
 *
 * @author zxy
 */
public final class PageInfo {

    /**
     * 私有的构造
     */
    private PageInfo() {
        super();
    }

    /**
     * 实例化方法
     *
     * @return Map
     */
    public static Map getInstance() {
        Map<String, Object> map = new HashMap<>(7);
        // 当前页数
        map.put("pageNum", 1);
        // 每页条目数
        map.put("pageSize", PageUtil.pageSize);
        // 总页数
        map.put("totalPage", 0);
        // 总记录数
        map.put("totalCount", 0);
        // 查询结果集
        map.put("dataList", null);
        // 有上一页
        map.put("hasPrePage", false);
        // 有下一页
        map.put("hasNextPage", false);
        return map;
    }

    /**
     * 实例化方法
     *
     * @param map Map
     * @return Map
     */
    public static Map getInstance(Map map) {
        // 结果页
        setPageInfo(map);
        // 当前页数
        setPageNum(map);
        // 每页条目数
        setPageSize(map);
        // 查询结果集
        map.put("dataList", null);
        // 有上一页
        setHasPrePage(map);
        // 有下一页
        setHasNextPage(map);
        return map;
    }

    /**
     * 实例化方法
     *
     * @param map      Map
     * @param dataList Object
     * @return Map
     */
    public static Map getInstance(Map map, Object dataList) {
        getInstance(map);
        // 查询结果集
        map.put("dataList", dataList);
        return map;
    }

    /**
     * 得到分页起始行号
     *
     * @return int
     */
    public static int getStartRows(Map map) {
        int pageNum = Integer.parseInt(map.get("pageNum").toString());
        int pageSize = Integer.parseInt(map.get("pageSize").toString());
        return (pageNum - 1) * pageSize;
    }

    /**
     * 得到分页结束行号
     *
     * @return int
     */
    public static int getEndRows(Map map) {
        int pageNum = Integer.parseInt(map.get("pageNum").toString());
        int pageSize = Integer.parseInt(map.get("pageSize").toString());
        return pageNum * pageSize;
    }

    /**
     * 钩子方法判断是否需要分页
     *
     * @param obj Object
     * @return boolean
     */
    public static boolean isFalsePage(Object obj) {
        if (obj instanceof Map) {
            Object pageList = ((Map) obj).get("pageList");
            if (null != pageList && "false".equals(pageList.toString())) {
                return true;
            }
        } else {
            Object pageList = null;
            try {
                pageList = ReflectHelper.getValueByFieldName(obj, "pageList");
            } catch (Exception e) {
                try {
                    Map map = (Map) ReflectHelper.getValueByFieldName(obj, "page");
                    pageList = map.get("pageList");
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
            if (null != pageList && "false".equals(pageList.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * SETTER METHOD
     */
    private static void setPageInfo(Map map) {
        String[] keys = {"pageNum", "pageSize", "totalPage", "totalCount", "dataList", "hasPrePage", "hasNextPage"};
        Object[] objs = new Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            objs[i] = map.get(keys[i]);
        }
        map.putAll(getInstance());
        // 原值转换
        for (int i = 0; i < objs.length; i++) {
            setMapKeyValue(map, keys[i], objs[i]);
        }
    }

    private static void setMapKeyValue(Map map, String key, Object value) {
        if (null != value && !"".equals(value)) {
            map.put(key, value);
        }
    }

    private static void setPageNum(Map map) {
        // 总页数
        setTotalPage(map);
        Object pageNum = map.get("pageNum");
        Object totalPage = map.get("totalPage");
        if (null == pageNum || "".equals(pageNum)) {
            pageNum = 1;
        } else {
            int pn = Integer.parseInt(pageNum.toString());
            int tp = Integer.parseInt(totalPage.toString());
            pageNum = Math.min(Math.max(1, pn), tp);
        }
        map.put("pageNum", pageNum);
    }

    private static void setPageSize(Map map) {
        Object pageSize = map.get("pageSize");
        if (null == pageSize || "".equals(pageSize)) {
            map.put("pageSize", PageUtil.pageSize);
        } else {
            map.put("pageSize", Integer.parseInt(pageSize.toString()));
        }
    }

    private static void setTotalPage(Map map) {
        Object tcObj = map.get("totalCount");
        Object psObj = map.get("pageSize");
        int totalPage = 1;
        if (null != tcObj && !"".equals(tcObj)) {
            if (null != psObj && !"".equals(psObj)) {
                int totalCount = Integer.parseInt(tcObj.toString());
                int pageSize = Integer.parseInt(psObj.toString());
                totalPage = (int) Math.ceil(((double) totalCount) / pageSize);
            }
        }
        map.put("totalPage", totalPage);
    }

    private static void setHasPrePage(Map map) {
        int pageNum = Integer.parseInt(map.get("pageNum").toString());
        map.put("hasPrePage", pageNum > 1);
    }

    private static void setHasNextPage(Map map) {
        int pageNum = Integer.parseInt(map.get("pageNum").toString());
        int totalPage = Integer.parseInt(map.get("totalPage").toString());
        map.put("hasNextPage", pageNum < totalPage);
    }
}
