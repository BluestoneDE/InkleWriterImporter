package inkle.gui;

import com.google.gson.GsonBuilder;
import inkle.json.Option;
import inkle.json.Stitch;
import inkle.json.Story;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextToInkleController {
    private String text;
    private boolean copyMode = false;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextArea textArea;
    @FXML
    private Button button;

    @FXML
    private void initialize() {
        // drag and drop text files
        rootPane.setOnDragOver(event -> {
            if (event.getGestureSource() != rootPane && event.getDragboard().hasFiles())
                event.acceptTransferModes(copyMode ? TransferMode.NONE : TransferMode.COPY_OR_MOVE);
            event.consume();
        });
        rootPane.setOnDragDropped(event -> {
            event.setDropCompleted(loadFile(event.getDragboard().getFiles().get(0)));
            event.consume();
        });

        // configure text area and fill with default text
        textArea.setFont(Font.font("Consolas Bold", 18));
        textArea.setText("Stanley's Doors | the narrator\n\n" +
                "This is the story of a man named Stanley. " +
                "Stanley worked for a company in a big building where he was Employee Number 427. " +
                "One day he got up from his desk, and stepped out of his office. \n" +
                "When Stanley came to a set of two open doors, he entered the door on his left.\n" +
                "* enter the left door\n" +
                "\tStanley entered the left door like he was told.\n" +
                "\tGood boy Stanley\n" +
                "* enter the right door\n" +
                "\tStanley ignored the narrator and entered the wrong door.\n" +
                "* do nothing\n" +
                "What happened next?\n" +
                "* end the story\n" +
                "* end the story but different\n" +
                "\tSomething was different and Stanley could feel it.\n" +
                "The story Ended!\n");
    }

    @FXML
    private void convert() {
        if (copyMode) {
            // change back to text
            textArea.setText(text);
            button.setText("convert");
        } else {
            // read text and decide to convert
            text = textArea.getText();
            if (text.length() == 0 || text.startsWith("{")) return;

            // clear keys and decide on format
            Stitch.keyList.clear();
            int author = text.indexOf("# author: ");
            // it's ink if first comment is the title and author can be found within first 1000 positions
            boolean ink_format = text.split("\n", 2)[0].matches("^// -+ .* ----") &&
                    0 < author && author < 1000;
            String JsonText = ink_format ? convertInkFormat(text) : convertOwnFormat(text);
            if (JsonText == null) return;
            textArea.setText(JsonText);
            button.setText("go back");
        }
        textArea.setEditable(copyMode);
        copyMode = !copyMode;
        selectAll();
    }

    private String convertInkFormat(String text) {
        Story story = new Story();

        // split text by line-breaks
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("\n")));
        lines.replaceAll(s -> s.replaceAll("<>", "")); // ink format has these for some reason
        lines.replaceAll(String::trim);
        // remove empty lines and comments and todos
        lines.removeIf(line -> line.length() == 0 || line.startsWith("//") || line.startsWith("TODO:"));

        // set author and title
        if (lines.get(1).startsWith("# author: ")) {
            story.setAuthor(lines.get(1).substring(10));
            lines.remove(1);
        }
        if (lines.get(0).startsWith("# ")) {
            String title = lines.get(0).substring(2);
            if (title.startsWith("title: ")) title = title.substring(7);
            story.setTitle(title);
            lines.remove(0);
        }

        // the first direct should be initial
        if (lines.get(0).startsWith("-> ")) {
            String initial = lines.get(0).substring(3);
            if (initial.length() > Stitch.keyLength) initial = initial.substring(0, Stitch.keyLength);
            story.data.setInitial(initial);
            story.data.editorData.setPlayPoint(initial);
            lines.remove(0);
        }

        Option currentOption = null;
        Stitch currentStitch = new Stitch();
        currentStitch.addPage(1);

        // interpret each line and create stitches
        for (String line : lines) {

            // if this is a key to the next line
            if (line.matches("^=+ .+")) {
                Pattern pattern = Pattern.compile("^=+ (\\w+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String key = matcher.group(1);
                    if (currentStitch.hasPage()) {
                        currentStitch.setKeyOnce(key);
                        continue;
                    }
                    if (currentStitch.hasKey()) story.data.addStitch(currentStitch);
                    currentStitch = new Stitch();
                    currentStitch.setKeyOnce(key);
                    continue;
                }
            }

            // if it is an option
            if (line.startsWith("+ ")) {
                currentOption = new Option(line.substring(2));
                currentStitch.addOption(currentOption);
                continue;
            }

            // if this is a divert or link
            if (line.startsWith("-> ")) {
                // add link to previous option
                if (currentOption != null && !currentOption.hasLinkPath()) {
                    currentOption.setLinkPath(line.substring(3));
                    continue;
                }

                // end
                if (line.equals("-> END")) {
                    story.data.addStitch(currentStitch);
                    continue;
                }

                // divert
                String divert = line.substring(3);
                if (divert.length() > Stitch.keyLength && !Character.isDigit(divert.charAt(16)))
                    divert = divert.substring(0, Stitch.keyLength);
                currentStitch.addDivert(divert);
                story.data.addStitch(currentStitch);
                currentStitch = new Stitch();
                continue;
            }

            // add prompt or make new stitch
            if (currentStitch.hasPrompt()) {
                Stitch nextStitch = new Stitch(line);
                currentStitch.addDivert(nextStitch.getKey());
                story.data.addStitch(currentStitch);
                currentStitch = nextStitch;
            } else {
                currentStitch.addPrompt(line);
            }
            currentOption = null;
        }

        return new GsonBuilder().serializeNulls().create().toJson(story);
    }

    private String convertOwnFormat(String text) {
        // split text by line-breaks and set title
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("\n")));
        String[] title = lines.get(0).split("[ \\t]+\\|+[ \t]*", 2);
        Story story = new Story(title[0]);
        lines.remove(0);
        if (title.length > 1) story.data.editorData.setAuthorName(title[1]);
        // remove empty lines and comments and todos
        lines.removeIf(line -> line.length() == 0 || line.startsWith("//"));

        // early abort
        if (lines.size() == 0) return null;

        // interpret each line and create stitches
        Stitch currentStitch = null, previousStitch;
        String initialStitchKey = null;
        ArrayList<Option> unlinkedOptions = new ArrayList<>();
        ArrayList<Stitch> responseStitches = new ArrayList<>();
        for (String line : lines) {
            System.out.println(line);
            // add initial stitch
            if (currentStitch == null) {
                currentStitch = new Stitch(line);
                currentStitch.addPage(1);
                story.data.addStitch(currentStitch);
                initialStitchKey = currentStitch.getKey();
                continue;
            }

            if (!currentStitch.hasPrompt()) currentStitch.addPrompt(line);

            if ((line.startsWith("*") || line.startsWith("-")) && currentStitch.content.size() > 0) {
                // it's an option
                String option = line.substring(1).trim();
                if (option.length() == 0) continue;
                Option newOption = new Option(option);
                unlinkedOptions.add(newOption);
                currentStitch.addOption(newOption);
                continue;
            }

            if (line.startsWith("\t")) {
                String response = line.substring(1).trim();
                if (response.length() == 0) continue;
                Stitch responseStitch = new Stitch(response);
                if (!unlinkedOptions.isEmpty()) {
                    // it's a response to a previous option
                    int optionIndex = unlinkedOptions.size() - 1;
                    unlinkedOptions.get(optionIndex).setLinkPath(responseStitch.getKey());
                    unlinkedOptions.remove(optionIndex);
                } else {
                    // it's a response to the previous response
                    responseStitches.get(responseStitches.size()-1).addDivert(responseStitch.getKey());
                }
                story.data.addStitch(responseStitch);
                responseStitches.add(responseStitch);
                continue;
            }

            // add current stitch to story and make new one
            story.data.addStitch(currentStitch);
            previousStitch = currentStitch;
            currentStitch = new Stitch(line);
            // add divert to previous stitch if needed
            if (!previousStitch.hasDivert() && !previousStitch.hasOptions())
                previousStitch.addDivert(currentStitch.getKey());
            // link up responses and unlinked options to new stitch
            responseStitches.removeIf(Stitch::hasDivert);
            for (Stitch response : responseStitches) response.addDivert(currentStitch.getKey());
            responseStitches.clear();
            for (Option option : unlinkedOptions) option.setLinkPath(currentStitch.getKey());
            unlinkedOptions.clear();
        }
        story.data.addStitch(currentStitch);
        for (Option option : unlinkedOptions) option.setLinkPath(currentStitch.getKey());
        unlinkedOptions.clear();
        // abort if there are no stitches
        if (story.data.stitches.size() == 0) return null;

        // change first stitch
        if (initialStitchKey == null) initialStitchKey = story.data.stitches.entrySet().iterator().next().getKey();
        story.data.setInitial(initialStitchKey);
        story.data.editorData.setPlayPoint(initialStitchKey);
        return new GsonBuilder().serializeNulls().create().toJson(story);
    }

    private boolean loadFile(File file) {
        if (copyMode || file == null) return false;
        boolean success = false;
        try {
            Path path = file.toPath();
            if (file.toString().endsWith(".ink") || Files.probeContentType(path).startsWith("text")) {
                BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                StringBuilder fullText = new StringBuilder();
                int i;
                while ((i = reader.read()) != -1) fullText.append((char) i);
                textArea.setText(fullText.toString());
                success = true;
            }
        } catch (Exception ignored) {
        }
        return success;
    }

    @FXML
    private void selectAll() {
        if (copyMode) {
            textArea.requestFocus();
            textArea.selectAll();
            textArea.copy();
        }
    }
}
