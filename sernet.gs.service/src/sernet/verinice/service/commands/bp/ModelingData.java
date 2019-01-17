/*******************************************************************************
 * Copyright (c) 2019 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/
package sernet.verinice.service.commands.bp;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sernet.verinice.model.bp.elements.ItNetwork;
import sernet.verinice.model.bp.groups.BpRequirementGroup;
import sernet.verinice.model.bp.groups.SafeguardGroup;
import sernet.verinice.model.common.CnATreeElement;

/**
 * Keep track of information about copied items during modeling
 */
public class ModelingData {

    private final Set<CnATreeElement> requirementGroups;
    private final Set<CnATreeElement> targetElements;
    private final boolean handleSafeguards;
    private final boolean handleDummySafeguards;
    private final Set<String> moduleUuidsFromCompendium;
    private final ItNetwork itNetwork;
    private final Map<CnATreeElement, CnATreeElement> copiedElementsByCompendiumElement = new HashMap<>();
    private final Set<CnATreeElement> existingElements = new HashSet<>();

    public ModelingData(Set<String> moduleUuidsFromCompendium,
            Set<CnATreeElement> requirementGroups, Set<CnATreeElement> targetElements,
            ItNetwork itNetwork, boolean handleSafeguards, boolean handleDummySafeguards) {
        this.itNetwork = itNetwork;
        this.moduleUuidsFromCompendium = Collections.unmodifiableSet(moduleUuidsFromCompendium);
        this.requirementGroups = Collections.unmodifiableSet(requirementGroups);
        this.targetElements = Collections.unmodifiableSet(targetElements);
        this.handleSafeguards = handleSafeguards;
        this.handleDummySafeguards = handleDummySafeguards;
    }

    public Set<String> getModuleUuidsFromCompendium() {
        return moduleUuidsFromCompendium;
    }

    public Set<CnATreeElement> getRequirementGroups() {
        return requirementGroups;
    }

    public Set<CnATreeElement> getTargetElements() {
        return targetElements;
    }

    public ItNetwork getItNetwork() {
        return itNetwork;
    }

    public boolean isHandleSafeguards() {
        return handleSafeguards;
    }

    public boolean isHandleDummySafeguards() {
        return handleDummySafeguards;
    }

    public void addMappingForExistingElement(CnATreeElement compendiumElement,
            CnATreeElement scopeElement) {
        existingElements.add(scopeElement);
    }

    public void addMappingForNewElement(CnATreeElement compendiumElement,
            CnATreeElement scopeElement) {
        copiedElementsByCompendiumElement.put(compendiumElement, scopeElement);

    }

    public Set<String> getModuleUuidsFromScope() {
        return Stream
                .concat(copiedElementsByCompendiumElement.values().stream(),
                        existingElements.stream())
                .filter(item -> item.getTypeId().equals(BpRequirementGroup.TYPE_ID))
                .map(CnATreeElement::getUuid).collect(Collectors.toSet());
    }

    public Set<String> getSafeguardGroupUuidsFromScope() {
        return copiedElementsByCompendiumElement.values().stream()
                .filter(item -> item.getTypeId().equals(SafeguardGroup.TYPE_ID))
                .map(CnATreeElement::getUuid).collect(Collectors.toSet());
    }

}
