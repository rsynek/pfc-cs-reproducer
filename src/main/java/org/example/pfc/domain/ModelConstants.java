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
package org.example.pfc.domain;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.function.Predicate;

public class ModelConstants {

    /**
     * System property for configuring the PLANNING_USER entityId.
     */
    public static final String PLANNING_USER_ID_PROPERTY = "planningUserId";
    public static final String PLANNING_USER_ID = System.getProperty(PLANNING_USER_ID_PROPERTY, "planninguser");
    /**
     * Planning user is defined user for avoid breaking hard constraints. When no user is found that met the task required
     * potential owners set, or the required skills set, etc, the PLANNING_USER is assigned.
     */
    public static final User PLANNING_USER = new User(PLANNING_USER_ID, true, Collections.emptyMap());
    public static final Predicate<String> IS_PLANNING_USER = entityId -> PLANNING_USER.getId().equals(entityId);

    private static final ZonedDateTime DUMMY_DATE = ZonedDateTime.parse("2021-01-01T01:01:01.001Z",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    /**
     * This task was introduced for dealing with situations where the solution ends up with no tasks. e.g. there is a
     * solution with tasks A and B, and a user completes both tasks in the kogito runtime. When the completion events
     * are processed both tasks are removed from the solution with the proper problem fact changes. The solution remains
     * thus with no tasks and an exception is thrown.
     * Since the only potential owner for the dummy task is the PLANNING_USER this task won't affect the score dramatically.
     */
    public static final TaskAssignment DUMMY_TASK_ASSIGNMENT = new TaskAssignment(Task.newBuilder()
            .id("-1")
            .name("dummy-task")
            .state("dummy-state")
            .description("dummy-description")
            .referenceName("dummy-reference-name")
            .priority("1")
            .processInstanceId("dummy-process-instance-id")
            .processId("dummy-process-id")
            .rootProcessInstanceId("dummy-root-process-instance-id")
            .rootProcessId("dummy-root-process-id")
            .started(DUMMY_DATE)
            .completed(DUMMY_DATE)
            .lastUpdate(DUMMY_DATE)
            .endpoint("dummy-endpoint")
            .build());

    /**
     * This task was introduced for avoid falling into scenarios where we have all assignments pinned and only one
     * entity for selection, which will make the solver enter in the bailing out loop. At least this second task plus
     * the DUMMY_TASK_ASSIGNMENT ensures two available elements. (see https://issues.jboss.org/browse/PLANNER-1738)
     * Will be removed when issue is fixed.
     */
    public static final TaskAssignment DUMMY_TASK_ASSIGNMENT_PLANNER_1738 = new TaskAssignment(Task.newBuilder()
            .id("-1738")
            .name("dummy-task-1738")
            .state("dummy-state-1738")
            .description("dummy-description-1738")
            .referenceName("dummy-reference-name-1738")
            .priority("1")
            .processInstanceId("dummy-process-instance-id-241")
            .processId("dummy-process-id-241")
            .rootProcessInstanceId("dummy-root-process-instance-id-241")
            .rootProcessId("dummy-root-process-id-241")
            .started(DUMMY_DATE)
            .completed(DUMMY_DATE)
            .lastUpdate(DUMMY_DATE)
            .endpoint("dummy-endpoint-241")
            .build());


    public static final Predicate<TaskAssignment> IS_NOT_DUMMY_TASK_ASSIGNMENT =
            taskAssignment -> !DUMMY_TASK_ASSIGNMENT.getId().equals(taskAssignment.getId()) &&
                    !DUMMY_TASK_ASSIGNMENT_PLANNER_1738.getId().equals(taskAssignment.getId());

    private ModelConstants() {
    }
}
