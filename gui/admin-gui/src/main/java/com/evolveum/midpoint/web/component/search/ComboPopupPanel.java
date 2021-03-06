/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.util.DisplayableValue;

import java.io.Serializable;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class ComboPopupPanel<T extends Serializable> extends SearchPopupPanel<T> {

    private static final long serialVersionUID = 1L;

	private static final String ID_COMBO_INPUT = "comboInput";

    private IModel<List<DisplayableValue<T>>> choices;

    public ComboPopupPanel(String id, IModel<DisplayableValue<T>> model, IModel<List<DisplayableValue<T>>> choices) {
        super(id, model);
        this.choices = choices;

        initLayout();
    }

    private void initLayout() {
        IModel<T> data = new PropertyModel<>(getModel(), SearchValue.F_VALUE);

        final DisplayableRenderer<T> renderer = new DisplayableRenderer<>(choices);
        final DropDownChoice<T> input = new DropDownChoice(ID_COMBO_INPUT, data, choices, renderer) {

        	private static final long serialVersionUID = 1L;

			@Override
            public IConverter getConverter(Class type) {
                return renderer;
            }
        };
        input.setNullValid(true);
        input.setOutputMarkupId(true);
        add(input);
    }
}
