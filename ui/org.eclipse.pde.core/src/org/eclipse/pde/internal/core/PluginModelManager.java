/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ibundle.*;

public class PluginModelManager implements IAdaptable {
	private static final String OSGI_RUNTIME ="org.eclipse.osgi";
	private IModelProviderListener providerListener;
	private ExternalModelManager externalManager;
	private WorkspaceModelManager workspaceManager;
	private SearchablePluginsManager searchablePluginsManager;
	private ArrayList listeners;
	private Map fEntries;

	public PluginModelManager() {
		providerListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				handleModelsChanged(e);
			}
		};
		listeners = new ArrayList();
		searchablePluginsManager = new SearchablePluginsManager(this);
	}
	
	/*
	 * Returns true if OSGi runtime is currently present either in
	 * the workspace or in the target platform.
	 */
	public boolean isOSGiRuntime() {
		try {
			ModelEntry entry = findEntry("org.eclipse.platform");
			if (entry != null) {
				IPluginModelBase model = entry.getActiveModel();
				IResource resource = model.getUnderlyingResource();
				int version = new PluginVersionIdentifier(model.getPluginBase().getVersion()).getMajorComponent();
				if (resource != null &&  version < 3) {
					IProject project = resource.getProject();
					if (project.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jProject = JavaCore.create(project);
						IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
						for (int i = 0; i < roots.length; i++) {
							if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
								return false;
							}
						}
					}
					if (project.getFile("startup.jar").exists())
						return false;
				}
			}
		} catch (Exception e) {
		}
		return findEntry(OSGI_RUNTIME) != null;
	}

	public Object getAdapter(Class key) {
		return null;
	}
	
	public void addPluginModelListener(IPluginModelListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public void removePluginModelListener(IPluginModelListener listener) {
		if (listeners.contains(listener))
			listeners.remove(listener);
	}
	
	public boolean isEmpty() {
		return getEntryTable().isEmpty();
	}

	public ModelEntry[] getEntries() {
		Collection values = getEntryTable().values();
		return (ModelEntry[]) values.toArray(new ModelEntry[values.size()]);
	}

	public IPluginModelBase[] getPlugins() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model.isEnabled())
				result.add(model);
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public IPluginModel[] getPluginsOnly() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model.isEnabled() && model instanceof IPluginModel)
				result.add(model);
		}
		return (IPluginModel[])result.toArray(new IPluginModel[result.size()]);
	}
	
	public IFragmentModel[] getFragments() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model instanceof IFragmentModel)
				result.add(model);
		}
		return (IFragmentModel[])result.toArray(new IFragmentModel[result.size()]);
		
	}
	
	public ModelEntry findEntry(IProject project) {
		Map map = getEntryTable();
		IPluginModelBase model = workspaceManager.getWorkspacePluginModel(project);
		if (model==null)
			return null;
		String id = model.getPluginBase().getId();
		if (id == null || id.length() == 0)
			return null;
		return (ModelEntry)map.get(id);
	}
	
	public IPluginModelBase findModel(IProject project) {
		ModelEntry entry = findEntry(project);
		return (entry != null) ? entry.getActiveModel() : null;
	}
	
	public ModelEntry findEntry(String id) {
		return (ModelEntry) getEntryTable().get(id);
	}
	
	public IPluginModelBase findModel(String id) {
		ModelEntry entry = findEntry(id);
		return (entry == null) ?  null : entry.getActiveModel();
	}
	
	public IPluginModelBase findPlugin(String id, String version, int match) {
		return findModel(id);
	}
	
	public IPluginModel findPluginModel(String id) {
		IPluginModelBase model = findModel(id);
		return (model != null && model instanceof IPluginModel) ? (IPluginModel)model : null;
	}
	
	public IFragmentModel findFragmentModel(String id) {
		IPluginModelBase model = findModel(id);
		return (model != null && model instanceof IFragmentModel) ? (IFragmentModel)model : null;
	}
	
	private void handleModelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();
		ArrayList changedPlugins = new ArrayList();

		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (int i = 0; i < removed.length; i++) {
				if (!(removed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) removed[i];
				IPluginBase plugin = model.getPluginBase();
				updateTable(plugin.getId(), model, false, delta);
				changedPlugins.add(plugin);
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (int i = 0; i < added.length; i++) {
				if (!(added[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) added[i];
				IPluginBase plugin = model.getPluginBase();
				updateTable(plugin.getId(), model, true, delta);
				changedPlugins.add(plugin);
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (int i = 0; i < changed.length; i++) {
				if (!(changed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) changed[i];
				boolean workspace = model.getUnderlyingResource()!=null;
				IPluginBase plugin = model.getPluginBase();
				String id = plugin.getId();
				if (id != null) {
					ModelEntry entry = (ModelEntry)getEntryTable().get(plugin.getId());
					if (entry!=null) {
						if (workspace && model!=entry.getWorkspaceModel()) {
							//wrong slot - id changed
							handleIdChange(id, model, entry, delta);
						}
						else {
							delta.addEntry(entry, PluginModelDelta.CHANGED);
							changedPlugins.add(plugin);
							updateBundleDescription(model);
						}
						changedPlugins.add(plugin);
					}
					else if (workspace) {
						// model change, entry does not exist - must be
						// id change
						handleIdChange(id, model, null, delta);
						changedPlugins.add(plugin);
					}
				}
			}
		}
		updateAffectedEntries((IPluginBase[])changedPlugins.toArray(new IPluginBase[changedPlugins.size()]));
		fireDelta(delta);
	}
	
	private ModelEntry findOldEntry(IPluginModelBase model) {
		Collection values = getEntryTable().values();

		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase candidate = entry.getWorkspaceModel();
			if (model == candidate)
				return entry;
		}
		return null;
	}

	private void handleIdChange(String newId, IPluginModelBase model, ModelEntry newEntry, PluginModelDelta delta) {
		ModelEntry oldEntry = findOldEntry(model);
		// we must remove the model from the old entry
		if (oldEntry!=null) {
			oldEntry.setWorkspaceModel(null);
			if (oldEntry.isEmpty()) {
				// remove the old entry completely
				fEntries.remove(oldEntry.getId());
				delta.addEntry(oldEntry, PluginModelDelta.REMOVED);
			}
			else {
				// just notify that the old entry has changed
				delta.addEntry(oldEntry, PluginModelDelta.CHANGED);
			}
		}
		// add the model to the new entry; if does not exist, create
		if (newEntry!=null) {
			// change the workspace model of the new entry
			newEntry.setWorkspaceModel(model);
			delta.addEntry(newEntry, PluginModelDelta.CHANGED);
		}
		else {
			// create the new entry
			newEntry = new ModelEntry(this, newId);
			fEntries.put(newId, newEntry);
			delta.addEntry(newEntry, PluginModelDelta.ADDED);
		}
		// make sure bundle description of this model is up to date
		// if this is a bundle
		updateBundleDescription(model);
	}

	private void updateTable(
		String id,
		IPluginModelBase model,
		boolean added,
		PluginModelDelta delta) {
		boolean workspace = model.getUnderlyingResource()!=null;
		if (id == null)
			return;
		Map entries = getEntryTable();
		ModelEntry entry = (ModelEntry) entries.get(id);
		int kind = 0;
		if (added && entry == null) {
			entry = new ModelEntry(this, id);
			entries.put(id, entry);
			kind = PluginModelDelta.ADDED;
			try {
				entry.updateClasspathContainer(false, true);
			}
			catch (CoreException e) {
			}
		}
		if (added) {
			if (workspace)
				entry.setWorkspaceModel(model);
			else
				entry.setExternalModel(model);
		} else if (entry != null) {
			if (workspace) entry.setWorkspaceModel(null);
			else
				entry.setExternalModel(null);
			if (entry.isEmpty()) {
				entries.remove(id);
				kind = PluginModelDelta.REMOVED;
			}
		}
		if (workspace) {
			PDEState state = externalManager.getState();
			if (added) {
				addWorkspaceBundleToState(model, state);
			} else {
				removeWorkspaceBundleFromState(model, state);
			}
			state.resolveState(true);
		}
		if (kind==0) kind = PluginModelDelta.CHANGED;
		delta.addEntry(entry, kind);
	}

	private void updateAffectedEntries(IPluginBase [] changedPlugins) {
		// Reset classpath containers for affected entries
		ModelEntry [] entries = getEntries();
		Map map = new HashMap();
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];

			if (entry.isAffected(changedPlugins)) {
				try {
					if (entry.shouldUpdateClasspathContainer(true, true)) {
						IProject proj = entry.getWorkspaceModel().getUnderlyingResource().getProject();
						entry.getClasspathContainer().reset();
						map.put(JavaCore.create(proj), entry.getClasspathContainer());
					}
				}
				catch (CoreException e) {
				}
			}
		}
		if (map.size() > 0) {
			try {
				IJavaProject[] jProjects = (IJavaProject[])map.keySet().toArray(new IJavaProject[map.size()]);
				IClasspathContainer[] containers = (IClasspathContainer[])map.values().toArray(new IClasspathContainer[map.size()]);
				JavaCore.setClasspathContainer(
					new Path(PDECore.CLASSPATH_CONTAINER_ID),
					jProjects,
					containers,
					null);
			} catch (JavaModelException e) {
			}
		}
	}
	
	/*
	 * This method must be synchronized so that only one thread
	 * initializes the table, and the rest would block until
	 * the table is initialized.
	 * 
	 */
	private synchronized void initializeTable() {
		if (fEntries != null) return;
		fEntries = Collections.synchronizedMap(new TreeMap());
		IPluginModelBase[] models = externalManager.getAllModels();
		addToTable(models, false);
		models = workspaceManager.getAllModels();
		addToTable(models, true);
		addWorkspaceBundlesToState();
		searchablePluginsManager.initialize();
	}

	/*
	 * Allow access to the table only through this getter.
	 * It always calls initialize to make sure the table is initialized.
	 * If more than one thread tries to read the table at the same time,
	 *  and the table is not initialized yet, thread2 would wait. 
	 *  This way there are no partial reads.
	 */
	private Map getEntryTable() {
		initializeTable();
		return fEntries;
	}

	public void addWorkspaceBundlesToState() {
		IPluginModelBase[] models = workspaceManager.getAllModels();
		PDEState state = externalManager.getState();
		for (int i = 0; i < models.length; i++) {
			addWorkspaceBundleToState(models[i], state);
		}
		state.resolveState(true);
	}
	
	private void addWorkspaceBundleToState(IPluginModelBase model, PDEState state) {
		if (!(model instanceof IBundlePluginModelBase))
			return;
		String id = model.getPluginBase().getId();
		if (id == null)
			return;
		ModelEntry entry = findEntry(id);
		if (entry == null)
			return;
		IPluginModelBase external = entry.getExternalModel();
		if (external != null) {
			BundleDescription desc = external.getBundleDescription();
			state.removeBundleDescription(desc);
		}
		if (model.getBundleDescription() == null) {
			BundleDescription bundle = state.addBundle(new File(model.getInstallLocation()));	
			model.setBundleDescription(bundle);
		}
	}
	
	private void removeWorkspaceBundleFromState(IPluginModelBase model, PDEState state) {
		BundleDescription description = model.getBundleDescription();
		if (description == null)
			return;
		
		state.removeBundleDescription(description);
		
		String id = model.getPluginBase().getId();
		if (id == null) {
			return;
		}
		
		ModelEntry entry = findEntry(id);
		if (entry == null) {
			return;
		}
		
		IPluginModelBase external = entry.getExternalModel();
		if (external != null) {
			BundleDescription desc = external.getBundleDescription();
			state.addBundleDescription(desc);
		}
	}
	
	private void updateBundleDescription(IPluginModelBase model) {
		BundleDescription description = model.getBundleDescription();
		if (description == null)
			return;
		PDEState state = externalManager.getState();
		state.removeBundleDescription(description);
		BundleDescription newDesc = state.addBundle(new File(model.getInstallLocation()));
		model.setBundleDescription(newDesc);
		state.resolveState(true);
	}
	
	private void addToTable(
		IPluginModelBase[] pmodels,
		boolean workspace) {
		for (int i = 0; i < pmodels.length; i++) {
			addToTable(pmodels[i], workspace);
		}
	}

	private void addToTable(IPluginModelBase model, boolean workspace) {
		String id = model.getPluginBase().getId();
		if (id == null)
			return;
		Map entries = getEntryTable();
		ModelEntry entry = (ModelEntry) entries.get(id);
		if (entry == null) {
			entry = new ModelEntry(this, id);
			entries.put(id, entry);
		}
		if (workspace)
			entry.setWorkspaceModel(model);
		else
			entry.setExternalModel(model);
	}
	
	private void fireDelta(PluginModelDelta delta) {
		Object [] entries = listeners.toArray();
		for (int i=0; i<entries.length; i++) {
			((IPluginModelListener)entries[i]).modelsChanged(delta);
		}
	}

	public void connect(WorkspaceModelManager wm, ExternalModelManager em) {
		externalManager = em;
		workspaceManager = wm;
		externalManager.addModelProviderListener(providerListener);
		workspaceManager.addModelProviderListener(providerListener);
	}
	public void shutdown() {
		if (workspaceManager != null)	
			workspaceManager.removeModelProviderListener(providerListener);
		if (externalManager != null)
			externalManager.removeModelProviderListener(providerListener);
		searchablePluginsManager.shutdown();
	}
	
	public void setInJavaSearch(ModelEntry [] entries, boolean value, IProgressMonitor monitor) throws CoreException {
		PluginModelDelta delta = new PluginModelDelta();
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()!=value) {
				entry.setInJavaSearch(value);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0) {
			searchablePluginsManager.persistStates( monitor);
			fireDelta(delta);
		}
	}
	 
	void searchablePluginsRemoved() {
		ModelEntry [] entries = getEntries();
		PluginModelDelta delta = new PluginModelDelta();
		
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()) {
				entry.setInJavaSearch(false);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0)
			fireDelta(delta);
	}
	
	public IFileAdapterFactory getFileAdapterFactory() {
		return searchablePluginsManager;
	}
}