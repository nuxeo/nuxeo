/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.GadgetNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.spaces.core.contribs.api.GadgetProvider;
import org.nuxeo.ecm.spaces.core.contribs.api.SpaceProvider;
import org.nuxeo.ecm.spaces.core.contribs.api.UniversProvider;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.OperationNotSupportedException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * 
 * @author 10044893
 * 
 */
public class SpaceManagerImpl extends DefaultComponent implements SpaceManager {

	private static final Log LOGGER = LogFactory.getLog(SpaceManagerImpl.class);

	private List<String> universDescriptorBlackList = new ArrayList<String>();

	private static final String UNIVERS_CONTRIB = "universContrib";
	private static final String SPACE_CONTRIB = "spaceContrib";
	private static final String GADGET_CONTRIB = "gadgetContrib";

	@Override
	public void registerContribution(Object contribution,
			String extensionPoint, ComponentInstance contributor)
			throws Exception {
		
		if (UNIVERS_CONTRIB.equals(extensionPoint)) {
			UniversContribDescriptor descriptor = (UniversContribDescriptor) contribution;

			if (descriptor.isRemove()) {
				removeUniversDescriptor(descriptor);
			} else if (!universDescriptorBlackList.contains(descriptor
					.getName())) {
				UniversProvider provider = (UniversProvider) Class.forName(
						descriptor.getClassName()).newInstance();
				manageUniversDescriptor(descriptor, provider);
			} else {
				LOGGER.debug("Univers descriptor with name="
						+ descriptor.getName() + " and class name "
						+ descriptor.getClassName()
						+ " is ignored because has been blacklisted");
			}
		} else if (SPACE_CONTRIB.equals(extensionPoint)) {
			SpaceContribDescriptor descriptor = (SpaceContribDescriptor) contribution;
			SpaceProvider provider = (SpaceProvider) Class.forName(
					descriptor.getClassName()).newInstance();
			manageSpaceDescriptor(descriptor, provider);
		} else if (GADGET_CONTRIB.equals(extensionPoint)) {
			GadgetContribDescriptor descriptor = (GadgetContribDescriptor) contribution;
			if (descriptor.isRemove()) {
				removeGadgetDescriptor(descriptor);
			}
			GadgetProvider provider = (GadgetProvider) Class.forName(
					descriptor.getClassName()).newInstance();
			manageGadgetDescriptor(descriptor, provider);
		}

	}

	@Override
	public void unregisterContribution(Object contribution,
			String extensionPoint, ComponentInstance contributor)
			throws Exception {
		if (SPACE_CONTRIB.equals(extensionPoint)) {
			manageSpaceDescriptor((SpaceContribDescriptor) contribution, null);
		}
	}

	private synchronized void manageUniversDescriptor(
			UniversContribDescriptor descriptor, UniversProvider provider) {
		if (provider != null) {
			registeredUniversProviders.add(getOrderOrMax(descriptor.getOrder(),
					registeredUniversProviders.size()),
					new DescriptorUniversProviderPair(provider, descriptor));
		} else {
			// remove
			UniversContribDescriptor x = new UniversContribDescriptor();
			String universDescriptorName = descriptor.getName();
			x.setName(universDescriptorName);

			universDescriptorBlackList.add(universDescriptorName);
			LOGGER.debug("Univers descriptor " + universDescriptorName
					+ " has been blackListed");

			DescriptorUniversProviderPair pair = new DescriptorUniversProviderPair(
					null, x);
			registeredUniversProviders.remove(pair);
		}
	}

	private synchronized void manageSpaceDescriptor(
			SpaceContribDescriptor descriptor, SpaceProvider provider) {
		if (provider != null) {
			registeredSpacesProviders.add(getOrderOrMax(descriptor.getOrder(),
					registeredSpacesProviders.size()),
					new DescriptorSpaceProviderPair(descriptor, provider));
		} else {
			// remove
			DescriptorSpaceProviderPair pair = new DescriptorSpaceProviderPair(
					descriptor, null);
			registeredSpacesProviders.remove(pair);
		}
	}

