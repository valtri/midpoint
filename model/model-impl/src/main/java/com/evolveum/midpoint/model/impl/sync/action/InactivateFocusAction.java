/**
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.impl.sync.action;

import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public class InactivateFocusAction extends BaseAction {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.sync.Action#handle(com.evolveum.midpoint.model.lens.LensContext, com.evolveum.midpoint.model.sync.SynchronizationSituation, java.util.Map, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <F extends FocusType> void handle(LensContext<F> context, SynchronizationSituation<F> situation,
			Map<QName, Object> parameters, Task task, OperationResult parentResult) {
		ActivationStatusType desiredStatus = ActivationStatusType.DISABLED;

		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null) {
			PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
			ItemPath pathAdminStatus = SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;
			if (objectCurrent != null) {
				PrismProperty<Object> administrativeStatusProp = objectCurrent.findProperty(pathAdminStatus);
				if (administrativeStatusProp != null) {
					if (desiredStatus.equals(administrativeStatusProp.getRealValue())) {
						// Desired status already set, nothing to do
						return;
					}
				}
			}
			ObjectDelta<F> activationDelta = getPrismContext().deltaFactory().object().createModificationReplaceProperty(focusContext.getObjectTypeClass(),
					focusContext.getOid(), pathAdminStatus, desiredStatus);
			focusContext.setPrimaryDelta(activationDelta);
		}

	}

}
