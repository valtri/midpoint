/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;


/**
 * Created by Viliam Repan (lazyman).
 */
public class Search<T> extends Component<T> {

    public Search(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public Popover<Search<T>> byName() {

        SelenideElement linksContainer = getParentElement().$(Schrodinger.byDataId("div", "linksContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        try {
            linksContainer.$(Schrodinger.byDataId("a", "basic")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        } catch (Throwable t) {
            // all is ok, basic search is already selected option, TODO: Schrodinger should provide easy method to check component existence
        }

        getParentElement().$(Schrodinger.byDataId("a", "mainButton")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        ElementsCollection popoverElements = getParentElement().$$(Schrodinger.byDataId("popover"));

        SelenideElement popover = null;
        for (SelenideElement popoverElement : popoverElements) {
            if (popoverElement.isDisplayed()) {
                popover = popoverElement;
                break;
            }
            popover = popoverElement;
        }
        return new Popover<>(this, popover);
    }

    public InputBox<Search<T>> byFullText() {

        SelenideElement linksContainer = getParentElement().$(Schrodinger.byDataId("div", "linksContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        try {
            linksContainer.$(Schrodinger.byDataId("a", "fullText")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        } catch (Throwable t) {
            // all is ok, fullText search is already selected option, TODO: Schrodinger should provide easy method to check component existence
        }

        // we assume fulltext is enabled in systemconfig, else error is thrown here:
        SelenideElement fullTextField = getParentElement().$(Schrodinger.byDataId("input", "fullTextField")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new InputBox<> (this, fullTextField);
    }
}

