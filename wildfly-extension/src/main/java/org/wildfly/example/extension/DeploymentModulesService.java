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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DeploymentModulesService implements Service<Set<DeploymentDependency>> {
    public static final ServiceName NAME = ServiceName.of("wildfly", "example", "deployment", "modules");

    private final Map<ModuleIdentifier, DeploymentDependency> modules = Collections.synchronizedMap(new LinkedHashMap<ModuleIdentifier, DeploymentDependency>());

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
        modules.clear();
    }

    @Override
    public Set<DeploymentDependency> getValue() throws IllegalStateException, IllegalArgumentException {
        synchronized (modules) {
            return new LinkedHashSet<>(modules.values());
        }
    }

    protected DeploymentDependency addInclude(final ModuleIdentifier moduleIdentifier, final boolean importServices) {
        return modules.put(moduleIdentifier, new DeploymentDependency(moduleIdentifier, true, importServices));
    }

    protected DeploymentDependency removeInclude(final ModuleIdentifier moduleIdentifier) {
        return modules.remove(moduleIdentifier);
    }

    protected DeploymentDependency addExclude(final ModuleIdentifier moduleIdentifier) {
        return modules.put(moduleIdentifier, new DeploymentDependency(moduleIdentifier, false, false));
    }

    protected DeploymentDependency removeExclude(final ModuleIdentifier moduleIdentifier) {
        return modules.remove(moduleIdentifier);
    }
}
