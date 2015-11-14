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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ManagerController implements Initializable {

    private static final String BIND_ADDRESS_KEY = "bind.address";
    private static final String LAST_DIR_KEY = "last.dir";
    private static final String LAST_WILDFLY_HOME_KEY = "last.wildfly.home";
    private static final String TIMEOUT_KEY = "startup.timeout";

    private static final AtomicReference<Server> runningServer = new AtomicReference<>();

    @FXML
    private Button browseButton;

    @FXML
    private Button startStandaloneButton;

    @FXML
    private Button startDomainButton;

    @FXML
    private Button stopButton;

    @FXML
    private TextField wildflyHomeText;

    @FXML
    private TextArea consoleOutputTextArea;

    @FXML
    private TextArea serverConsoleOutputTextArea;

    @FXML
    private TextField serverTimeoutField;

    @FXML
    private TextField bindAddressField;

    private final Preferences preferences;
    private final DirectoryChooser directoryChooser;

    public ManagerController() {
        preferences = Preferences.userNodeForPackage(ManagerController.class);
        directoryChooser = new DirectoryChooser();
    }

    static void shutdown() {
        final Server server = runningServer.get();
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Validates the path is a valid WildFly directory by checking for a {@code jboss-modules.jar}.
     *
     * @param path the path to check
     *
     * @return {@code true} if the path appears to be a valid WildFly directory, otherwise {@code false}.
     */
    private static boolean isValidWildFlyHome(final Path path) {
        return Files.exists(path.resolve("jboss-modules.jar"));
    }

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {

        // Configure the last directory browsed and the last WildFly directory used
        final String defaultDir = preferences.get(LAST_DIR_KEY, System.getProperty("user.home"));
        final String lastWildFlyHome = preferences.get(LAST_WILDFLY_HOME_KEY, null);
        if (lastWildFlyHome != null) {
            wildflyHomeText.setText(lastWildFlyHome);
            if (isValidWildFlyHome(Paths.get(wildflyHomeText.getText()))) {
                updateStartUI(false);
            }
        }

        // Validate or invalidate buttons based on the value of the WildFly Home
        wildflyHomeText.textProperty().addListener((observable, oldValue, newValue) -> {
            // Attempt to see if we should enable the buttons
            if (newValue.isEmpty()) {
                updateStartButtons(true);
            } else {
                final Path path = Paths.get(newValue);
                if (Files.exists(path) && isValidWildFlyHome(path)) {
                    preferences.put(LAST_WILDFLY_HOME_KEY, path.toAbsolutePath().toString());
                    updateStartButtons(false);
                } else {
                    updateStartButtons(true);
                }
            }
        });

        directoryChooser.setInitialDirectory(new File(defaultDir));

        // Configure the timeout field
        final Pattern longPattern = Pattern.compile("[0-9]+");
        serverTimeoutField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (longPattern.matcher(newValue).matches()) {
                serverTimeoutField.setText(newValue);
                preferences.putLong(TIMEOUT_KEY, Long.parseLong(newValue));
            } else if (!newValue.isEmpty()) {
                serverTimeoutField.setText(oldValue);
            }
        });
        serverTimeoutField.setText(Long.toString(preferences.getLong(TIMEOUT_KEY, 60L)));

        // Configure the bind address
        bindAddressField.setText(preferences.get(BIND_ADDRESS_KEY, "127.0.0.1"));

        // Set-up a redirect for the console output
        final PrintStream stdout = new PrintStream(new TextAreaOutputStream(consoleOutputTextArea));
        System.setOut(stdout);
        System.setErr(stdout);
    }

    @FXML
    protected void startStandalone() {
        final Path wildflyHome = getWildFlyHome();
        if (wildflyHome != null) {
            final Service<Server> serverService = new StandaloneServerStartService(wildflyHome, getBindAddress(), getTimeout(), new TextAreaOutputStream(serverConsoleOutputTextArea));
            start(serverService);
        }
    }

    @FXML
    protected void startDomain() {
        final Path wildflyHome = getWildFlyHome();
        if (wildflyHome != null) {
            final Service<Server> serverService = new DomainServerStartService(wildflyHome, getBindAddress(), getTimeout(), new TextAreaOutputStream(serverConsoleOutputTextArea));
            start(serverService);
        }
    }

    @FXML
    protected void stop() {
        final Server server = runningServer.getAndSet(null);
        if (server != null) {
            stopButton.setDisable(true);
            final Service<Void> stopService = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            server.stop();
                            return null;
                        }
                    };
                }
            };
            stopService.setOnFailed(workerStateEvent -> {
                // TODO (jrp) ensure the server is destroyed
                alertException(workerStateEvent.getSource());
                updateStartUI(false);
            });
            stopService.setOnSucceeded(workerStateEvent -> updateStartUI(false));
            stopService.start();
        }
    }

    @FXML
    protected void browse() {
        final File dir = directoryChooser.showDialog(wildflyHomeText.getScene().getWindow());
        if (dir != null) {
            if (isValidWildFlyHome(dir.toPath())) {
                wildflyHomeText.setText(dir.getAbsolutePath());
                final File lastDir = dir.getParentFile();
                if (lastDir != null) {
                    preferences.put(LAST_DIR_KEY, lastDir.getAbsolutePath());
                    directoryChooser.setInitialDirectory(lastDir);
                }
                updateStartUI(false);
            } else {
                alertInvalidWildFlyHone(dir.getAbsolutePath());
            }
        }
    }

    private String getBindAddress() {
        String result = bindAddressField.getText();
        if (result.isEmpty()) {
            result = "127.0.0.1";
        }
        return result;
    }

    private long getTimeout() {
        final String timeout = serverTimeoutField.getText();
        if (timeout.isEmpty()) {
            return 60L;
        }
        return Long.parseLong(timeout);
    }

    private Path getWildFlyHome() {
        final String value = wildflyHomeText.getText();
        if (value.isEmpty()) {
            updateStartUI(true);
            alertInvalidWildFlyHone(value);
            return null;
        }
        final Path path = Paths.get(value);
        if (Files.notExists(path) || !isValidWildFlyHome(path)) {
            alertInvalidWildFlyHone(value);
            return null;
        }
        return path;
    }

    private void start(final Service<Server> service) {
        updateStartUI(true);
        serverConsoleOutputTextArea.clear();
        service.setOnFailed(workerStateEvent -> {
            alertException(workerStateEvent.getSource());
            updateStartUI(false);
        });
        service.setOnSucceeded(workerStateEvent -> {
            final Server server = (Server) workerStateEvent.getSource().getValue();
            if (runningServer.compareAndSet(null, server)) {
                stopButton.setDisable(false);
            } else {
                server.kill();
                // Get the current running server
                final Server current = runningServer.get();
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Server Already Running");
                alert.setContentText("A server is already running. Running server: " + current);
                alert.setResizable(true);
                // Set the preferred size otherwise it seems to be too small and cut off the text on Linux
                alert.getDialogPane().setPrefSize(480.0, 320.0);
                alert.showAndWait();
            }
        });
        service.start();
    }

    private void updateStartButtons(final boolean disable) {
        startStandaloneButton.setDisable(disable);
        startDomainButton.setDisable(disable);
    }

    private void updateStartUI(final boolean disable) {
        startStandaloneButton.setDisable(disable);
        startDomainButton.setDisable(disable);
        browseButton.setDisable(disable);
        wildflyHomeText.setDisable(disable);
    }

    private void alertInvalidWildFlyHone(final String wildflyHome) {
        updateStartButtons(true);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Invalid WildFly Home directory");
        alert.setContentText(String.format("The WildFly Home directory %s is expected to have a jboss-modules.jar.", wildflyHome));
        alert.setResizable(true);
        // Set the preferred size otherwise it seems to be too small and cut off the text on Linux
        alert.getDialogPane().setPrefSize(480.0, 320.0);
        alert.showAndWait();
    }


    private void alertException(final Worker<?> worker) {
        if (worker == null) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception");
            alert.setHeaderText("Error found during processing");
            alert.setContentText("Error could not be determined as worker was null");
        } else {
            alertException(worker.getException());
        }
    }

    private void alertException(final Throwable t) {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception");
        alert.setHeaderText("Error found during processing");
        alert.setContentText(t.getMessage());

        // Print the cause
        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        final Label label = new Label("Stack Trace:");

        final TextArea textArea = new TextArea(writer.toString());
        textArea.setEditable(false);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(label, 0, 0);
        content.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(content);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }

    private static class TextAreaOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate;
        private final TextArea consoleOutput;

        private TextAreaOutputStream(final TextArea consoleOutput) {
            this.consoleOutput = consoleOutput;
            delegate = new ByteArrayOutputStream(1024);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            delegate.write(b);
            flush();
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            delegate.write(b, off, len);
            flush();
        }

        @Override
        public void write(final int b) throws IOException {
            delegate.write((char) b);
            flush();
        }

        @Override
        public void flush() throws IOException {
            // There is potential that this could flood the task queue and make the GUI unresponsive
            Platform.runLater(() -> {
                synchronized (delegate) {
                    consoleOutput.appendText(delegate.toString());
                    delegate.reset();
                }
            });
        }
    }
}
