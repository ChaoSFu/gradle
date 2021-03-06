/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugin.management.internal;

import org.gradle.api.Action;
import org.gradle.plugin.management.PluginResolutionStrategy;
import org.gradle.plugin.repository.PluginRepositoriesSpec;

public class DefaultPluginManagementSpec implements PluginManagementSpecInternal {

    private final PluginRepositoriesSpec repositoriesSpec;
    private final PluginResolutionStrategyInternal pluginResolutionStrategy;

    public DefaultPluginManagementSpec(PluginRepositoriesSpec repositoriesSpec, PluginResolutionStrategyInternal pluginResolutionStrategy) {
        this.repositoriesSpec = repositoriesSpec;
        this.pluginResolutionStrategy = pluginResolutionStrategy;
    }

    @Override
    public void repositories(Action<? super PluginRepositoriesSpec> repositoriesAction) {
        repositoriesAction.execute(repositoriesSpec);
    }

    @Override
    public PluginRepositoriesSpec getRepositories() {
        return repositoriesSpec;
    }

    @Override
    public void resolutionStrategy(Action<? super PluginResolutionStrategy> action) {
        action.execute(pluginResolutionStrategy);
    }

    @Override
    public PluginResolutionStrategyInternal getResolutionStrategy() {
        return pluginResolutionStrategy;
    }

}
