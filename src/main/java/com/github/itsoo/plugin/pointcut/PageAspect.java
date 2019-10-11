package com.github.itsoo.plugin.pointcut;

import com.github.itsoo.plugin.toolkit.ReflectHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.github.itsoo.plugin.PageInfo.*;

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
     * @throws Throwable Throwable
     */
    @Around("@annotation(com.github.itsoo.plugin.annotation.Page)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] objects = point.getArgs();
        // 分页参数索引
        int index = getTargetIndex(point.getArgs());
        // 未得到分页参数
        if (index == -1) {
            Map<String, Object> pageInfo;
            for (Object object : objects) {
                pageInfo = handlePageInfo(point.proceed(), object);
                if (pageInfo != null) {
                    return pageInfo;
                }
            }

            return null;
        }

        return handlePageInfo(point.proceed(), objects[index]);
    }

    /**
     * 获取入参包含 pageNum 或 pageSize 属性容器
     *
     * @param objects Object[]
     * @return int
     * @throws IllegalAccessException IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    private int getTargetIndex(Object[] objects) throws IllegalAccessException, NoSuchFieldException {
        for (int i = 0, len = objects.length; i < len; i++) {
            Object obj = objects[i];
            if (obj instanceof Map) {
                if (hasKeyOfPageInfo((Map) obj)) {
                    return i;
                }

                continue;
            }

            if (hasKeyOfPageInfo((Map) ReflectHelper.getFieldValue(obj, PAGE))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 移除分页相关信息
     *
     * @param pageInfo Map
     */
    private void removePageInfos(Map<String, Object> pageInfo) {
        pageInfo.remove(PAGE);
        pageInfo.remove(PAGE_NUM);
        pageInfo.remove(PAGE_SIZE);
        pageInfo.remove(TOTAL_PAGE);
        pageInfo.remove(TOTAL_COUNT);
        pageInfo.remove(HAS_PRE_PAGE);
        pageInfo.remove(HAS_NEXT_PAGE);
        pageInfo.remove(PAGE_LIST);
    }

    /**
     * 入参包含分页参数
     *
     * @param pageInfo Map
     * @return boolean
     */
    private boolean hasKeyOfPageInfo(Map<String, Object> pageInfo) {
        return pageInfo != null && pageInfo.get(PAGE_NUM) != null;
    }

    /**
     * 重置表及相关参数
     *
     * @param obj      Object
     * @param pageInfo Map
     */
    @SuppressWarnings("unchecked")
    private void resetPageInfo(Object obj, Map<String, Object> pageInfo) {
        if (obj instanceof Map) {
            ((Map) obj).remove(PAGE);
        }

        if (pageInfo != null) {
            Map<String, Object> page = (Map) pageInfo.remove(PAGE);
            if (page != null) {
                pageInfo.putAll(page);
            }

            if (isFalsePage(obj)) {
                removePageInfos(pageInfo);
            }
        }
    }

    /**
     * 设置分页相关信息
     *
     * @param src Object
     * @param obj Object
     * @return Map
     * @throws Throwable Throwable
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePageInfo(Object src, Object obj) throws Throwable {
        // 参数为 Map 类型
        Map<String, Object> pageInfo;
        if (obj instanceof Map) {
            pageInfo = getInstance(new HashMap((Map) obj), src);
            resetPageInfo(obj, pageInfo);
            pageInfo.put(DATA_LIST, src);
        } else { // 参数为其它 Bean
            try {
                pageInfo = (Map) ReflectHelper.getFieldValue(obj, PAGE);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("POJO 缺少必要的 Map page 属性");
            }

            getInstance(pageInfo == null ? new HashMap() : pageInfo, src);
            resetPageInfo(obj, pageInfo);
            ReflectHelper.setFieldValue(obj, PAGE, pageInfo);
        }

        return pageInfo;
    }
}