	private synchronized void manageGadgetDescriptor(
			GadgetContribDescriptor descriptor, GadgetProvider provider) {
		if (provider != null) {
			registeredGadgetsProviders.add(getOrderOrMax(descriptor.getOrder(),
					registeredGadgetsProviders.size()),
					new DescriptorGadgetProviderPair(provider, descriptor));
		} else {
			// remove
			GadgetContribDescriptor x = new GadgetContribDescriptor();
			x.setName(descriptor.getName());
			DescriptorGadgetProviderPair pair = new DescriptorGadgetProviderPair(
					null, x);
			registeredSpacesProviders.remove(pair);
		}
	}

	private int getOrderOrMax(String value, int max) {
		int order = 0;
		try {
			if (value != null)
				order = Integer.parseInt(value);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		if (order <= max) {
			return order;
		} else {
			return max;
		}
	}

	private void removeUniversDescriptor(UniversContribDescriptor descriptor) {
		manageUniversDescriptor(descriptor, null);
	}
	
	private void removeGadgetDescriptor(GadgetContribDescriptor descriptor) {
		manageGadgetDescriptor(descriptor, null);
	}

	private List<DescriptorUniversProviderPair> registeredUniversProviders = new ArrayList<DescriptorUniversProviderPair>();
	private List<DescriptorSpaceProviderPair> registeredSpacesProviders = new ArrayList<DescriptorSpaceProviderPair>();
	private List<DescriptorGadgetProviderPair> registeredGadgetsProviders = new ArrayList<DescriptorGadgetProviderPair>();

	// protected DefaultSpaceManager defaultSpaceManager = null;

	/**
	 * Universe list
	 */
	public List<Univers> getUniversList(CoreSession coreSession)
			throws SpaceException {
		List<Univers> list = new ArrayList<Univers>();

		for (DescriptorUniversProviderPair pair : registeredUniversProviders) {
			List<? extends Univers> newUniverses;
			try {
				newUniverses = pair.getProvider().getAllElements(coreSession);
				if (newUniverses != null) {
					list.addAll(newUniverses);
				}
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}

		}
		return list;
	}

	/**
	 * Get a univers
	 * 
	 * @throws UniversNotFoundException
	 *             , SpaceException
	 */
	public Univers getUnivers(String name, CoreSession coreSession)
			throws SpaceException {

		for (DescriptorUniversProviderPair pair : registeredUniversProviders) {

			try {
				return pair.getProvider().getElementByName(name, coreSession);
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			} catch (NoElementFoundException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		throw new UniversNotFoundException("No Univers with name : '" + name
				+ "' was found");

	}

	public Univers getUniversFromId(String id, CoreSession coreSession)
			throws UniversNotFoundException, SpaceNotFoundException {

		try {
			return coreSession.getDocument(new IdRef(id)).getAdapter(
					Univers.class);
		} catch (ClientException e) {
			throw new UniversNotFoundException(e);
		}
	}

	public Univers updateUnivers(Univers newUnivers, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorUniversProviderPair pair : registeredUniversProviders) {
			try {
				return pair.getProvider().update(newUnivers, coreSession);
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		throw new UniversNotFoundException("No Univers found with  name='"
				+ newUnivers.getName() + "and id=" + newUnivers.getId()
				+ "'  . Updating univers has failed");
	}

	public void deleteUnivers(Univers univers, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorUniversProviderPair pair : registeredUniversProviders) {
			try {
				pair.getProvider().delete(univers, coreSession);
				return;
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		throw new UniversNotFoundException(
				"No Univers found with this name : '" + univers.getName()
						+ "'  . Deleting univers has failed");
	}

	/**
	 * Space list for a given univers
	 */
	public List<Space> getSpacesForUnivers(Univers univers,
			CoreSession coreSession) throws SpaceException {
		List<Space> list = new ArrayList<Space>();

		for (DescriptorSpaceProviderPair pair : registeredSpacesProviders) {
			SpaceContribDescriptor descriptor = pair.getDescriptor();
			SpaceProvider provider = pair.getProvider();
			String pattern = descriptor.getPattern();
			if (pattern == null || pattern.equals("*"))
				pattern = ".*";
			if (Pattern.matches(pattern, univers.getName())) {
				List<? extends Space> spacesForUnivers = null;
				try {
					spacesForUnivers = provider.getElementsForParent(univers,
							coreSession);
					if (spacesForUnivers != null) {
						list = addAll(list, spacesForUnivers);
					}
				} catch (OperationNotSupportedException e) {
					LOGGER.debug(e.getMessage(), e);
				}

			}
		}
		return list;
	}

	private List<Space> addAll(List<Space> list,
			List<? extends Space> spacesForUnivers) {

		for (Space space : spacesForUnivers) {
			boolean found = false;
			for (Space space1 : list) {
				if (space1.isEqualTo(space)) {
					found = true;
					break;
				}
			}
			if (!found) {
				list.add(space);
			}
		}
		return list;
	}

	public Space getSpace(String name, Univers univers, CoreSession coreSession)
			throws SpaceException {

		for (DescriptorSpaceProviderPair pair : registeredSpacesProviders) {
			SpaceContribDescriptor descriptor = pair.getDescriptor();
			SpaceProvider provider = pair.getProvider();
			String pattern = descriptor.getPattern();
			if (pattern == null || pattern.equals("*"))
				pattern = ".*";
			if (Pattern.matches(pattern, univers.getName())) {
				try {
					Space spaceResult = provider.getElement(name, univers,
							coreSession);
					if (spaceResult != null) {
						return spaceResult;
					} else {
						LOGGER
								.warn("Returns null , should throw a NoElementFoundException exception instead");
					}
				} catch (NoElementFoundException e) {
					LOGGER.info(e.getClass().getName() + " from "
							+ descriptor.getClassName());
					LOGGER.debug(e.getMessage(), e);
				} catch (OperationNotSupportedException e) {
					LOGGER.info(e.getClass().getName() + " from "
							+ descriptor.getClassName());
					LOGGER.debug(e.getMessage(), e);
				}
			}
		}
		throw new SpaceNotFoundException("No Space found with this name : '"
				+ name + "'");
	}

	public Space updateSpace(Space newSpace, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorSpaceProviderPair pair : registeredSpacesProviders) {
			SpaceProvider provider = pair.getProvider();
			try {
				return provider.update(newSpace, coreSession);
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}

		}
		throw new SpaceNotFoundException("No Space found with this name : '"
				+ newSpace.getName() + "' id=" + newSpace.getId());
	}

	public void deleteSpace(Space space, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorSpaceProviderPair pair : registeredSpacesProviders) {
			SpaceProvider provider = pair.getProvider();
			try {
				provider.delete(space, coreSession);
				return;
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}

		}
		throw new SpaceNotFoundException("No Space found with this name : '"
				+ space.getName() + "' Deleting space failed");
	}

	/**
	 * Gadgets for a given space
	 */
	public List<Gadget> getGadgetsForSpace(Space space, CoreSession coreSession)
			throws SpaceException {
		assert space != null;
		List<Gadget> list = new ArrayList<Gadget>();

		for (DescriptorGadgetProviderPair pair : registeredGadgetsProviders) {
			GadgetContribDescriptor descriptor = pair.getDescriptor();
			GadgetProvider provider = pair.getProvider();
			String pattern = descriptor.getPattern();
			if (pattern == null || pattern.equals("*"))
				pattern = ".*";
			if (Pattern.matches(pattern, space.getName())) {
				List<? extends Gadget> spacesForUnivers;
				try {
					spacesForUnivers = provider.getElementsForParent(space,
							coreSession);
					if (spacesForUnivers != null)
						list.addAll(spacesForUnivers);
				} catch (OperationNotSupportedException e) {
					LOGGER.debug(e.getMessage(), e);
				}
			}
		}
		return list;
	}

	public Gadget createGadget(Gadget gadget, Space space,
			CoreSession coreSession) throws SpaceException {
		for (DescriptorGadgetProviderPair pair : registeredGadgetsProviders) {
			GadgetContribDescriptor descriptor = pair.getDescriptor();
			GadgetProvider provider = pair.getProvider();
			String pattern = descriptor.getPattern();
			if (pattern == null || pattern.equals("*"))
				pattern = ".*";
			if (Pattern.matches(pattern, space.getName())) {
				try {

					Gadget x = provider.create(gadget, space, coreSession);
					if (x != null)
						return x;

				} catch (OperationNotSupportedException e) {
					LOGGER.debug(e.getMessage(), e);
				}

			}
		}
		throw new SpaceException("creation of gadget failed");
	}

	public Space createSpace(Space space, Univers univers,
			CoreSession coreSession) throws SpaceException {
		for (DescriptorSpaceProviderPair pair : registeredSpacesProviders) {
			SpaceContribDescriptor descriptor = pair.getDescriptor();
			SpaceProvider provider = pair.getProvider();
			String pattern = descriptor.getPattern();
			if (pattern == null || pattern.equals("*"))
				pattern = ".*";
			if (Pattern.matches(pattern, univers.getName())) {

				try {
					return provider.create(space, univers, coreSession);
				} catch (OperationNotSupportedException e) {
					LOGGER.debug(e.getMessage(), e);
				}

			}
		}
		throw new SpaceException("creation of space failed");
	}

	public Univers createUnivers(Univers univers, CoreSession coreSession)
			throws SpaceException {

		for (DescriptorUniversProviderPair pair : registeredUniversProviders) {
			UniversProvider provider = pair.getProvider();
			try {
				return provider.create(univers, coreSession);
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		throw new SpaceException("creation of univers has failed");

	}

	public void deleteGadget(Gadget gadget, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorGadgetProviderPair pair : registeredGadgetsProviders) {
			GadgetProvider provider = pair.getProvider();
			try {
				provider.delete(gadget, coreSession);
				return;
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}

		}
		throw new GadgetNotFoundException("No Gadget found with this name : '"
				+ gadget.getName() + "' Deleting gadget failed");
	}

	public Gadget getGadget(String name, Space space, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorGadgetProviderPair pair : registeredGadgetsProviders) {
			GadgetContribDescriptor descriptor = pair.getDescriptor();
			GadgetProvider provider = pair.getProvider();
			String pattern = descriptor.getPattern();
			if (pattern == null || pattern.equals("*"))
				pattern = ".*";
			if (Pattern.matches(pattern, space.getName())) {
				try {
					return provider.getElement(name, space, coreSession);
				} catch (OperationNotSupportedException e) {
					LOGGER.debug(e.getMessage(), e);
				} catch (NoElementFoundException e) {
					LOGGER.debug(e.getMessage(), e);
				}
			}
		}
		throw new GadgetNotFoundException("No Gadget found with this name : '"
				+ name + "'");
	}

	public Gadget updateGadget(Gadget newGadget, CoreSession coreSession)
			throws SpaceException {
		for (DescriptorGadgetProviderPair pair : registeredGadgetsProviders) {
			GadgetProvider provider = pair.getProvider();
			try {
				return provider.update(newGadget, coreSession);
			} catch (OperationNotSupportedException e) {
				LOGGER.debug(e.getMessage(), e);
			}

		}
		throw new GadgetNotFoundException("Update gadget name='"
				+ newGadget.getName() + "' , id='" + newGadget.getId()
				+ "' has failed ");
	}

	public Gadget getGadgetFromId(String gadgetId, CoreSession coreSession)
			throws GadgetNotFoundException {
		try {
			return coreSession.getDocument(new IdRef(gadgetId)).getAdapter(
					Gadget.class);
		} catch (ClientException e) {
			throw new GadgetNotFoundException(e);
		}
	}

	public Space getSpaceFromId(String spaceId, CoreSession coreSession)
			throws SpaceException {

		if (spaceId == null) {
			throw new SpaceException("space id can not be null");
		}
		try {
			return coreSession.getDocument(new IdRef(spaceId)).getAdapter(
					Space.class);
		} catch (DocumentSecurityException e) {
			throw new SpaceSecurityException(e);
		} catch (ClientException e) {
			throw new SpaceException(e);
		}
	}

}