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

import org.example.pfc.domain.ModelConstants;
import org.example.pfc.domain.TaskAssignment;
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
                planningUserAssignment(constraintFactory)
        };
    }

    protected Constraint planningUserAssignment(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> ModelConstants.IS_PLANNING_USER.test(taskAssignment.getUser().getId()))
                .penalize("PlanningUser assignment", softLevelWeight(0, 1));
    }
}