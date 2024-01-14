package com.eingsoft.emop.tc.util;

import com.google.common.base.Strings;

/**
 * 根据Item ID版本，查找查找上一个版本、下一个版本
 *
 * @author king
 */
public class ItemRevIdUtil {

    /**
     * 第一个参数大于第二个参数时，返回1； 相等则返回0， 小于则返回-1
     * 
     * @param revId1
     * @param revId2
     * @return
     */
    public static int compareRevId(String revId1, String revId2) {
        revId1 = revId1 == null ? "" : revId1;
        revId2 = revId2 == null ? "" : revId2;
        int len1 = revId1.length();
        int len2 = revId2.length();
        if (len1 < len2) {
            return -1;
        } else if (len1 > len2) {
            return 1;
        } else {
            int n = revId1.compareTo(revId2);
            return n > 0 ? 1 : (n < 0 ? -1 : 0);
        }
    }

    private static String getFirstRevId() {
        return "A";
    }

    private static char getFirstRevIdChar() {
        return 'A';
    }

    private static char getLastRevIdChar() {
        return 'Z';
    }

    /**
     * 当版本为A时，则认为是初始版本
     * 
     * @param revId
     * @return
     */
    public static boolean isFirstRevId(String revId) {
        if (Strings.isNullOrEmpty(revId)) {
            return false;
        }
        return getFirstRevId().equals(revId);
    }

    /**
     * 初始版本为A，其上一个版本返回为空，否则返回正常的上一个版本，比如B -> A
     * 
     * @param revId
     * @return
     */
    public static String getPreviousRevId(String revId) {
        if (Strings.isNullOrEmpty(revId)) {
            return "";
        }
        char firstChar = getFirstRevIdChar();
        char lastChar = getLastRevIdChar();
        char[] itemRevChars = revId.toCharArray();
        for (int i = itemRevChars.length - 1; i >= 0; --i) {
            char ch = itemRevChars[i];
            ch = (char)(ch - 1);
            if (ch >= firstChar) {
                itemRevChars[i] = ch;
                break;
            } else if (i == 0) {
                itemRevChars[i] = '\0';
            } else {
                itemRevChars[i] = lastChar;
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < itemRevChars.length; ++i) {
            char ch = itemRevChars[i];
            if (ch != '\0') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Z的下一个版本为AA, A的下一个版本为B
     * 
     * @param revId
     * @return
     */
    public static String getNextRevId(String revId) {
        if (Strings.isNullOrEmpty(revId)) {
            return "";
        }
        char firstChar = getFirstRevIdChar();
        char lastChar = getLastRevIdChar();
        char[] itemRevChars = revId.toCharArray();
        boolean carry = false;
        for (int i = itemRevChars.length - 1; i >= 0; --i) {
            char ch = itemRevChars[i];
            ch = (char)(ch + 1);
            if (ch <= lastChar) {
                itemRevChars[i] = ch;
                break;
            } else if (i == 0) {
                itemRevChars[i] = firstChar;
                carry = true;
            } else {
                itemRevChars[i] = firstChar;
            }
        }
        StringBuffer sb = new StringBuffer();
        if (carry) {
            sb.append(firstChar);
        }
        for (int i = 0; i < itemRevChars.length; ++i) {
            char ch = itemRevChars[i];
            sb.append(ch);
        }
        return sb.toString();
    }
}
