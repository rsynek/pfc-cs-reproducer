/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.pfc.solver;

import org.example.pfc.domain.TaskAssigningSolution;
import org.example.pfc.domain.TaskAssignment;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.api.solver.ProblemFactChange;

/**
 * Adds a TaskAssignment to the working solution. If a TaskAssignment with the given identifier already exists an
 * exception is thrown.
 */
public class AddTaskProblemFactChange implements ProblemFactChange<TaskAssigningSolution> {

    private int id;

    private TaskAssignment taskAssignment;

    public AddTaskProblemFactChange(TaskAssignment taskAssignment) {
        this.taskAssignment = taskAssignment;
    }

    public TaskAssignment getTaskAssignment() {
        return taskAssignment;
    }

    @Override
    public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
        TaskAssignment workingTaskAssignment = scoreDirector.lookUpWorkingObjectOrReturnNull(taskAssignment);
        if (workingTaskAssignment != null) {
            throw new IllegalStateException(String.format("A task assignment with the given identifier id: %s already exists", taskAssignment.getId()));
        }
        scoreDirector.beforeEntityAdded(taskAssignment);
        // Planning entity lists are already cloned by the SolutionCloner, no need to clone.
        solution.getTaskAssignmentList().add(taskAssignment);
        scoreDirector.afterEntityAdded(taskAssignment);
        scoreDirector.triggerVariableListeners();
    }

    public int getId() {
        return id;
    }
}
