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

/**
 * Lightweight expression evaluator for Map-based records.
 *
 * Supported:
 * - Arithmetic: +, - (top-level only; handles unary +/-)
 * - Functions: CONCAT, TIMEDIFF, TO_LOCAL_DATETIME, IF
 * - Conditions: AND, OR, (=, !=, >, <, >=, <=), LIKE, NOT LIKE
 * - Atomic: :field reference, quoted strings, numbers
 *
 * Notes:
 * - LIKE supports %, _ and backslash escape (\%, \_)
 * - Comparator attempts numeric and Instant comparisons before string compare
 */
public class ExpressionEvaluator {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static Object evaluate(String expr, Map<String, Object> record) {
        expr = expr.trim();

        // ── 1) Top-level arithmetic (+ / -) ──
        List<String> terms = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        splitTopLevelByPlusMinus(expr, terms, ops);

        if (terms.size() > 1) {
            BigDecimal acc = toBigDecimal(resolveAtomicOrFuncValue(terms.get(0), record));
            for (int i = 0; i < ops.size(); i++) {
                BigDecimal rhs = toBigDecimal(resolveAtomicOrFuncValue(terms.get(i + 1), record));
                acc = (ops.get(i) == '+') ? acc.add(rhs) : acc.subtract(rhs);
            }
            return acc;
        }

