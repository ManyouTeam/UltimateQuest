package cn.superiormc.ultimatequest.editor;

import java.util.HashMap;
import java.util.Map;

public class EditorContext {

    private final Map<String, EditorTarget> drafts = new HashMap<>();

    private EditorScope scope;

    private EditorTarget target;

    private String path = "";

    private int page = 0;

    public EditorTarget draft(EditorScope scope, String id) {
        return drafts.get(scope.name() + "/" + id);
    }

    public void remember(EditorTarget target) {
        drafts.put(target.getScope().name() + "/" + target.getId(), target);
    }

    public EditorScope getScope() {
        return scope;
    }

    public void setScope(EditorScope scope) {
        this.scope = scope;
    }

    public EditorTarget getTarget() {
        return target;
    }

    public void setTarget(EditorTarget target) {
        this.target = target;
    }

    public String getPath() {
        return path == null ? "" : path;
    }

    public void setPath(String path) {
        this.path = path == null ? "" : path;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }
}
