/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.changedetection.rules;

import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshotterRegistry;
import org.gradle.api.internal.changedetection.state.TaskExecution;
import org.gradle.api.internal.changedetection.state.TaskHistoryRepository;
import org.gradle.api.internal.changedetection.state.ValueSnapshotter;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.internal.classloader.ClassLoaderHierarchyHasher;
import org.gradle.normalization.internal.InputNormalizationHandlerInternal;
import org.gradle.normalization.internal.InputNormalizationStrategy;

import java.io.File;
import java.util.Set;

/**
 * Represents the complete changes in a tasks state
 */
public class TaskUpToDateState {

    public static final int MAX_OUT_OF_DATE_MESSAGES = 3;

    private final TaskStateChanges inputFileChanges;
    private final OutputFilesTaskStateChanges outputFileChanges;
    private final DiscoveredInputsListener discoveredInputsListener;
    private final TaskStateChanges allTaskChanges;
    private final TaskStateChanges rebuildChanges;

    public TaskUpToDateState(TaskInternal task, TaskHistoryRepository.History history,
                             FileCollectionSnapshotterRegistry fileCollectionSnapshotterRegistry,
                             FileCollectionFactory fileCollectionFactory, ClassLoaderHierarchyHasher classLoaderHierarchyHasher, ValueSnapshotter valueSnapshotter) {
        TaskExecution thisExecution = history.getCurrentExecution();
        TaskExecution lastExecution = history.getPreviousExecution();
        InputNormalizationStrategy inputNormalizationStrategy = ((InputNormalizationHandlerInternal) task.getProject().getNormalization()).buildFinalStrategy();

        TaskStateChanges noHistoryState = new NoHistoryTaskStateChanges(lastExecution);
        TaskStateChanges previousSuccessState = new PreviousSuccessTaskStateChanges(lastExecution, thisExecution, task);
        TaskStateChanges taskTypeState = new TaskTypeTaskStateChanges(lastExecution, thisExecution, task.getPath(), task.getClass(), task.getTaskActions(), classLoaderHierarchyHasher);
        TaskStateChanges inputPropertiesState = new InputPropertiesTaskStateChanges(lastExecution, thisExecution, task, valueSnapshotter);

        // Capture outputs state
        OutputFilesTaskStateChanges uncachedOutputChanges = new OutputFilesTaskStateChanges(lastExecution, thisExecution, task, fileCollectionSnapshotterRegistry, inputNormalizationStrategy);
        TaskStateChanges outputFileChanges = caching(uncachedOutputChanges);
        this.outputFileChanges = uncachedOutputChanges;

        // Capture inputs state
        InputFilesTaskStateChanges directInputFileChanges = new InputFilesTaskStateChanges(lastExecution, thisExecution, task, fileCollectionSnapshotterRegistry, inputNormalizationStrategy);
        TaskStateChanges inputFileChanges = caching(directInputFileChanges);
        this.inputFileChanges = new ErrorHandlingTaskStateChanges(task, inputFileChanges);

        // Capture discovered inputs state from previous execution
        DiscoveredInputsTaskStateChanges discoveredChanges = new DiscoveredInputsTaskStateChanges(lastExecution, thisExecution, fileCollectionSnapshotterRegistry, fileCollectionFactory, task, inputNormalizationStrategy);
        this.discoveredInputsListener = discoveredChanges;
        TaskStateChanges discoveredInputFilesChanges = caching(discoveredChanges);

        this.allTaskChanges = new ErrorHandlingTaskStateChanges(task, new SummaryTaskStateChanges(MAX_OUT_OF_DATE_MESSAGES, previousSuccessState, noHistoryState, taskTypeState, inputPropertiesState, outputFileChanges, inputFileChanges, discoveredInputFilesChanges));
        this.rebuildChanges = new ErrorHandlingTaskStateChanges(task, new SummaryTaskStateChanges(1, previousSuccessState, noHistoryState, taskTypeState, inputPropertiesState, outputFileChanges));
    }

    private static TaskStateChanges caching(TaskStateChanges wrapped) {
        return new CachingTaskStateChanges(MAX_OUT_OF_DATE_MESSAGES, wrapped);
    }

    /**
     * Returns changes to input files only.
     */
    public TaskStateChanges getInputFilesChanges() {
        return inputFileChanges;
    }

    /**
     * Returns if any output files have been changed, added or removed.
     */
    public boolean hasAnyOutputFileChanges() {
        return outputFileChanges.iteratorIncludingAdded().hasNext();
    }

    /**
     * Returns any change to task inputs or outputs.
     */
    public TaskStateChanges getAllTaskChanges() {
        return allTaskChanges;
    }

    /**
     * Returns changes that would force an incremental task to fully rebuild.
     */
    public TaskStateChanges getRebuildChanges() {
        return rebuildChanges;
    }

    /**
     * Record discovered inputs.
     */
    public void newInputs(Set<File> discoveredInputs) {
        discoveredInputsListener.newInputs(discoveredInputs);
    }

}
