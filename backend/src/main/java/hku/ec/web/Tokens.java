package hku.ec.web;

/** 从 Authorization: Bearer xxx 或 X-Token 里取 token */
public final class Tokens {

    private Tokens() { }

    public static String from(String authorizationHeader) {
        if (authorizationHeader == null) return null;
        String v = authorizationHeader.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return v.substring(7).trim();
        }
        return v.isEmpty() ? null : v;
    }
}
