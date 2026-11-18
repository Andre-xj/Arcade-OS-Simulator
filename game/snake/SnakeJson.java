package game.snake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HEAP vs STACK SNAKE - the game's own tiny JSON reader.
 *
 * {@code arcadeos.Json} is package-private in the shared package, so the Snake
 * package cannot reach it. This is a deliberately small recursive-descent
 * reader that only has to handle the level files: objects, arrays, strings,
 * numbers, booleans and null. It never touches the file system - the caller
 * hands it text - so a broken file can be reported instead of crashing.
 */
final class SnakeJson {

    private final String s;
    private int i;

    private SnakeJson(String s) { this.s = s; }

    /** Parses one JSON value and rejects trailing junk. */
    static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("empty document");
        SnakeJson p = new SnakeJson(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i < p.s.length()) {
            throw new IllegalArgumentException("trailing characters at " + p.i);
        }
        return v;
    }

    // ── Typed accessors used by the level loader ─────────────────────────
    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        throw new IllegalArgumentException("expected a JSON object");
    }

    /** A nested object, or {@code null} when the key is missing. */
    static Map<String, Object> child(Map<String, Object> o, String key) {
        Object v = o == null ? null : o.get(key);
        if (v == null) return null;
        return obj(v);
    }

    static String str(Map<String, Object> o, String key, String def) {
        Object v = o == null ? null : o.get(key);
        return v instanceof String ? (String) v : def;
    }

    static int intg(Map<String, Object> o, String key, int def) {
        Object v = o == null ? null : o.get(key);
        if (v instanceof Number) return (int) Math.round(((Number) v).doubleValue());
        if (v instanceof String) {
            try { return (int) Math.round(Double.parseDouble((String) v)); }
            catch (NumberFormatException ignored) { return def; }
        }
        return def;
    }

    static long lng(Map<String, Object> o, String key, long def) {
        return intg(o, key, (int) def);
    }

    static boolean bool(Map<String, Object> o, String key, boolean def) {
        Object v = o == null ? null : o.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return def;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Map<String, Object> o, String key) {
        Object v = o == null ? null : o.get(key);
        if (v instanceof List) return (List<Object>) v;
        return null;
    }

    // ── Recursive descent ────────────────────────────────────────────────
    private Object value() {
        char c = peek();
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true");  return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null");  return null;
            default:  return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;                                  // '{'
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            String k = string();
            ws();
            if (peek() != ':') throw fail("expected ':'");
            i++;
            ws();
            m.put(k, value());
            ws();
            char c = peek();
            if (c == ',') { i++; continue; }
            if (c == '}') { i++; return m; }
            throw fail("expected ',' or '}'");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;                                  // '['
        ws();
        if (peek() == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = peek();
            if (c == ',') { i++; continue; }
            if (c == ']') { i++; return l; }
            throw fail("expected ',' or ']'");
        }
    }

    private String string() {
        if (peek() != '"') throw fail("expected a string");
        i++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw fail("unterminated string");
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            if (i >= s.length()) throw fail("unterminated escape");
            char e = s.charAt(i++);
            switch (e) {
                case '"':  sb.append('"');  break;
                case '\\': sb.append('\\'); break;
                case '/':  sb.append('/');  break;
                case 'b':  sb.append('\b'); break;
                case 'f':  sb.append('\f'); break;
                case 'n':  sb.append('\n'); break;
                case 'r':  sb.append('\r'); break;
                case 't':  sb.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw fail("bad \\u escape");
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: throw fail("bad escape \\" + e);
            }
        }
    }

    private Double number() {
        int start = i;
        if (peek() == '-' || peek() == '+') i++;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'
                    || c == '-' || c == '+') i++;
            else break;
        }
        if (i == start) throw fail("expected a value");
        try {
            return Double.valueOf(s.substring(start, i));
        } catch (NumberFormatException ex) {
            throw fail("bad number \"" + s.substring(start, i) + "\"");
        }
    }

    private void expect(String lit) {
        if (!s.startsWith(lit, i)) throw fail("expected " + lit);
        i += lit.length();
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
            else break;
        }
    }

    private char peek() {
        if (i >= s.length()) throw fail("unexpected end of document");
        return s.charAt(i);
    }

    private IllegalArgumentException fail(String msg) {
        return new IllegalArgumentException(msg + " (at " + i + ")");
    }
}
