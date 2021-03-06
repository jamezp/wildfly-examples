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

package org.wildfly.example.extension;

import org.jboss.modules.ModuleIdentifier;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DeploymentDependency {

    private final ModuleIdentifier identifier;
    private final boolean include;
    private final boolean importServices;
    private final boolean optional;

    protected DeploymentDependency(final ModuleIdentifier identifier, final boolean include,
                                   final boolean importServices, final boolean optional) {
        this.identifier = identifier;
        this.include = include;
        this.importServices = importServices;
        this.optional = optional;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public boolean isInclude() {
        return include;
    }

    public boolean isImport() {
        return importServices;
    }

    public boolean isOptional() {
        return optional;
    }
}
