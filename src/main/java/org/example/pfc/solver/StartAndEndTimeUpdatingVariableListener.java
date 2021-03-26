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


import static org.example.pfc.domain.TaskAssignment.END_TIME_IN_MINUTES;
import static org.example.pfc.domain.TaskAssignment.START_TIME_IN_MINUTES;

import java.util.Objects;

import org.example.pfc.domain.ChainElement;
import org.example.pfc.domain.TaskAssigningSolution;
import org.example.pfc.domain.TaskAssignment;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

public class StartAndEndTimeUpdatingVariableListener implements VariableListener<TaskAssigningSolution, TaskAssignment> {

    @Override
    public void beforeEntityAdded(final ScoreDirector<TaskAssigningSolution> scoreDirector, final TaskAssignment taskAssignment) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(final ScoreDirector<TaskAssigningSolution> scoreDirector, final TaskAssignment taskAssignment) {
        updateStartAndEndTime(scoreDirector, taskAssignment);
    }

    @Override
    public void beforeVariableChanged(final ScoreDirector<TaskAssigningSolution> scoreDirector, final TaskAssignment taskAssignment) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(final ScoreDirector<TaskAssigningSolution> scoreDirector, final TaskAssignment taskAssignment) {
        updateStartAndEndTime(scoreDirector, taskAssignment);
    }

    @Override
    public void beforeEntityRemoved(final ScoreDirector<TaskAssigningSolution> scoreDirector, final TaskAssignment taskAssignment) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(final ScoreDirector<TaskAssigningSolution> scoreDirector, TaskAssignment taskAssignment) {
        // Do nothing
    }

    private static void updateStartAndEndTime(final ScoreDirector<TaskAssigningSolution> scoreDirector, final TaskAssignment sourceTaskAssignment) {
        ChainElement previous = sourceTaskAssignment.getPreviousElement();
        TaskAssignment shadowTaskAssignment = sourceTaskAssignment;
        Integer previousEndTime = previous == null || !previous.isTaskAssignment() ? 0 : ((TaskAssignment) previous).getEndTimeInMinutes();
        Integer startTime = previousEndTime;
        Integer endTime = calculateEndTime(shadowTaskAssignment, startTime);
        while (shadowTaskAssignment != null && !Objects.equals(shadowTaskAssignment.getStartTimeInMinutes(), startTime)) {
            scoreDirector.beforeVariableChanged(shadowTaskAssignment, START_TIME_IN_MINUTES);
            shadowTaskAssignment.setStartTimeInMinutes(startTime);
            scoreDirector.afterVariableChanged(shadowTaskAssignment, START_TIME_IN_MINUTES);

            scoreDirector.beforeVariableChanged(shadowTaskAssignment, END_TIME_IN_MINUTES);
            shadowTaskAssignment.setEndTimeInMinutes(endTime);
            scoreDirector.afterVariableChanged(shadowTaskAssignment, END_TIME_IN_MINUTES);

            previousEndTime = shadowTaskAssignment.getEndTimeInMinutes();
            shadowTaskAssignment = shadowTaskAssignment.getNextElement();
            startTime = previousEndTime;
            endTime = calculateEndTime(shadowTaskAssignment, startTime);
        }
    }

    private static Integer calculateEndTime(final TaskAssignment shadowTaskAssignment, final Integer startTime) {
        if (startTime == null || shadowTaskAssignment == null) {
            return 0;
        }
        return startTime + shadowTaskAssignment.getDurationInMinutes();
    }
}
