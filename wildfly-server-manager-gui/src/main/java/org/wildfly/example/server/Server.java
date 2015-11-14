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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.logging.Logger;
import org.wildfly.core.launcher.CommandBuilder;
import org.wildfly.core.launcher.DomainCommandBuilder;
import org.wildfly.core.launcher.ProcessHelper;
import org.wildfly.core.launcher.StandaloneCommandBuilder;
import org.wildfly.plugin.core.ContainerDescription;
import org.wildfly.plugin.core.ServerHelper;
import org.wildfly.plugin.core.ServerProcess;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class);

    private final CommandBuilder commandBuilder;
    private final OutputStream stdout;
    private final ModelControllerClient client;
    private volatile ContainerDescription containerDescription;
    private ServerProcess process;

    private Server(final CommandBuilder commandBuilder, final ModelControllerClient client, final OutputStream stdout) {
        this.commandBuilder = commandBuilder;
        this.stdout = stdout;
        this.client = client;
    }

    static Server create(final CommandBuilder commandBuilder, final String address, final OutputStream stdout) throws UnknownHostException {
        Objects.requireNonNull(commandBuilder, "The commandBuilder cannot be null");
        final ModelControllerClient client = ModelControllerClient.Factory.create(address, 9990);
        final Server server;
        if (commandBuilder instanceof DomainCommandBuilder) {
            final DomainClient domainClient = DomainClient.Factory.create(client);
            server = new Server(commandBuilder, domainClient, stdout) {

                @Override
                protected void stopServer() throws IOException {
                    ServerHelper.shutdownDomain(domainClient);
                    safeClose(domainClient);
                }

                @Override
                protected void waitForStart(final Process process, final long timeout) throws IOException, InterruptedException, TimeoutException {
                    ServerHelper.waitForDomain(process, domainClient, timeout);
                }

                @Override
                public boolean isRunning() {
                    return ServerHelper.isDomainRunning(domainClient);
                }

                @Override
                public String toString() {
                    return "Domain: " + getRunningVersion();
                }
            };
        } else if (commandBuilder instanceof StandaloneCommandBuilder) {
            server = new Server(commandBuilder, client, stdout) {

                @Override
                protected void stopServer() throws IOException {
                    ServerHelper.shutdownStandalone(client);
                    safeClose(client);
                }

                @Override
                protected void waitForStart(final Process process, final long timeout) throws IOException, InterruptedException, TimeoutException {
                    ServerHelper.waitForStandalone(process, client, timeout);
                }

                @Override
                public boolean isRunning() {
                    return ServerHelper.isStandaloneRunning(client);
                }

                @Override
                public String toString() {
                    return "Standalone : " + getRunningVersion();
                }
            };
        } else {
            throw new IllegalArgumentException(String.format("The commandBuilder %s is not a server CommandBuilder", commandBuilder.getClass().getName()));
        }
        return server;
    }

    /**
     * Starts the server.
     *
     * @throws IOException the an error occurs creating the process
     */
    final synchronized void start(final long timeout) throws IOException, InterruptedException {
        final ServerProcess process = ServerProcess.start(commandBuilder, Collections.emptyMap(), stdout);
        try {
            waitForStart(process, timeout);
        } catch (Exception e) {
            try {
                process.destroy();
                process.waitFor(5L, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            throw new RuntimeException(e);
        }
        this.process = process;
    }

    /**
     * Stops the server.
     */
    final synchronized void stop() {
        try {
            // Stop the servers
            stopServer();
        } catch (IOException e) {
            LOGGER.error("Error attempting to stop the server.", e);
        } finally {
            try {
                ProcessHelper.destroyProcess(process);
            } catch (InterruptedException ignore) {
                // no-op
            }
            containerDescription = null;
        }
    }

    /**
     * Kills the process forcibly.
     */
    void kill() {
        final Process process = this.process;
        if (process != null) {
            process.destroyForcibly();
        }
    }

    /**
     * Stops the server before the process is destroyed. A no-op override will just destroy the process.
     */
    abstract void stopServer() throws IOException;

    abstract void waitForStart(Process process, long timeout) throws IOException, InterruptedException, TimeoutException;

    /**
     * Checks the status of the server and returns {@code true} if the server is fully started.
     *
     * @return {@code true} if the server is fully started, otherwise {@code false}
     */
    public abstract boolean isRunning();

    /**
     * Queries the running server and returns a string identifying information about the container.
     *
     * @return information about the container or {@code null} if the server is not running
     */
    ContainerDescription getRunningVersion() {
        // Lazy load the version
        if (isRunning() && containerDescription == null) {
            synchronized (this) {
                if (containerDescription == null) {
                    try {
                        containerDescription = org.wildfly.plugin.core.ServerHelper.getContainerDescription(client);
                    } catch (IOException e) {
                        LOGGER.error("Error determining running version.", e);
                    }
                }
            }
        }
        return containerDescription;
    }

    static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Exception ignore) {
        }
    }
}
