package inkle.json;

import java.util.Hashtable;

public class Data {
    public Hashtable<String, Stitch> stitches;
    public String initial = "onceUponATime";
    public boolean optionMirroring = true;
    public boolean allowCheckpoints = false;
    public EditorData editorData;

    public Data() {
        stitches = new Hashtable<>();
        editorData = new EditorData();
    }

    public Data(Hashtable<String, Stitch> stitches, String initial, boolean optionMirroring, boolean allowCheckpoints, EditorData editorData) {
        this.stitches = stitches;
        this.initial = initial;
        this.optionMirroring = optionMirroring;
        this.allowCheckpoints = allowCheckpoints;
        this.editorData = editorData;
    }

    public Hashtable<String, Stitch> getStitches() {
        return stitches;
    }

    public void setStitches(Hashtable<String, Stitch> stitches) {
        this.stitches = stitches;
    }


    public void addStitch(Stitch stitch) {
        addStitch(stitch.getKey(), stitch);
    }

    public void addStitch(String key, Stitch stitch) {
        stitches.putIfAbsent(key, stitch);
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public boolean isOptionMirroring() {
        return optionMirroring;
    }

    public void setOptionMirroring(boolean optionMirroring) {
        this.optionMirroring = optionMirroring;
    }

    public boolean isAllowCheckpoints() {
        return allowCheckpoints;
    }

    public void setAllowCheckpoints(boolean allowCheckpoints) {
        this.allowCheckpoints = allowCheckpoints;
    }

    public EditorData getEditorData() {
        return editorData;
    }

    public void setEditorData(EditorData editorData) {
        this.editorData = editorData;
    }
}
