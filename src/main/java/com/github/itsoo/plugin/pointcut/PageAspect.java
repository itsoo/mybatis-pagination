package com.github.itsoo.plugin.pointcut;

import com.github.itsoo.plugin.PageInfo;
import com.github.itsoo.plugin.ReflectHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 分页切面
 *
 * @author zxy
 */
@Aspect
@Component
public class PageAspect {

    /**
     * 切点环绕通知
     *
     * @param point ProceedingJoinPoint
     * @return Object
     */
    @Around("@annotation(com.github.itsoo.plugin.annotation.Page)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Map pageInfo = null;
        Object[] objects = point.getArgs();
        Object obj;
        // 入参下标
        int index = -1;
        // 遍历得到入参包含 pageNum 或 pageSize 属性容器
        for (int i = 0, len = objects.length; i < len; i++) {
            obj = objects[i];
            // 参数为 Map 类型
            if (obj instanceof Map) {
                pageInfo = new HashMap((Map) obj);
                if (hasKey4PageInfo(pageInfo)) {
                    index = i;
                    break;
                }
            } else { // 参数为其它 Bean
                try {
                    pageInfo = (Map) ReflectHelper.getValueByFieldName(obj, "page");
                } catch (Exception e) {
                    continue;
                }
                if (hasKey4PageInfo(pageInfo)) {
                    index = i;
                    break;
                }
            }
        }
        // 未得到分页参数的入参
        if (index == -1) {
            for (int i = 0, len = objects.length; i < len; i++) {
                obj = objects[i];
                pageInfo = setPageInfo(point.proceed(), obj);
                // 结束循环
                if (null != pageInfo) {
                    break;
                }
            }
        } else {
            obj = objects[index];
            pageInfo = setPageInfo(point.proceed(), obj);
        }
        return pageInfo;
    }

    /**
     * 移除分页相关信息
     *
     * @param pageInfo Map
     */
    private void removePageInfos(Map pageInfo) {
        pageInfo.remove("pageNum");
        pageInfo.remove("pageSize");
        pageInfo.remove("totalPage");
        pageInfo.remove("totalCount");
        pageInfo.remove("hasPrePage");
        pageInfo.remove("hasNextPage");
        // 控制分页属性 ("false"不分页)
        pageInfo.remove("pageList");
    }

    /**
     * 入参包含分页参数
     *
     * @param pageInfo Map
     * @return boolean
     */
    private boolean hasKey4PageInfo(Map pageInfo) {
        return pageInfo != null && pageInfo.get("pageNum") != null;
    }

    /**
     * 设置分页相关信息
     *
     * @param arc Object
     * @param obj Object
     * @return Map
     * @throws Throwable
     */
    private Map setPageInfo(Object arc, Object obj) throws Throwable {
        Map pageInfo;
        if (obj instanceof String
                || obj instanceof Double
                || obj instanceof Float
                || obj instanceof Long
                || obj instanceof Integer
                || obj instanceof Character
                || obj instanceof Boolean
                || obj instanceof Short
                || obj instanceof Byte) { // String 及包装类型返回 null
            return null;
        }
        // 参数为 Map 类型
        if (obj instanceof Map) {
            pageInfo = new HashMap((Map) obj);
            PageInfo.getInstance(pageInfo, arc);
            Map page = (Map) pageInfo.remove("page");
            if (page != null) {
                pageInfo.putAll(page);
            }
            if (PageInfo.isFalsePage(obj)) {
                removePageInfos(pageInfo);
            }
            pageInfo.put("dataList", arc);
        } else { // 参数为其它 Bean
            try {
                pageInfo = (Map) ReflectHelper.getValueByFieldName(obj, "page");
            } catch (Exception e) {
                throw new Throwable("POJO 缺少必要的 Map page 属性");
            }
            if (null == pageInfo) {
                pageInfo = new HashMap(7);
            }
            PageInfo.getInstance(pageInfo, arc);
            ReflectHelper.setValueByFieldName(obj, "page", pageInfo);
        }
        return pageInfo;
    }
}
