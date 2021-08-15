package org.clever.task.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.beans.PropertyDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 13:48 <br/>
 */
public class SqlUtils {
    public static final char UNDERLINE = '_';

    public static String insertSql(String tableName, Object entity, boolean camelToUnderscore) {
        Map<String, Object> fields = toMap(entity);
        return insertSql(tableName, fields, camelToUnderscore);
    }

    public static String insertSql(String tableName, Map<String, Object> fields, boolean camelToUnderscore) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(tableName).append(" (");
        int index = 0;
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            String fieldName = field.getKey();
            if (index != 0) {
                sb.append(", ");
            }
            sb.append(getFieldName(fieldName, camelToUnderscore));
            index++;
        }
        sb.append(") values (");
        index = 0;
        for (Map.Entry<String, ?> field : fields.entrySet()) {
            String fieldName = field.getKey();
            Object value = field.getValue();
            if (index != 0) {
                sb.append(", ");
            }
            sb.append(":").append(fieldName);
            index++;
        }
        sb.append(")");
        return sb.toString();
    }

    public static Map<String, Object> toMap(Object entity) {
        final Map<String, Object> map = new LinkedHashMap<>();
        final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        PropertyDescriptor[] props = beanWrapper.getPropertyDescriptors();
        for (PropertyDescriptor pd : props) {
            if (beanWrapper.isReadableProperty(pd.getName())) {
                String fieldName = pd.getName();
                Object fieldValue = beanWrapper.getPropertyValue(fieldName);
                if (fieldValue instanceof Class) {
                    continue;
                }
                if (fieldValue != null) {
                    map.put(fieldName, fieldValue);
                }
            }
        }
        return map;
    }

    private static String getFieldName(String fieldName, boolean camelToUnderscore) {
        if (!camelToUnderscore) {
            return fieldName;
        }
        return camelToUnderline(fieldName);
    }

    /**
     * 字符串驼峰转下划线格式
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    private static String camelToUnderline(String param) {
        if (StringUtils.isBlank(param)) {
            return StringUtils.EMPTY;
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len + 16);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append(UNDERLINE);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
