/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.AbstractSummaryPanel;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.SummaryTagSimple;
import com.evolveum.midpoint.web.component.wf.WfGuiUtil;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.wicket.model.IModel;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 *
 */
public class WorkItemSummaryPanel extends AbstractSummaryPanel<WorkItemType> {
	private static final long serialVersionUID = -5077637168906420769L;

	private static final String ID_ASSIGNED_TAG = "assignedTag";

	private final IModel<WorkItemDto> dtoModel;

	public WorkItemSummaryPanel(String id, IModel<WorkItemType> model, IModel<WorkItemDto> dtoModel, ModelServiceLocator serviceLocator) {
		super(id, model, serviceLocator, null);
		this.dtoModel = dtoModel;

		initLayoutCommon(serviceLocator);

		SummaryTagSimple<WorkItemType> isAssignedTag = new SummaryTagSimple<WorkItemType>(ID_ASSIGNED_TAG, model) {
			@Override
			protected void initialize(WorkItemType workItem) {
				if (workItem.getAssigneeRef() != null) {
					setIconCssClass("fa fa-fw fa-lock");
					setLabel(getString("WorkItemSummaryPanel.allocated"));
				} else {
					setIconCssClass("fa fa-fw fa-unlock");
					setLabel(getString("WorkItemSummaryPanel.notAllocated"));
				}
			}
		};
		addTag(isAssignedTag);
	}

	@Override
	protected IModel<String> getDisplayNameModel() {
		return new ReadOnlyModel<>(() -> {
			WorkItemDto workItemDto = dtoModel.getObject();
			return defaultIfNull(
					WfGuiUtil.getLocalizedProcessName(workItemDto.getWorkflowContext(), WorkItemSummaryPanel.this),
					workItemDto.getName());
		});
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {		// TODO
		return "summary-panel-task"; // TODO
	}

	@Override
	protected String getBoxAdditionalCssClass() {			// TODO
		return "summary-panel-task"; // TODO
	}

	@Override
	protected boolean isIdentifierVisible() {
		return false;
	}

	@Override
	protected String getTagBoxCssClass() {
		return "summary-tag-box";
	}

	@Override
	protected IModel<String> getTitleModel() {
		return new IModel<String>() {
			@Override
			public String getObject() {
				UserType requester = dtoModel.getObject().getRequester();
				if (requester == null) {
					// MID-4539 if we don't have authorization to see requester
					return getString("TaskSummaryPanel.requestedBy", getString("TaskSummaryPanel.unknown"));
				}

				String displayName = WebComponentUtil.getDisplayName(requester.asPrismObject());
				String name = WebComponentUtil.getName(requester.asPrismObject());
				if (displayName != null) {
					return getString("TaskSummaryPanel.requestedByWithFullName", displayName, name);
				} else {
					return getString("TaskSummaryPanel.requestedBy", name);
				}
			}
		};
	}

	@Override
	protected IModel<String> getTitle2Model() {
		return new IModel<String>() {
			@Override
			public String getObject() {
				WorkItemType workItem = getModelObject();
				return getString("TaskSummaryPanel.requestedOn",
						WebComponentUtil.getLocalizedDate(WfContextUtil.getWorkflowContext(workItem).getStartTimestamp(), DateLabelComponent.MEDIUM_MEDIUM_STYLE));
			}
		};
	}

//	@Override
//	protected IModel<String> getTitle3Model() {
//		return new IModel<String>() {
//			@Override
//			public String getObject() {
//				WorkItemType workItem = getModelObject();
//				return getString("WorkItemSummaryPanel.createdOn",
//						workItem.getWorkItemCreatedTimestamp());		// todo formatting
//			}
//		};
//	}
}