        // ── 2) Functions ──
        if (expr.regionMatches(true, 0, "CONCAT(", 0, 7)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "CONCAT"));
            StringBuilder sb = new StringBuilder();
            for (String a : args) {
                Object v = resolveAtomicOrFuncValue(a, record);
                sb.append(v == null ? "" : v.toString());
            }
            return sb.toString();
        }

        if (expr.regionMatches(true, 0, "TIMEDIFF(", 0, 9)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "TIMEDIFF"));
            if (args.size() != 3) throw new IllegalArgumentException("TIMEDIFF(dt1, dt2, UNIT)");
            Instant t1 = toInstant(resolveAtomicOrFuncValue(args.get(0), record));
            Instant t2 = toInstant(resolveAtomicOrFuncValue(args.get(1), record));
            String unitStr = String.valueOf(resolveAtomicOrFuncValue(args.get(2), record)).toUpperCase();

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
            Object raw = resolveAtomicOrFuncValue(args.get(0), record);
            String pattern = String.valueOf(resolveAtomicOrFuncValue(args.get(1), record));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);

            if (raw == null) return null;
            if (raw instanceof LocalDateTime ldt) return ldt;
            if (raw instanceof Instant i) return LocalDateTime.ofInstant(i, ZONE);
            if (raw instanceof String s) return LocalDateTime.parse(s, fmt);
            if (raw instanceof Number n) {
                long v = n.longValue();
                Instant i = (String.valueOf(v).length() >= 13)
                        ? Instant.ofEpochMilli(v)
                        : Instant.ofEpochSecond(v);
                return LocalDateTime.ofInstant(i, ZONE);
            }
            throw new IllegalArgumentException("TO_LOCAL_DATETIME unsupported type: " + raw.getClass());
        }

        if (expr.regionMatches(true, 0, "IF(", 0, 3)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "IF"));
            if (args.size() != 3) throw new IllegalArgumentException("IF(condition, trueValue, falseValue)");
            Object condResult = evaluateCondition(args.get(0).trim(), record);
            boolean condition = toBoolean(condResult);
            return condition
                    ? resolveAtomicOrFuncValue(args.get(1).trim(), record)
                    : resolveAtomicOrFuncValue(args.get(2).trim(), record);
        }

        if (expr.regionMatches(true, 0, "NVL(", 0, 4)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "NVL"));
            if (args.size() != 2) {
                throw new IllegalArgumentException("NVL(value, defaultValue)");
            }

            Object v = resolveAtomicOrFuncValue(args.get(0), record);
            return (v != null)
                    ? v
                    : resolveAtomicOrFuncValue(args.get(1), record);
        }

        if (expr.regionMatches(true, 0, "COALESCE(", 0, 9)) {
            List<String> args = splitArgsTopLevel(extractCallArgs(expr, "COALESCE"));
            if (args.isEmpty()) {
                throw new IllegalArgumentException("COALESCE requires at least one argument");
            }

            for (String a : args) {
                Object v = resolveAtomicOrFuncValue(a, record);
                if (v != null) return v;
            }
            return null;
        }

        throw new UnsupportedOperationException("Unsupported expression: " + expr);
    }

    /**
     * Single-record condition evaluation.
     * Example: matches(":tx_amt > 1000000 AND :wd_or_dp = 'WD'", record)
     */
    public static boolean matches(String conditionExpr, Map<String, Object> record) {
        if (conditionExpr == null || conditionExpr.isBlank()) return true;
        Object result = evaluateCondition(conditionExpr, record);
        return toBoolean(result);
    }

    /**
     * Filter records by condition.
     */
    public static List<Map<String, Object>> filter(List<Map<String, Object>> records, String conditionExpr) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (records == null || records.isEmpty()) return out;

        for (Map<String, Object> r : records) {
            if (matches(conditionExpr, r)) out.add(r);
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Atomic / Function value resolving
    // ─────────────────────────────────────────────────────────────────────────────

    private static Object resolveAtomicOrFuncValue(String expr, Map<String, Object> record) {
        expr = expr.trim();

        // quoted string literal
        if (isQuotedString(expr)) return unquote(expr);

        // number literal
        if (isNumber(expr)) return new BigDecimal(expr);

        // field reference: :key
        if (expr.startsWith(":")) return record.getOrDefault(expr.substring(1), null);

        // function expression: only allow known functions (avoid false positives)
        String u = expr.toUpperCase();
        if (u.startsWith("CONCAT(") || u.startsWith("TIMEDIFF(") || u.startsWith("TO_LOCAL_DATETIME(") || u.startsWith("IF(") || u.startsWith("NVL(") || u.startsWith("COALESCE(")) {
            return evaluate(expr, record);
        }

        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Condition evaluation (AND / OR / LIKE / comparisons)
    // ─────────────────────────────────────────────────────────────────────────────

    private static Object evaluateCondition(String condExpr, Map<String, Object> record) {
        condExpr = stripOuterParens(condExpr.trim());

        // OR
        List<String> orParts = splitTopLevelByWord(condExpr, "OR");
        if (orParts.size() > 1) {
            for (String p : orParts) {
                if (toBoolean(evaluateCondition(p.trim(), record))) return true;
            }
            return false;
        }

        // AND
        List<String> andParts = splitTopLevelByWord(condExpr, "AND");
        if (andParts.size() > 1) {
            for (String p : andParts) {
                if (!toBoolean(evaluateCondition(p.trim(), record))) return false;
            }
            return true;
        }

        // NOT LIKE (must be before LIKE)
        int idxNotLike = indexOfTopLevelWordOp(condExpr, "NOT LIKE");
        if (idxNotLike > 0) {
            String left = condExpr.substring(0, idxNotLike).trim();
            String right = condExpr.substring(idxNotLike + "NOT LIKE".length()).trim();
            Object leftVal = resolveAtomicOrFuncValue(left, record);
            Object rightVal = resolveAtomicOrFuncValue(right, record);
            return !like(leftVal, rightVal, true);
        }

        // LIKE
        int idxLike = indexOfTopLevelWordOp(condExpr, "LIKE");
        if (idxLike > 0) {
            String left = condExpr.substring(0, idxLike).trim();
            String right = condExpr.substring(idxLike + "LIKE".length()).trim();
            Object leftVal = resolveAtomicOrFuncValue(left, record);
            Object rightVal = resolveAtomicOrFuncValue(right, record);
            return like(leftVal, rightVal, true);
        }

        // Comparators
        String[] ops = {"!=", ">=", "<=", "=", ">", "<"};
        for (String op : ops) {
            int idx = indexOfTopLevelComparator(condExpr, op);
            if (idx > 0) {
                String left = condExpr.substring(0, idx).trim();
                String right = condExpr.substring(idx + op.length()).trim();
                Object leftVal = resolveAtomicOrFuncValue(left, record);
                Object rightVal = resolveAtomicOrFuncValue(right, record);

                int cmp = compareSmart(leftVal, rightVal);
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

        // IS NOT NULL (must be before IS NULL)
        int idxIsNotNull = indexOfTopLevelWordOp(condExpr, "IS NOT NULL");
        if (idxIsNotNull > 0) {
            String left = condExpr.substring(0, idxIsNotNull).trim();
            Object leftVal = resolveAtomicOrFuncValue(left, record);
            return leftVal != null;
        }

        // IS NULL
        int idxIsNull = indexOfTopLevelWordOp(condExpr, "IS NULL");
        if (idxIsNull > 0) {
            String left = condExpr.substring(0, idxIsNull).trim();
            Object leftVal = resolveAtomicOrFuncValue(left, record);
            return leftVal == null;
        }

        Object val = resolveAtomicOrFuncValue(condExpr, record);
        return toBoolean(val);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Parsing helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Split expression by top-level + / -, supporting unary +/-.
     */
    private static void splitTopLevelByPlusMinus(String expr, List<String> terms, List<Character> ops) {
        int depth = 0;
        boolean inStr = false;
        char prev = 0;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (inStr) {
                buf.append(c);
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c;
                continue;
            }

            if (c == '\'') {
                inStr = true;
                buf.append(c);
                prev = c;
                continue;
            }

            if (c == '(') depth++;
            if (c == ')') depth = Math.max(0, depth - 1);

            if (depth == 0 && (c == '+' || c == '-')) {
                // Detect unary +/- : if previous non-space is start or an operator / delimiter
                int j = i - 1;
                while (j >= 0 && Character.isWhitespace(expr.charAt(j))) j--;
                boolean unary = (j < 0) || "+-*/(,".indexOf(expr.charAt(j)) >= 0;

                if (!unary) {
                    String t = buf.toString().trim();
                    if (!t.isEmpty()) terms.add(t);
                    ops.add(c);
                    buf.setLength(0);
                    prev = c;
                    continue;
                }
            }

            buf.append(c);
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
        int depth = 0;
        boolean inStr = false;
        char prev = 0;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inStr) {
                buf.append(c);
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c;
                continue;
            }

            if (c == '\'') {
                inStr = true;
                buf.append(c);
                prev = c;
                continue;
            }

            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);

            if (depth == 0 && c == ',') {
                args.add(buf.toString().trim());
                buf.setLength(0);
            } else {
                buf.append(c);
            }
            prev = c;
        }

        String last = buf.toString().trim();
        if (!last.isEmpty()) args.add(last);
        return args;
    }

    private static List<String> splitTopLevelByWord(String s, String token) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inStr = false;
        char prev = 0;

        int start = 0;
        final int n = s.length(), tlen = token.length();

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);

            if (inStr) {
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c;
                continue;
            }
            if (c == '\'') {
                inStr = true;
                prev = c;
                continue;
            }

            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);

            if (depth == 0 && i + tlen <= n &&
                    s.regionMatches(true, i, token, 0, tlen) &&
                    isWordBoundary(s, i - 1) && isWordBoundary(s, i + tlen)) {

                parts.add(s.substring(start, i).trim());
                start = i + tlen;     // move after token
                i += tlen - 1;        // advance loop
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
        if (s.length() >= 2 && s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')') {
            int depth = 0;
            boolean inStr = false;
            char prev = 0;

            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (inStr) {
                    if (c == '\'' && prev != '\\') inStr = false;
                } else {
                    if (c == '\'') inStr = true;
                    else if (c == '(') depth++;
                    else if (c == ')') {
                        depth--;
                        if (depth == 0 && i != s.length() - 1) return s;
                    }
                }
                prev = c;
            }
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static int indexOfTopLevelComparator(String s, String op) {
        int depth = 0;
        boolean inStr = false;
        char prev = 0;
        final int n = s.length(), olen = op.length();

        for (int i = 0; i <= n - olen; i++) {
            char c = s.charAt(i);

            if (inStr) {
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c;
                continue;
            }
            if (c == '\'') {
                inStr = true;
                prev = c;
                continue;
            }

            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);

            if (depth == 0 && s.regionMatches(i, op, 0, olen)) return i;
            prev = c;
        }
        return -1;
    }

    private static int indexOfTopLevelWordOp(String s, String opWord) {
        int depth = 0;
        boolean inStr = false;
        char prev = 0;
        final int n = s.length();
        final int olen = opWord.length();

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);

            if (inStr) {
                if (c == '\'' && prev != '\\') inStr = false;
                prev = c;
                continue;
            }
            if (c == '\'') {
                inStr = true;
                prev = c;
                continue;
            }

            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);

            if (depth == 0 && i + olen <= n &&
                    s.regionMatches(true, i, opWord, 0, olen) &&
                    isWordBoundary(s, i - 1) && isWordBoundary(s, i + olen)) {
                return i;
            }

            prev = c;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // LIKE support
    // ─────────────────────────────────────────────────────────────────────────────

    private static boolean like(Object value, Object patternObj, boolean caseInsensitive) {
        if (value == null || patternObj == null) return false;

        String text = String.valueOf(value);
        String pattern = String.valueOf(patternObj);

        if (isQuotedString(pattern)) pattern = unquote(pattern);

        String regex = sqlLikeToRegex(pattern);

        int flags = caseInsensitive
                ? java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE
                : 0;

        return java.util.regex.Pattern.compile(regex, flags).matcher(text).matches();
    }

    /**
     * Convert SQL LIKE pattern to Java regex.
     * - % => .*
     * - _ => .
     * - \% and \_ are treated as literals
     */
    private static String sqlLikeToRegex(String likePattern) {
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        boolean escape = false;

        for (int i = 0; i < likePattern.length(); i++) {
            char c = likePattern.charAt(i);

            if (!escape && c == '\\') {
                escape = true;
                continue;
            }

            if (!escape) {
                if (c == '%') { sb.append(".*"); continue; }
                if (c == '_') { sb.append(".");  continue; }
            }

            // Escape regex meta chars
            if ("[](){}.*+?$^|#\\".indexOf(c) >= 0) sb.append("\\");
            sb.append(c);

            escape = false;
        }

        sb.append("$");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Type conversion helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private static boolean isQuotedString(String s) {
        return s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'';
    }

    private static String unquote(String s) {
        return s.substring(1, s.length() - 1).replace("\\'", "'");
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
                    ? Instant.ofEpochMilli(val)
                    : Instant.ofEpochSecond(val);
        }

        if (v instanceof String s) {
            // ISO-8601 Instant
            try { return Instant.parse(s); } catch (Exception ignore) {}
            // epoch seconds/millis as string
            if (isNumber(s)) {
                long val = new BigDecimal(s).longValue();
                return (String.valueOf(val).length() >= 13)
                        ? Instant.ofEpochMilli(val)
                        : Instant.ofEpochSecond(val);
            }
        }

        return null;
    }

    /**
     * Smart comparison:
     * - null handling
     * - numeric comparison if both are numbers (or numeric strings)
     * - instant comparison if both convertible to Instant
     * - otherwise string compare
     */
    private static int compareSmart(Object a, Object b) {
        if (a == null || b == null) return (a == b) ? 0 : (a == null ? -1 : 1);

        // numeric
        BigDecimal na = tryBigDecimal(a);
        BigDecimal nb = tryBigDecimal(b);
        if (na != null && nb != null) return na.compareTo(nb);

        // datetime
        Instant ia = toInstant(a);
        Instant ib = toInstant(b);
        if (ia != null && ib != null) return ia.compareTo(ib);

        // fallback string
        return a.toString().compareTo(b.toString());
    }

    private static BigDecimal tryBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        if (v instanceof String s && isNumber(s)) return new BigDecimal(s);
        return null;
    }

    private static boolean toBoolean(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof String s) return s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("y");
        return false;
    }
}
