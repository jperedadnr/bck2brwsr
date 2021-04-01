package com.gluonhq.bck;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class App extends Application {

    private static final boolean isWeb = System.getProperty("java.vendor", "none").equalsIgnoreCase("bck2brwsr");

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
System.out.println("Hello, JavaFX! In Main");
String osname = System.getProperty("os.name");
String vendor = System.getProperty("java.vendor");
String vendor2 = System.getProperty("java.vendor", "none");
boolean vendor3 = System.getProperty("java.vendor", "none").equalsIgnoreCase("bck2brwsr");
System.out.println("[HelloFX main] osname = " + osname+" and vendor = " + vendor+" and vendor2 = " + vendor2+" and vendor 3 = " + vendor3);
System.out.println("[HelloFX main] web? " + isWeb);
System.setProperty("prism.debug", "true");
System.setProperty("prism.verbose", "true");
System.setProperty("javafx.verbose", "true");
System.setProperty("glass.platform", "Web");
System.setProperty("glass.disableThreadChecks", "true");
System.setProperty("quantum.debug", "true");
System.out.println("Hello, JavaFX! Launch!");
// subOne();
String[] dooh = new String[1];
dooh[0] = "dooh";
        launch(App.class, dooh);
System.out.println("Hello, JavaFX! Out Main");

    }

}
