/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.example.server;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Manager extends Application {

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws IOException {
        primaryStage.setTitle("WildFly Server Manager");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("wildfly_icon_256px.png")));
        final Parent root = FXMLLoader.load(getClass().getResource("manager.fxml"));

        primaryStage.setScene(new Scene(root));

        primaryStage.show();
    }

    @Override
    public void stop() {
        ManagerController.shutdown();
    }
}
