package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.TimeUnit;

public class Main extends Application {

    private Controller controller = null;
    private FXMLLoader fxmlLoader = null;

    @Override
    public void stop() {
        System.out.println("We caught the exit condition!");

        if (fxmlLoader == null)
            fxmlLoader = new FXMLLoader();

        try {
            fxmlLoader.load(getClass().getResource("sample.fxml"));
        }
        catch (Exception e){
            e.printStackTrace();
        }

        if (controller == null)
            controller = fxmlLoader.getController();

        // Shut down threads and wait (max 30 seconds) for them to
        // shut down.
        controller.executorService.shutdownNow();
        try {
            controller.executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (Exception e) {
        }

        if (!controller.executorService.isTerminated())
            System.err.println("Error shutting down threads!");
        else
            System.out.println("*** Successfully shutdown threads ***");

        Platform.exit();

    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        fxmlLoader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        primaryStage.setTitle("Cipher");
        primaryStage.setScene(new Scene(root));
        primaryStage.sizeToScene();
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
