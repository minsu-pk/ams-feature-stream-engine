package com.kbank.ams.featurestreamengine.common.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static Object evaluate(String expr, Map<String, Object> record) {
        expr = expr.trim();

        // ── 1. 최상위 수준의 산술(+ / -) 처리 ──
        List<String> terms = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        splitTopLevelByPlusMinus(expr, terms, ops);
        if (terms.size() > 1) {
            BigDecimal acc = toBigDecimal(resolveAtomicValue(terms.get(0), record));
            for (int i = 0; i < ops.size(); i++) {
                BigDecimal rhs = toBigDecimal(resolveAtomicValue(terms.get(i + 1), record));
                acc = (ops.get(i) == '+') ? acc.add(rhs) : acc.subtract(rhs);
            }
            return acc;
        }

        // ── 2. 함수 처리 ──
        if (expr.regionMatches(true, 0, "CONCAT(", 0, 7)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "CONCAT"));
            StringBuilder sb = new StringBuilder();
            for (String a : args) {
                Object v = resolveAtomicValue(a, record);
                sb.append(v == null ? "" : v.toString());
            }
            return sb.toString();
        }

        if (expr.regionMatches(true, 0, "TIMEDIFF(", 0, 9)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "TIMEDIFF"));
            if (args.size() != 3) throw new IllegalArgumentException("TIMEDIFF(dt1, dt2, UNIT)");
            Instant t1 = toInstant(resolveAtomicValue(args.get(0), record));
            Instant t2 = toInstant(resolveAtomicValue(args.get(1), record));
            String unitStr = String.valueOf(resolveAtomicValue(args.get(2), record)).toUpperCase();
            ChronoUnit unit = switch (unitStr) {
                case "SECOND", "SECONDS" -> ChronoUnit.SECONDS;
                case "MINUTE", "MINUTES" -> ChronoUnit.MINUTES;
                case "HOUR", "HOURS" -> ChronoUnit.HOURS;
                case "DAY", "DAYS" -> ChronoUnit.DAYS;
                default -> throw new IllegalArgumentException("Unsupported unit: " + unitStr);
            };
            return (t1 != null && t2 != null) ? unit.between(t2, t1) : null;
        }

        if (expr.regionMatches(true, 0, "TO_LOCAL_DATETIME(", 0, 18)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "TO_LOCAL_DATETIME"));
            if (args.size() != 2) throw new IllegalArgumentException("TO_LOCAL_DATETIME(value, 'pattern')");
            Object raw = resolveAtomicValue(args.get(0), record);
            String pattern = String.valueOf(resolveAtomicValue(args.get(1), record));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
            if (raw == null) return null;
            if (raw instanceof LocalDateTime ldt) return ldt;
            if (raw instanceof Instant i) return LocalDateTime.ofInstant(i, ZONE);
            if (raw instanceof String s) return LocalDateTime.parse(s, fmt);
            if (raw instanceof Number n) {
                long v = n.longValue();
                Instant i = (String.valueOf(v).length() >= 13)
                    ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
                return LocalDateTime.ofInstant(i, ZONE);
            }
            throw new IllegalArgumentException("TO_LOCAL_DATETIME unsupported type: " + raw.getClass());
        }

        if (expr.regionMatches(true, 0, "IF(", 0, 3)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "IF"));
            if (args.size() != 3)
                throw new IllegalArgumentException("IF(condition, trueValue, falseValue)");
            Object condResult = evaluateCondition(args.get(0).trim(), record);
            boolean condition = toBoolean(condResult);
            return condition ? resolveAtomicValue(args.get(1).trim(), record)
                : resolveAtomicValue(args.get(2).trim(), record);
        }

        throw new UnsupportedOperationException("Unsupported expression: " + expr);
    }

    /**
     * 단일 레코드가 조건식을 만족하는지 평가.
     * 예: matches(":tx_amt > 1000000 AND :wd_or_dp = 'WD'", record)
     */
    public static boolean matches(String conditionExpr, Map<String, Object> record) {
        if (conditionExpr == null || conditionExpr.isBlank()) {
            // 조건이 비어 있으면 항상 true 로 간주 (필터 없음)
            return true;
        }
        Object result = evaluateCondition(conditionExpr, record);
        return toBoolean(result);
    }

    /**
     * 레코드 리스트에 조건식을 적용해서, 조건을 만족하는 것만 반환.
     */
    public static List<Map<String, Object>> filter(
        List<Map<String, Object>> records,
        String conditionExpr
    ) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (records == null || records.isEmpty()) return out;

        for (Map<String, Object> r : records) {
            if (matches(conditionExpr, r)) {
                out.add(r);
            }
        }
        return out;
    }

    private static Object resolveAtomicValue(String expr, Map<String, Object> record) {
        expr = expr.trim();

        // 문자열 리터럴: 'TEXT'
        if (isQuotedString(expr)) {
            return unquote(expr);
        }

        // 숫자 리터럴: 10, -3.14
        if (isNumber(expr)) {
            return new BigDecimal(expr);
        }

        // 키 참조: :key1, :acct_nbr
        if (expr.startsWith(":")) {
            return record.getOrDefault(expr.substring(1), null);
        }

        // 기본적으로 매칭되는 게 없을 때
        return null;
    }

    // ── 조건식 평가 (AND / OR / 비교 등) ──
    private static Object evaluateCondition(String condExpr, Map<String, Object> record) {
        condExpr = stripOuterParens(condExpr.trim());

        List<String> orParts = splitTopLevelByWord(condExpr, "OR");
        if (orParts.size() > 1) {
            for (String p : orParts)
                if (toBoolean(evaluateCondition(p.trim(), record))) return true;
            return false;
        }

        List<String> andParts = splitTopLevelByWord(condExpr, "AND");
        if (andParts.size() > 1) {
            for (String p : andParts)
                if (!toBoolean(evaluateCondition(p.trim(), record))) return false;
            return true;
        }

        String[] ops = {"!=", ">=", "<=", "=", ">", "<"};
        for (String op : ops) {
            int idx = indexOfTopLevelComparator(condExpr, op);
            if (idx > 0) {
                String left = condExpr.substring(0, idx).trim();
                String right = condExpr.substring(idx + op.length()).trim();
                Object leftVal = resolveAtomicValue(left, record);
                Object rightVal = resolveAtomicValue(right, record);
                int cmp = compare(leftVal, rightVal);
                return switch (op) {
                    case "=" -> cmp == 0;
                    case "!=" -> cmp != 0;
                    case ">" -> cmp > 0;
                    case "<" -> cmp < 0;
                    case ">=" -> cmp >= 0;
                    case "<=" -> cmp <= 0;
                    default -> false;
                };
            }
        }

        Object val = resolveAtomicValue(condExpr, record);
        return toBoolean(val);
    }

    // ── 헬퍼들 ──
    private static void splitTopLevelByPlusMinus(String expr, List<String> terms, List<Character> ops) {
        int depth = 0; boolean inStr = false; char prev = 0; StringBuilder buf = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (inStr) {
                buf.append(c);
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c; continue;
            }
            if (c == '\'') { inStr = true; buf.append(c); prev = c; continue; }
            if (c == '(') depth++;
            if (c == ')') depth = Math.max(0, depth - 1);
            if (depth == 0 && (c == '+' || c == '-')) {
                String t = buf.toString().trim();
                if (!t.isEmpty()) terms.add(t);
                ops.add(c);
                buf.setLength(0);
            } else buf.append(c);
            prev = c;
        }
        String last = buf.toString().trim();
        if (!last.isEmpty()) terms.add(last);
    }

    private static String extractCallArgs(String expr, String name) {
        String prefix = name + "(";
        if (!expr.regionMatches(true, 0, prefix, 0, prefix.length()) || !expr.endsWith(")"))
            throw new IllegalArgumentException("Call syntax error: " + expr);
        return expr.substring(prefix.length(), expr.length() - 1);
    }

    private static List<String> splitArgsTopLevel(String s) {
        List<String> args = new ArrayList<>();
        int depth = 0; boolean inStr = false; char prev = 0; StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) { buf.append(c); if (c == '\'' && prev != '\\') inStr = false; prev = c; continue; }
            if (c == '\'') { inStr = true; buf.append(c); prev = c; continue; }
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            if (depth == 0 && c == ',') {
                args.add(buf.toString().trim()); buf.setLength(0);
            } else buf.append(c);
            prev = c;
        }
        String last = buf.toString().trim();
        if (!last.isEmpty()) args.add(last);
        return args;
    }

    private static boolean isQuotedString(String s) {
        return s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length()-1) == '\'';
    }

    private static String unquote(String s) {
        return s.substring(1, s.length()-1).replace("\\'", "'");
    }

    private static boolean isNumber(String s) {
        return s.matches("[+-]?\\d+(?:\\.\\d+)?");
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        if (v instanceof String s && isNumber(s)) return new BigDecimal(s);
        throw new IllegalArgumentException("Not a number: " + v);
    }

    private static Instant toInstant(Object v) {
        if (v == null) return null;
        if (v instanceof Instant i) return i;
        if (v instanceof LocalDateTime ldt) return ldt.atZone(ZONE).toInstant();
        if (v instanceof ZonedDateTime zdt) return zdt.toInstant();
        if (v instanceof OffsetDateTime odt) return odt.toInstant();
        if (v instanceof Number n) {
            long val = n.longValue();
            return (String.valueOf(val).length() >= 13)
                ? Instant.ofEpochMilli(val) : Instant.ofEpochSecond(val);
        }
        if (v instanceof String s) {
            try { return Instant.parse(s); } catch (Exception ignore) {}
        }
        return null;
    }

    private static int compare(Object a, Object b) {
        if (a == null || b == null) return (a == b) ? 0 : (a == null ? -1 : 1);
        if (a instanceof Number n1 && b instanceof Number n2)
            return new BigDecimal(n1.toString()).compareTo(new BigDecimal(n2.toString()));
        return a.toString().compareTo(b.toString());
    }

    private static boolean toBoolean(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s)
            return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("y");
        return false;
    }

    private static List<String> splitTopLevelByWord(String s, String token) {
        List<String> parts = new ArrayList<>();
        int depth = 0; boolean inStr = false; char prev = 0;
        int start = 0; final int n = s.length(), tlen = token.length();

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c; continue;
            }
            if (c == '\'') { inStr = true; prev = c; continue; }
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);

            if (depth == 0 && i + tlen <= n &&
                s.regionMatches(true, i, token, 0, tlen) &&
                isWordBoundary(s, i-1) && isWordBoundary(s, i+tlen)) {
                parts.add(s.substring(start, i).trim());
                i += tlen - 1;
                start = i + 1;
            }
            prev = c;
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty()) parts.add(last);
        return parts;
    }

    private static boolean isWordBoundary(String s, int idx) {
        if (idx < 0 || idx >= s.length()) return true;
        char ch = s.charAt(idx);
        return !(Character.isLetterOrDigit(ch) || ch == '_');
    }

    private static String stripOuterParens(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '(' && s.charAt(s.length()-1) == ')') {
            int depth = 0; boolean inStr = false; char prev = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (inStr) {
                    if (c == '\'' && prev != '\\') inStr = false;
                } else {
                    if (c == '\'') inStr = true;
                    else if (c == '(') depth++;
                    else if (c == ')') {
                        depth--;
                        if (depth == 0 && i != s.length()-1) return s;
                    }
                }
                prev = c;
            }
            return s.substring(1, s.length()-1).trim();
        }
        return s;
    }

    private static int indexOfTopLevelComparator(String s, String op) {
        int depth = 0; boolean inStr = false; char prev = 0;
        final int n = s.length(), olen = op.length();
        for (int i = 0; i <= n - olen; i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c; continue;
            }
            if (c == '\'') { inStr = true; prev = c; continue; }
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            if (depth == 0 && s.regionMatches(i, op, 0, olen)) return i;
            prev = c;
        }
        return -1;
    }
}
