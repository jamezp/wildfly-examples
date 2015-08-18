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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.example.extension.deployment.DeploymentModuleProcessor;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DeploymentModuleResource extends PersistentResourceDefinition {

    static final DeploymentModuleResource INSTANCE = new DeploymentModuleResource();

    static final PersistentResourceXMLBuilder XML_BUILDER = PersistentResourceXMLDescription.builder(INSTANCE, Namespace.CURRENT.getUriString())
            .addChild(IncludeResource.XML_BUILDER)
            .addChild(ExcludeResource.XML_BUILDER);

    private final List<? extends PersistentResourceDefinition> children = Arrays.asList(IncludeResource.INSTANCE, ExcludeResource.INSTANCE);

    private DeploymentModuleResource() {
        super(
                Resources.SUBSYSTEM_PATH,
                Resources.getResourceDescriptionResolver(),
                new DeploymentModuleAddHandler(),
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return children;
    }

    private static class DeploymentModuleAddHandler extends AbstractAddStepHandler {
        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // Add the ModuleDependenciesService to the service container
            final ServiceTarget target = context.getServiceTarget();
            target.addService(DeploymentModulesService.NAME, new DeploymentModulesService())
                    .install();

            // Register the deployment unit processor
            context.addStep(new AbstractDeploymentChainStep() {
                @Override
                protected void execute(final DeploymentProcessorTarget processorTarget) {
                    // The current last dependency phase is 0x2300, using 0x5000 should give us plenty space
                    processorTarget.addDeploymentProcessor(Resources.SUBSYSTEM_NAME, Phase.DEPENDENCIES, 0x5000, new DeploymentModuleProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
