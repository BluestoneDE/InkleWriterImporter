package inkle.json;

public class Story {
    public String title = "My Story";
    public Data data;

    public int url_key = 163485;

    public Story() {
        data = new Data();
        url_key = (int) (1000000 * Math.random());
    }

    public Story(String title) {
        this();
        setTitle(title);
    }

    public Story(String title, Data data, int url_key) {
        this(title);
        setData(data);
        setUrlKey(url_key);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        try {
            data.editorData.setAuthorName(author);
        } catch (Exception ignored) {
        }
    }

    public String getAuthor() {
        try {
            return data.editorData.getAuthorName();
        } catch (Exception ignored) {
            return null;
        }
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public int getUrl_key() {
        return url_key;
    }

    public void setUrlKey(int url_key) {
        this.url_key = url_key;
    }
}
