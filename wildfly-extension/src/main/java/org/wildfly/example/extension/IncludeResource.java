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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class IncludeResource extends PersistentResourceDefinition {

    private static final String NAME = "include";

    private static final AttributeDefinition IMPORT_SERVICES = SimpleAttributeDefinitionBuilder.create("import-services", ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    private static final AttributeDefinition OPTIONAL = SimpleAttributeDefinitionBuilder.create("optional", ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    private static final List<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(IMPORT_SERVICES, OPTIONAL));

    static final IncludeResource INSTANCE = new IncludeResource();

    static final PersistentResourceXMLDescription.PersistentResourceXMLBuilder XML_BUILDER = PersistentResourceXMLDescription.builder(INSTANCE)
            .addAttribute(IMPORT_SERVICES)
            .addAttribute(OPTIONAL);

    private IncludeResource() {
        super(
                PathElement.pathElement(NAME),
                Resources.getResourceDescriptionResolver(NAME),
                new IncludeAddHandler(),
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    private static class IncludeAddHandler extends AbstractAddStepHandler {
        public IncludeAddHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            final ServiceRegistry registry = context.getServiceRegistry(true);
            final DeploymentModulesService service = (DeploymentModulesService) registry.getRequiredService(DeploymentModulesService.NAME).getService();

            final ModuleIdentifier identifier = ModuleIdentifier.create(context.getCurrentAddressValue());
            final boolean importServices = IMPORT_SERVICES.resolveModelAttribute(context, model).asBoolean();
            final boolean optional = OPTIONAL.resolveModelAttribute(context, model).asBoolean();
            service.addInclude(identifier, importServices, optional);
            context.reloadRequired();
        }

        @Override
        protected void rollbackRuntime(final OperationContext context, final ModelNode operation, final Resource resource) {
            final ServiceRegistry registry = context.getServiceRegistry(true);
            final DeploymentModulesService service = (DeploymentModulesService) registry.getRequiredService(DeploymentModulesService.NAME).getService();

            final ModuleIdentifier identifier = ModuleIdentifier.create(context.getCurrentAddressValue());
            service.removeInclude(identifier);
        }
    }
}
