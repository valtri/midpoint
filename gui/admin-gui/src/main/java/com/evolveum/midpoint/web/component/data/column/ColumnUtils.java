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
package com.evolveum.midpoint.web.component.data.column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ArchetypeInteractionSpecification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;

public class ColumnUtils {

	public static <T> List<IColumn<T, String>> createColumns(List<ColumnTypeDto<String>> columns) {
		List<IColumn<T, String>> tableColumns = new ArrayList<>();
		for (ColumnTypeDto<String> column : columns) {
			PropertyColumn<T, String> tableColumn = null;
			if (column.isSortable()) {
				tableColumn = createPropertyColumn(column.getColumnName(), column.getSortableColumn(),
						column.getColumnValue(), column.isMultivalue());

			} else {
				tableColumn = new PropertyColumn<>(createStringResource(column.getColumnName()),
                    column.getColumnValue());
			}
			tableColumns.add(tableColumn);

		}
		return tableColumns;
	}

	private static <T> PropertyColumn<T, String> createPropertyColumn(String name, String sortableProperty,
			final String expression, final boolean multivalue) {

		return new PropertyColumn<T, String>(createStringResource(name), sortableProperty, expression) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item item, String componentId, IModel rowModel) {
				if (multivalue) {
					IModel<List> values = new PropertyModel<>(rowModel, expression);
					RepeatingView repeater = new RepeatingView(componentId);
					for (final Object task : values.getObject()) {
						repeater.add(new Label(repeater.newChildId(), task.toString()));
					}
					item.add(repeater);
					return;
				}

				super.populateItem(item, componentId, rowModel);
			}
		};

	}

	public static <O extends ObjectType> List<IColumn<SelectableBean<O>, String>> getDefaultColumns(Class<? extends O> type) {
		if (type == null) {
			return getDefaultUserColumns();
		}

		if (type.equals(UserType.class)) {
			return getDefaultUserColumns();
		} else if (RoleType.class.equals(type)) {
			return getDefaultRoleColumns();
		} else if (OrgType.class.equals(type)) {
			return getDefaultOrgColumns();
		} else if (ServiceType.class.equals(type)) {
			return getDefaultServiceColumns();
		} else if (type.equals(TaskType.class)) {
			return getDefaultTaskColumns();
		} else if (type.equals(ResourceType.class)) {
			return getDefaultResourceColumns();
		} else {
			return new ArrayList<>();
//			throw new UnsupportedOperationException("Will be implemented eventually");
		}
	}

	public static <O extends ObjectType> IColumn<SelectableBean<O>, String> createIconColumn(Class<? extends O> type, PageBase pageBase){

		return new IconColumn<SelectableBean<O>>(createIconColumnHeaderModel()) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<O>>> cellItem, String componentId, IModel<SelectableBean<O>> rowModel) {
				DisplayType displayType = getDisplayTypeForRowObject(rowModel, pageBase);
				if (displayType != null){
					cellItem.add(new ImagePanel(componentId, displayType));
				} else {
					super.populateItem(cellItem, componentId, rowModel);
				}
			}

			@Override
			protected IModel<String> createIconModel(final IModel<SelectableBean<O>> rowModel) {
				return Model.of(getIconColumnValue(type, rowModel));
			}

			@Override
			protected IModel<String> createTitleModel(final IModel<SelectableBean<O>> rowModel) {
				return Model.of(getIconColumnTitle(type, rowModel));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<O>> rowModel) {
				return getIconColumnDataModel(type, rowModel);
			}
		};

	}

	private static <O extends ObjectType> DisplayType getDisplayTypeForRowObject(IModel<SelectableBean<O>> rowModel, PageBase pageBase){
		O object = rowModel.getObject().getValue();
		if (object != null) {
			ArchetypeInteractionSpecification archetypeSpec = WebComponentUtil.getArchetypeSpecification(object.asPrismObject(), pageBase);
			if (archetypeSpec != null && archetypeSpec.getArchetypePolicy() != null) {
				return archetypeSpec.getArchetypePolicy().getDisplay();
			}
		}
		return null;
	}

	private static <T extends ObjectType> String getIconColumnValue(Class<? extends T> type, IModel<SelectableBean<T>> rowModel){
		T object = rowModel.getObject().getValue();
		if (object == null && !ShadowType.class.equals(type)){
			return null;
		} else if (type.equals(ObjectType.class)){
			return WebComponentUtil.createDefaultIcon(object.asPrismObject());
		} else if (type.equals(UserType.class)) {
			return WebComponentUtil.createUserIcon(object.asPrismContainer());
		} else if (RoleType.class.equals(type)) {
			return WebComponentUtil.createRoleIcon(object.asPrismContainer());
		} else if (OrgType.class.equals(type)) {
			return WebComponentUtil.createOrgIcon(object.asPrismContainer());
		} else if (ServiceType.class.equals(type)) {
			return WebComponentUtil.createServiceIcon(object.asPrismContainer()) ;
		} else if (ShadowType.class.equals(type)) {
			if (object == null) {
				return WebComponentUtil.createErrorIcon(rowModel.getObject().getResult());
			} else {
				return WebComponentUtil.createShadowIcon(object.asPrismContainer());
			}
		} else if (type.equals(TaskType.class)) {
			return WebComponentUtil.createTaskIcon(object.asPrismContainer());
		} else if (type.equals(ResourceType.class)) {
			return WebComponentUtil.createResourceIcon(object.asPrismContainer());
		} else if (type.equals(AccessCertificationDefinitionType.class)) {
			return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
		} else {
			return "";
//			throw new UnsupportedOperationException("Will be implemented eventually");
		}

	}

	private static <T extends ObjectType> IModel<String> getIconColumnDataModel(Class<? extends T> type, IModel<SelectableBean<T>> rowModel){
		if (ShadowType.class.equals(type)) {
				T shadow = rowModel.getObject().getValue();
				if (shadow == null){
					return null;
				}
				return ShadowUtil.isProtected(shadow.asPrismContainer()) ?
						createStringResource("ThreeStateBooleanPanel.true") : createStringResource("ThreeStateBooleanPanel.false");

		}
		return null;
	}

	private static <T extends ObjectType> String getIconColumnTitle(Class<? extends T> type, IModel<SelectableBean<T>> rowModel){
		T object = rowModel.getObject().getValue();
		if (object == null && !ShadowType.class.equals(type)){
			return null;
		} else if (type.equals(UserType.class)) {
			String iconClass = object != null ? WebComponentUtil.createUserIcon(object.asPrismContainer()) : null;
			String compareStringValue = GuiStyleConstants.CLASS_OBJECT_USER_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE;
			String titleValue = "";
			if (iconClass != null &&
					iconClass.startsWith(compareStringValue) &&
					iconClass.length() > compareStringValue.length()){
				titleValue = iconClass.substring(compareStringValue.length());
			}
			return createStringResource("ColumnUtils.getUserIconColumn.createTitleModel." + titleValue) == null ?
					"" : createStringResource("ColumnUtils.getUserIconColumn.createTitleModel." + titleValue).getString();
		} else {
			return object.asPrismContainer().getDefinition().getTypeName().getLocalPart();
		}
	}

	private static IModel<String> createIconColumnHeaderModel() {
		return new Model<String>() {
			@Override
			public String getObject() {
				return "";
			}
		};
	}

	public static StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
				.setParameters(objects);
	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultUserColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("UserType.givenName", UserType.F_GIVEN_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".givenName.orig", false),
				new ColumnTypeDto<String>("UserType.familyName", UserType.F_FAMILY_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".familyName.orig", false),
				new ColumnTypeDto<String>("UserType.fullName", UserType.F_FULL_NAME.getLocalPart(),
						SelectableBean.F_VALUE + ".fullName.orig", false),
				new ColumnTypeDto<String>("UserType.emailAddress", UserType.F_EMAIL_ADDRESS.getLocalPart(),
						SelectableBean.F_VALUE + ".emailAddress", false)

		);
		columns.addAll(ColumnUtils.<SelectableBean<T>>createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultTaskColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

		columns.add(
				new AbstractColumn<SelectableBean<T>, String>(createStringResource("TaskType.kind")) {
					private static final long serialVersionUID = 1L;

					@Override
					public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
							String componentId, IModel<SelectableBean<T>> rowModel) {
						SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
						PrismProperty<ShadowKindType> pKind = object.getValue() != null ?
								object.getValue().asPrismObject().findProperty(
										ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
								: null;
						if (pKind != null) {
							cellItem.add(new Label(componentId, WebComponentUtil
									.createLocalizedModelForEnum(pKind.getRealValue(), cellItem)));
						} else {
							cellItem.add(new Label(componentId));
						}

					}

				});

		columns.add(new AbstractColumn<SelectableBean<T>, String>(
				createStringResource("TaskType.intent")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
					String componentId, IModel<SelectableBean<T>> rowModel) {
				SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
				PrismProperty<String> pIntent = object.getValue() != null ?
						object.getValue().asPrismObject().findProperty(
								ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT))
						: null;
				if (pIntent != null) {
					cellItem.add(new Label(componentId, pIntent.getRealValue()));
				} else {
					cellItem.add(new Label(componentId));
				}
			}

		});

		columns.add(new AbstractColumn<SelectableBean<T>, String>(
				createStringResource("TaskType.objectClass")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
					String componentId, IModel<SelectableBean<T>> rowModel) {
				SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();
				PrismProperty<QName> pObjectClass = object.getValue() != null ?
						object.getValue().asPrismObject().findProperty(
								ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
						: null;
				if (pObjectClass != null) {
					cellItem.add(new Label(componentId, pObjectClass.getRealValue().getLocalPart()));
				} else {
					cellItem.add(new Label(componentId, ""));
				}

			}

		});

		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("TaskType.executionStatus", TaskType.F_EXECUTION_STATUS.getLocalPart(),
						SelectableBean.F_VALUE + ".executionStatus", false));
		columns.addAll(ColumnUtils.<SelectableBean<T>>createColumns(columnsDefs));

		return columns;

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultRoleColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();


		columns.addAll((Collection)getDefaultAbstractRoleColumns(RoleType.COMPLEX_TYPE));

		return columns;
	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultServiceColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

		columns.addAll((Collection)getDefaultAbstractRoleColumns(ServiceType.COMPLEX_TYPE));

		return columns;
	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultOrgColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

		columns.addAll((Collection)getDefaultAbstractRoleColumns(OrgType.COMPLEX_TYPE));

		return columns;
	}

	private static <T extends AbstractRoleType> List<IColumn<SelectableBean<T>, String>> getDefaultAbstractRoleColumns(QName type) {

		String sortByDisplayName = null;
		String sortByIdentifer = null;
		sortByDisplayName = AbstractRoleType.F_DISPLAY_NAME.getLocalPart();
		sortByIdentifer = AbstractRoleType.F_IDENTIFIER.getLocalPart();
		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("AbstractRoleType.displayName",
						sortByDisplayName,
						SelectableBean.F_VALUE + ".displayName", false),
				new ColumnTypeDto<String>("AbstractRoleType.description",
						null,
						SelectableBean.F_VALUE + ".description", false),
				new ColumnTypeDto<String>("AbstractRoleType.identifier", sortByIdentifer,
						SelectableBean.F_VALUE + ".identifier", false)

		);
		List<IColumn<SelectableBean<T>, String>> columns = createColumns(columnsDefs);
		
		IColumn<SelectableBean<T>, String> column = new AbstractExportableColumn<SelectableBean<T>, String>(
				createStringResource("pageUsers.accounts")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
					String componentId, IModel<SelectableBean<T>> model) {
				cellItem.add(new Label(componentId,
						model.getObject().getValue() != null ?
								model.getObject().getValue().getLinkRef().size() : null));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<T>> rowModel) {
				return Model.of(rowModel.getObject().getValue() != null ?
						Integer.toString(rowModel.getObject().getValue().getLinkRef().size()) : "");
			}


		};

		columns.add(column);
		return columns;

	}

	public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultResourceColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

		List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
				new ColumnTypeDto<String>("AbstractRoleType.description", null,
						SelectableBean.F_VALUE + ".description", false)

		);

		columns.addAll(ColumnUtils.<SelectableBean<T>>createColumns(columnsDefs));

		return columns;

	}

}
