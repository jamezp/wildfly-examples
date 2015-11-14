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

import java.io.OutputStream;
import java.nio.file.Path;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.wildfly.core.launcher.StandaloneCommandBuilder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class StandaloneServerStartService extends Service<Server> {
    private final Path wildflyHome;
    private final String bindAddress;
    private final long timeout;
    private final OutputStream out;

    public StandaloneServerStartService(final Path wildflyHome, final String bindAddress, final long timeout, final OutputStream out) {
        this.wildflyHome = wildflyHome;
        this.bindAddress = bindAddress;
        this.timeout = timeout;
        this.out = out;
    }

    @Override
    protected Task<Server> createTask() {
        return new Task<Server>() {
            @Override
            protected Server call() throws Exception {
                final StandaloneCommandBuilder commandBuilder = StandaloneCommandBuilder.of(wildflyHome)
                        // Give a hint for the address the server should listen on
                        .setBindAddressHint(bindAddress)
                        // Give a hint to the same address the management server should listen on
                        .setBindAddressHint("management", bindAddress)
                        // Don't add the color pattern to the console output
                        .addJavaOption("-Dorg.jboss.logmanager.nocolor=true");
                final Server server = Server.create(commandBuilder, bindAddress, out);
                server.start(timeout);
                return server;
            }
        };
    }
}
