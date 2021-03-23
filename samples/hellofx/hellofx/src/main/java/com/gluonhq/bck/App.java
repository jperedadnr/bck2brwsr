package com.gluonhq.bck;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class App extends Application {
    public void start(Stage stage) {
        System.out.println( "Hello FX, start!" );
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");

        // ImageView imageView = new ImageView(new Image(HelloFX.class.getResourceAsStream("/hellofx/openduke.png")));
        // imageView.setFitHeight(200);
        // imageView.setPreserveRatio(true);

        // VBox root = new VBox(30, imageView, label);
        VBox root = new VBox(label);
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root, 640, 480);
        // scene.getStylesheets().add(HelloFX.class.getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        System.out.println( "Hello FX, start done!" );
    }

    public static void main(String[] args) {
        System.out.println( "Hello FX, main!" );
        launch(args);
    }

}
