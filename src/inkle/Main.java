package inkle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("gui/TextToInkle.fxml")), 764, 551));
        primaryStage.setTitle("Text to Inkle Writer Importer");
        primaryStage.getIcons().add(new Image("/inkle.png"));
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(420);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
