package com.gluonhq.bck;

import javafx.animation.*;
import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;


public class App extends Application {

    private static final boolean isWeb = System.getProperty("java.vendor", "none").equalsIgnoreCase("bck2brwsr");

    public void start(Stage stage) {
        System.out.println( "Hello FX, start!" );
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label label = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
Group root = new Group();
Rectangle r = new Rectangle(30,50,250,250);
r.setFill(Color.YELLOW);
 Rectangle r2 = new Rectangle(10,20,50,50);
 r2.setFill(Color.BLUE);
  r2.setTranslateX(200);
Circle c = new Circle(40);
c.setCenterX(300);
c.setCenterY(60);
c.setFill(Color.RED);
      TranslateTransition tt = new TranslateTransition(Duration.millis(10000), r2);
     tt.setByX(200f);
//     Button b = new Button("CLICKME");
//     b.setTranslateY(350);
//     tt.setCycleCount(4f);
//     tt.setAutoReverse(true);
 
        // VBox root = new VBox(30, r, r2);
        // VBox root = new VBox(30, r);
        // root.setAlignment(Pos.CENTER);
root.getChildren().add(r);
root.getChildren().add(r2);
root.getChildren().add(c);
//root.getChildren().add(b);

        Scene scene = new Scene(root, 640, 480);
scene.setFill(Color.GREEN);

/*

        TranslateTransition animation = new TranslateTransition(
                Duration.seconds(5.), r
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.setByX(200);
        animation.setAutoReverse(true);
*/


        // ImageView imageView = new ImageView(new Image(HelloFX.class.getResourceAsStream("/hellofx/openduke.png")));
        // imageView.setFitHeight(200);
        // imageView.setPreserveRatio(true);

        // VBox root = new VBox(30, imageView, label);
        // VBox root = new VBox(label);
        // root.setAlignment(Pos.CENTER);
        // Scene scene = new Scene(root, 640, 480);
        // scene.getStylesheets().add(HelloFX.class.getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        tt.play();

        System.out.println( "Hello FX, start done!" );
/*
animation.playFromStart();
Thread t = new Thread() {
@Override public void run() {
    myloop();
}
};
System.out.println("START myloopTHREAD");
t.start();
*/
    }

    public void myloop () {
System.out.println("In myloopTHREAD");
        for (int i = 0; i < 100; i++) {
            System.err.println("[MYLOOP] iteration " + i);
            try {
                Thread.sleep(1000);
            }
            catch (Exception e) {
e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
System.out.println("Hello, JavaFX! In Main");
String osname = System.getProperty("os.name");
String vendor = System.getProperty("java.vendor");
String vendor2 = System.getProperty("java.vendor", "none");
boolean vendor3 = System.getProperty("java.vendor", "none").equalsIgnoreCase("bck2brwsr");
System.out.println("[HelloFX main] osname = " + osname+" and vendor = " + vendor+" and vendor2 = " + vendor2+" and vendor 3 = " + vendor3);
System.out.println("[HelloFX main] web? " + isWeb);
System.setProperty("prism.order", "es2");
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
