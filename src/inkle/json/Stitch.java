package inkle.json;

import java.util.ArrayList;
import java.util.Arrays;

public class Stitch {
    public final static int keyLength = 16;
    public static ArrayList<String> keyList = new ArrayList<>();
    public ArrayList<Object> content;
    private transient String key = null;
    private transient boolean prompt, page, divert, options;

    public Stitch() {
        content = new ArrayList<>();
    }

    public Stitch(String start) {
        this();
        addPrompt(start);
    }

    public Stitch(ArrayList<Object> content) {
        setContent(content);
    }

    public Stitch(String start, Option[] options, int pageNum) {
        this(start);
        content.addAll(Arrays.asList(options));
        //content.add(end);
        content.add(new PageNum(pageNum));
    }

    public ArrayList<Object> getContent() {
        return content;
    }

    public void setContent(ArrayList<Object> content) {
        this.content = content;
    }

    public void addPrompt(String prompt) {
        if (this.prompt) return;
        content.add(0, prompt.trim());
        this.prompt = true;
        setKeyOnce(toCamelCase(prompt));
    }

    public void setKeyOnce(String key) {
        // @todo there might be a bug if the key just happens to have a number at 'keyLength' position
        if (this.key != null) return;
        int counter = 1;
        if (key.length() > keyLength && !Character.isDigit(key.charAt(keyLength)))
            key = key.substring(0, keyLength);
        if (keyList.contains(key)) {
            if (key.length() > keyLength) {
                counter += Integer.parseInt(key.substring(keyLength, keyLength+1));
                key = key.substring(0, keyLength);
            }
            setKeyOnce(key + counter);
            return;
        }
        this.key = key;
        keyList.add(key);
    }

    public void addOption(Option option) {
        options = true;
        content.add(option);
    }


    public void addPage(int num) {
        addPage(new PageNum(num));
    }

    public void addPage(PageNum pageNum) {
        if (page) return;
        content.add(pageNum);
        page = true;
    }

    public void addDivert(String linkPath) {
        addDivert(new Divert(linkPath));
    }

    public void addDivert(Divert divert) {
        if (this.divert) return;
        content.add(divert);
        this.divert = true;
    }

    @Deprecated
    public void addContent(Object o) {
        content.add(o);
    }

    public String getKey() {
        return key;
    }

    public boolean hasKey() {
        return key != null && key.length() > 0;
    }

    public boolean hasOptions() {
        return options;
    }

    public boolean hasPrompt() {
        return prompt;
    }

    public boolean hasPage() {
        return page;
    }

    public boolean hasDivert() {
        return divert;
    }

    public static String toCamelCase(String s) {
        String[] parts = s.split("\\W*[_ ]|\\W");
        StringBuilder camelCaseString = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() == 0) continue;
            camelCaseString.append(i == 0 ? part.toLowerCase() : toProperCase(part));
        }
        return camelCaseString.toString();
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}

