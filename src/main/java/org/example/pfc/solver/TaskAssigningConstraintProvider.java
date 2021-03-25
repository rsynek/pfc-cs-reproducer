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

import static org.example.pfc.domain.TaskAssigningSolution.HARD_LEVELS_SIZE;
import static org.example.pfc.domain.TaskAssigningSolution.SOFT_LEVELS_SIZE;

import org.example.pfc.domain.DefaultLabels;
import org.example.pfc.domain.ModelConstants;
import org.example.pfc.domain.PriorityHelper;
import org.example.pfc.domain.TaskAssigningConditions;
import org.example.pfc.domain.TaskAssignment;
import org.example.pfc.domain.TaskHelper;
import org.optaplanner.core.api.score.buildin.bendablelong.BendableLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

public class TaskAssigningConstraintProvider implements ConstraintProvider {

    public static BendableLongScore hardLevelWeight(int hardLevel, long hardScore) {
        return BendableLongScore.ofHard(HARD_LEVELS_SIZE, SOFT_LEVELS_SIZE, hardLevel, hardScore);
    }

    public static BendableLongScore softLevelWeight(int softLevel, long softScore) {
        return BendableLongScore.ofSoft(HARD_LEVELS_SIZE, SOFT_LEVELS_SIZE, softLevel, softScore);
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                requiredPotentialOwner(constraintFactory),
                requiredSkills(constraintFactory),
                planningUserAssignment(constraintFactory),
                highLevelPriority(constraintFactory),
                desiredAffinities(constraintFactory),
                minimizeMakespan(constraintFactory),
                mediumLevelPriority(constraintFactory),
                lowLevelPriority(constraintFactory)
        };
    }

    protected Constraint requiredPotentialOwner(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> !TaskAssigningConditions.userMeetsPotentialOwnerOrPlanningUserCondition(taskAssignment.getTask(), taskAssignment.getUser()))
                .penalize("Required Potential Owner", hardLevelWeight(0, 1));
    }

    protected Constraint requiredSkills(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> !TaskAssigningConditions.userMeetsRequiredSkillsOrPlanningUserCondition(taskAssignment.getTask(), taskAssignment.getUser()))
                .penalize("Required Skills", hardLevelWeight(1, 1));
    }

    protected Constraint planningUserAssignment(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> ModelConstants.IS_PLANNING_USER.test(taskAssignment.getUser().getId()))
                .penalize("PlanningUser assignment", softLevelWeight(0, 1));
    }

    protected Constraint highLevelPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> PriorityHelper.isHighLevel(taskAssignment.getTask().getPriority()))
                .penalize("High level priority",
                        softLevelWeight(1, 1),
                        TaskAssignment::getEndTimeInMinutes);
    }

    protected Constraint desiredAffinities(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> taskAssignment.getUser().isEnabled())
                .reward("Desired Affinities",
                        softLevelWeight(2, 1),
                        taskAssignment -> TaskHelper.countMatchingLabels(taskAssignment.getTask(), taskAssignment.getUser(), DefaultLabels.AFFINITIES.name()));
    }

    protected Constraint minimizeMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> taskAssignment.getNextElement() == null)
                .penalize("Minimize makespan",
                        softLevelWeight(3, 1),
                        taskAssignment -> taskAssignment.getEndTimeInMinutes() * taskAssignment.getEndTimeInMinutes());
    }

    protected Constraint mediumLevelPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> PriorityHelper.isMediumLevel(taskAssignment.getTask().getPriority()))
                .penalize("Medium level priority",
                        softLevelWeight(4, 1),
                        TaskAssignment::getEndTimeInMinutes);
    }

    protected Constraint lowLevelPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> PriorityHelper.isLowLevel(taskAssignment.getTask().getPriority()))
                .penalize("Low level priority",
                        softLevelWeight(5, 1),
                        TaskAssignment::getEndTimeInMinutes);
    }
}