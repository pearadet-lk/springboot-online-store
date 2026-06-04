package com.onlinestore.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class PathRewriteRequestWrapper extends HttpServletRequestWrapper {

    private final String rewrittenUri;

    public PathRewriteRequestWrapper(HttpServletRequest request, String rewrittenUri) {
        super(request);
        this.rewrittenUri = rewrittenUri;
    }

    @Override
    public String getRequestURI() {
        return rewrittenUri;
    }

    @Override
    public String getServletPath() {
        return rewrittenUri;
    }
}
