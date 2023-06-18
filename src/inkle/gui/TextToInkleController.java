package inkle.gui;

import com.google.gson.GsonBuilder;
import inkle.json.Option;
import inkle.json.Stitch;
import inkle.json.Story;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;

import java.io.BufferedInputStream;
import java.io.InputStream;
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
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        });
        rootPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                try {
                    Path file = db.getFiles().get(0).toPath();
                    if (file.toString().endsWith(".ink") || Files.probeContentType(file).startsWith("text")) {
                        InputStream is = new BufferedInputStream(Files.newInputStream(file));
                        int i;
                        StringBuilder fullText = new StringBuilder();
                        while ((i = is.read()) != -1)
                            fullText.append((char) i);
                        textArea.setText(fullText.toString());
                        success = true;
                    }
                } catch (Exception ignored) {
                }
            }
            event.setDropCompleted(success);
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

            boolean inky_format = text.indexOf("inky format") > 0;
            String JsonText = inky_format ? convertInkyFormat(text) : convertOwnFormat(text);
            if (JsonText == null) return;
            textArea.setText(JsonText);
            button.setText("go back");
        }
        textArea.setEditable(copyMode);
        copyMode = !copyMode;
        selectAll();
    }

    private String convertInkyFormat(String text) {
        Story story = new Story();

        // split text by line-breaks
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("\n")));
        lines.replaceAll(String::trim); // trim all lines
        lines.removeIf(line -> line.length() == 0 || line.startsWith("//")); // remove empty lines and comments

        // set author and title
        if (lines.get(1).startsWith("# author: ")) {
            story.setAuthor(lines.get(1).substring(10));
            lines.remove(1);
        }
        if (lines.get(0).startsWith("# ")) {
            story.setTitle(lines.get(0).substring(2));
            lines.remove(0);
        }

        // the first direct should be initial
        if (lines.get(0).startsWith("-> ")) {
            String initial = lines.get(0).substring(3);
            if (initial.length() > Stitch.keyLength)
                initial = initial.substring(0, Stitch.keyLength);
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
                    if (currentStitch.hasKey())
                        story.data.addStitch(currentStitch);
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

        // early abort
        if (lines.size() == 0) return null;

        // interpret each line and create stitches
        Stitch currentStitch = null, previousStitch;
        String initialStitchKey = null;
        ArrayList<Option> unlinkedOptions = new ArrayList<>();
        ArrayList<Stitch> responseStitches = new ArrayList<>();
        for (String line : lines) {
            if (line.length() == 0 || line.startsWith("//")) continue; // skip empty lines and comments

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
                String option = line.substring(1);
                if (option.length() == 0) continue;
                if (option.startsWith(" ")) option = option.substring(1);
                if (option.length() == 0) continue;
                Option newOption = new Option(option);
                unlinkedOptions.add(newOption);
                currentStitch.addOption(newOption);
                continue;
            }

            if (line.startsWith("\t") && !unlinkedOptions.isEmpty()) {
                // it's a response to a previous option
                String response = line.substring(1);
                if (response.length() == 0) continue;
                if (response.startsWith(" ")) response = response.substring(1);
                if (response.length() == 0) continue;
                Stitch responseStitch = new Stitch(response);
                int optionIndex = unlinkedOptions.size() - 1;
                unlinkedOptions.get(optionIndex).setLinkPath(responseStitch.getKey());
                unlinkedOptions.remove(optionIndex);
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
            for (Stitch response : responseStitches) response.addDivert(currentStitch.getKey());
            responseStitches.clear();
            for (Option option : unlinkedOptions) option.setLinkPath(currentStitch.getKey());
            unlinkedOptions.clear();
        }
        if (currentStitch != null) {
            story.data.addStitch(currentStitch);
            for (Option option : unlinkedOptions) option.setLinkPath(currentStitch.getKey());
            unlinkedOptions.clear();
        }
        // abort if there are no stitches
        if (story.data.stitches.size() == 0) return null;

        // change first stitch
        if (initialStitchKey == null) initialStitchKey = story.data.stitches.entrySet().iterator().next().getKey();
        story.data.setInitial(initialStitchKey);
        story.data.editorData.setPlayPoint(initialStitchKey);
        return new GsonBuilder().serializeNulls().create().toJson(story);
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
