package inkle.json;

public class EditorData {
    public String playPoint = "onceUponATime";
    public boolean libraryVisible = false;
    public String authorName = "Anonymous";
    public int textSize = 0;

    public EditorData() {
    }

    public EditorData(String playPoint, boolean libraryVisible, String authorName, int textSize) {
        this.playPoint = playPoint;
        this.libraryVisible = libraryVisible;
        this.authorName = authorName;
        this.textSize = textSize;
    }

    public String getPlayPoint() {
        return playPoint;
    }

    public void setPlayPoint(String playPoint) {
        this.playPoint = playPoint;
    }

    public boolean isLibraryVisible() {
        return libraryVisible;
    }

    public void setLibraryVisible(boolean libraryVisible) {
        this.libraryVisible = libraryVisible;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }
}
