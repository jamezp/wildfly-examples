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

package org.wildfly.example.extension.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.example.extension.DeploymentDependency;
import org.wildfly.example.extension.DeploymentModulesService;
import org.wildfly.example.extension.logging.DeploymentModuleLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DeploymentModuleProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        final DeploymentModulesService service = (DeploymentModulesService) phaseContext.getServiceRegistry().getRequiredService(DeploymentModulesService.NAME).getService();

        // Add the logging modules
        for (DeploymentDependency dep : service.getValue()) {
            if (dep.isInclude()) {
                DeploymentModuleLogger.LOGGER.includingModule(dep.getIdentifier());
                moduleSpecification.addUserDependency(new ModuleDependency(moduleLoader, dep.getIdentifier(), false, false, false, true));
            } else {
                DeploymentModuleLogger.LOGGER.excludingModule(dep.getIdentifier());
                moduleSpecification.addExclusion(dep.getIdentifier());
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
